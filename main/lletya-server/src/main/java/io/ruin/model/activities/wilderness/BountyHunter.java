package io.ruin.model.activities.wilderness;

import com.google.gson.annotations.Expose;
import io.ruin.api.utils.Random;
import io.ruin.model.World;
import io.ruin.model.combat.KillingSpree;
import io.ruin.model.combat.PvpKillRewards;
import io.ruin.model.entity.player.Player;
import io.ruin.model.entity.player.DevTesting;
import io.ruin.model.inter.Interface;
import io.ruin.model.inter.InterfaceHandler;
import io.ruin.model.inter.InterfaceType;
import io.ruin.model.inter.actions.SimpleAction;
import io.ruin.model.inter.dialogue.MessageDialogue;
import io.ruin.model.inter.dialogue.OptionsDialogue;
import io.ruin.model.inter.utils.Config;
import io.ruin.model.inter.utils.Option;
import io.ruin.model.item.Item;
import io.ruin.model.item.ItemContainer;
import io.ruin.model.item.actions.impl.MysteriousEmblem;
import io.ruin.model.map.Bounds;
import io.ruin.model.map.ground.GroundItem;
import io.ruin.utility.Misc;
import io.ruin.utility.TimedList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class BountyHunter {
    public static final int TARGET_PANEL_CHILD=18, TARGET_NAME_CHILD=53, TARGET_LOCATION_CHILD=54;

    private static final Bounds EDGEVILLE_BOUNDS = new Bounds(2993, 3523, 3124, 3597, -1);

    public enum Targeting {

        ALL("All Wilderness", p -> true),
        DISABLED("Disabled", p -> false),
        EDGEVILLE("Edgeville Only", p -> p.wildernessLevel>=1&&p.wildernessLevel<=5&&p.getPosition().inBounds(EDGEVILLE_BOUNDS));

        public final String name;

        private final Predicate<Player> active;

        Targeting(String name, Predicate<Player> active) {
            this.name = name;
            this.active = active;
        }

        public boolean isActive(Player player) {
            return active.test(player);
        }

        private static boolean match(Player player, Player searching) {
            Targeting t1 = player.getBountyHunter().targeting;
            if (!t1.isActive(player) || !searching.getBountyHunter().targeting.isActive(searching))
                return false;
            Targeting t2 = searching.getBountyHunter().targeting;
            if (t1 == t2)
                return true;
            if (t1 == ALL)
                return t2.isActive(player);
            if (t2 == ALL)
                return t1.isActive(searching);
            return false;
        }

    }

    /**
     * Separator
     */

    private Player player;

    @Expose public Targeting targeting = Targeting.EDGEVILLE;
    @Expose private boolean edgevilleDefaultsApplied;

    public boolean interfaceHidden;

    public Player target;

    @Expose private TimedList recentTargets = new TimedList();
    @Expose private TimedList rewardedAccounts = new TimedList();
    @Expose private TimedList rewardedIps = new TimedList();

    protected int returnTicks;

    private int returnTimeoutTicks;

    @Expose private int skips;

    @Expose private long skipsResetAt;

    @Expose private long penaltyEnd;

    public void init(Player player) {
        this.player = player;
        if(!edgevilleDefaultsApplied){
            if(targeting==null||targeting==Targeting.ALL)targeting=Targeting.EDGEVILLE;
            edgevilleDefaultsApplied=true;
        }
        if(recentTargets==null)recentTargets=new TimedList();
        if(rewardedAccounts==null)rewardedAccounts=new TimedList();
        if(rewardedIps==null)rewardedIps=new TimedList();
    }

    public void refreshOverlay(){
        refreshOverlay(false);
    }
    private void refreshOverlay(boolean force){
        boolean hidden=!player.bountyHunterOverlay||(target==null&&!targeting.isActive(player));
        if(force||hidden!=interfaceHidden){interfaceHidden=hidden;player.getPacketSender().sendVarp(20002,hidden?1:0);}
    }

    protected void checkActive() {
        player.getEquipment().refreshBountyHunterBonuses();
        if (targeting == Targeting.DISABLED && target != null) {
            //If player disables their targeting and they have a target, auto skip.
            abandon();
        }
        refreshOverlay();
        if (player.wildernessLevel > 0 && targeting.isActive(player)) {
            if (returnTicks > 0 && ++returnTimeoutTicks >= 60)
                returnTicks = 0;
        } else {
            if (returnTicks == 0) {
                if (target != null) {
                    returnTicks = 200;
                    Config.WILDERNESS_TIMER.set(player, returnTicks);
                    player.getPacketSender().setHidden(Interface.WILDERNESS_OVERLAY, 57, false);
                    player.getPacketSender().setHidden(Interface.WILDERNESS_OVERLAY, 58, true);
                    player.sendMessage("<col=ff0000>You have 2 minutes to return to your Bounty Hunter area before you lose your target.");
                }
            } else if (returnTicks > 0) {
                if (--returnTicks == 0) {
                    player.closeInterface(InterfaceType.WILDERNESS_OVERLAY);
                    if (target != null) {
                        target.sendMessage("Your target has fled, you will be assigned a new one shortly.");
                        abandon();
                    }
                } else {
                    if (returnTicks == 100)
                        player.sendMessage("<col=ff0000>You have one minute to return to your Bounty Hunter area before you lose your target.");
                    if (returnTimeoutTicks > 0) {
                        Config.WILDERNESS_TIMER.set(player, returnTicks + 1); //+ 1 for slightly better accuracy...
                        player.getPacketSender().setHidden(Interface.WILDERNESS_OVERLAY, 57, false);
                        player.getPacketSender().setHidden(Interface.WILDERNESS_OVERLAY, 58, true);
                    }
                }
            }
            returnTimeoutTicks = 0;
        }
    }

    /**
     * Targeting
     */

    private boolean searchForTarget(long ms) {
        if (player.getCombat().isDead())
            return false;
        if (target != null) {
            updateTargetInfo();
            return false;
        }
        if (player.wildernessLevel<=0 || !targeting.isActive(player))
            return false;
        if (hasPenalty(ms)) {
            updatePenaltyInfo(Math.max(1L, TimeUnit.MILLISECONDS.toMinutes(penaltyEnd - ms)));
            return false;
        }
        return true;
    }

    private boolean allowAsTarget(Player searching, long ms) {
        if(player==searching||player.wildernessLevel<=0||searching.wildernessLevel<=0
                ||player.getUserId()<=0||searching.getUserId()<=0)return false;
        if (player.getCombat().isDead())
            return false;
        if (player.getUserId() == searching.getUserId())
            return false;
        boolean testing=DevTesting.isPair(player,searching);
        if (player.getIpInt() == searching.getIpInt() && !testing)
            return false;
        if (Math.abs(player.getCombat().getLevel() - searching.getCombat().getLevel()) > 5)
            return false;
        if (target != null)
            return false;
        if (!Targeting.match(player, searching))
            return false;
        if (hasPenalty(ms))
            return false;
        return testing || (!recentTargets.contains(searching.getUserId(), ms, 10L)
                &&!searching.getBountyHunter().recentTargets.contains(player.getUserId(),ms,10L));
    }

    private void target(Player pTarget, long ms) {
        target = pTarget;
        recentTargets.add(pTarget.getUserId(), ms);
        player.getPacketSender().sendHintIcon(target);
        refreshOverlay(true);
        player.sendMessage("<col=ff0000>You've been assigned a target: " + pTarget.getName());
        updateTargetInfo();
    }

    private void removeTarget() {
        target = null;
        player.getPacketSender().resetHintIcon(false);
        if (player.wildernessLevel == 0)
            player.closeInterface(InterfaceType.WILDERNESS_OVERLAY);
        returnTicks = returnTimeoutTicks = 0;
        clearTargetInfo();
    }

    private boolean hasPenalty(long ms) {
        if(DevTesting.isAccount(player))return false;
        if (skips >= 10) {
            skipsResetAt = skips = 0;
            penaltyEnd = ms + 1800000; //30 minutes
            return true;
        }
        if (skips > 0) {
            if (skipsResetAt == 0)
                skipsResetAt = ms + 1800000; //30 minutes
            else if (ms >= skipsResetAt)
                skipsResetAt = skips = 0;
        }
        return ms < penaltyEnd;
    }

    /**
     * Info (Interface)
     */

    private void updateTargetInfo() {
        /**
         * Target info
         */
        String skullImg = target.getCombat().isSkulled() ? "<img=" + KillingSpree.imgId(target) + "> " : "";
        int distance = Misc.getDistance(player.getPosition(), target.getPosition());
        String distanceColor;
        if (distance <= 20)
            distanceColor = "<col=009900>";
        else if (distance <= 40)
            distanceColor = "<col=996600>";
        else if (distance <= 60)
            distanceColor = "<col=990000>";
        else if (distance <= 80)
            distanceColor = "<col=df0101>";
        else
            distanceColor = "<col=0080ff>";
        String wild;
        if (target.wildernessLevel > 0) {
            int base = ((target.wildernessLevel - 1) / 5) * 5 + 1;
            wild = "Lvl " + base + "-" + (base + 4);
        } else {
            wild = "Safe";
        }
        player.getPacketSender().sendString(Interface.WILDERNESS_OVERLAY, TARGET_NAME_CHILD, skullImg + target.getName());
        player.getPacketSender().sendString(Interface.WILDERNESS_OVERLAY, TARGET_LOCATION_CHILD, distanceColor + wild + ", Cmb " + target.getCombat().getLevel());

        /**
         * Target wealth
         */
        long targetWealth = countWealth(target.getInventory()) + countWealth(target.getEquipment());

        if (target.getInventory().hasId(11941))
            targetWealth += countWealth(target.getLootingBag());
        if (target.getInventory().hasId(12791))
            targetWealth += countWealth(target.getRunePouch());

        if (targetWealth >= 2500000)
            Config.BOUNTY_HUNTER_RISK.set(player, 5); //red
        else if (targetWealth >= 1100000)
            Config.BOUNTY_HUNTER_RISK.set(player, 4); //blue
        else if (targetWealth >= 500000)
            Config.BOUNTY_HUNTER_RISK.set(player, 3); //green
        else if (targetWealth >= 100000)
            Config.BOUNTY_HUNTER_RISK.set(player, 2); //silver
        else
            Config.BOUNTY_HUNTER_RISK.set(player, 1); //brown

        /**
         * Target emblems
         */
        Item highestEmblem = findHighestEmblem(target);
        Config.BOUNTY_HUNTER_EMBLEM.set(player, highestEmblem == null ? 0 : MysteriousEmblem.tier(highestEmblem.getId()));
    }

    private void clearTargetInfo() {
        player.getPacketSender().sendString(90, TARGET_NAME_CHILD, "Searching...");
        player.getPacketSender().sendString(90, TARGET_LOCATION_CHILD, "Within 5 levels");
        Config.BOUNTY_HUNTER_RISK.set(player, 0);
        Config.BOUNTY_HUNTER_EMBLEM.set(player, 0);
    }

    private void updatePenaltyInfo(long minutes) {
        player.getPacketSender().sendString(90, TARGET_NAME_CHILD, "<col=ff0000>---</col>");
        player.getPacketSender().sendString(90, TARGET_LOCATION_CHILD, minutes + " " + (minutes > 1 ? "mins" : "min"));
        Config.BOUNTY_HUNTER_RISK.set(player, 0);
        Config.BOUNTY_HUNTER_EMBLEM.set(player, 0);
    }

    /**
     * Skipping
     */

    public void skip(boolean warning) {
        if(target==null){player.sendMessage("You don't have a Bounty Hunter target to skip.");return;}
        if(player.getCombat().isDefending(17)) {
            player.sendMessage("You need to be out of combat for 10 seconds before skipping a bounty target.");
            return;
        }
        if (warning) {
            String message = "<col=ff0000>Warning:</col> Skipping too many targets in a short period of time can<br>cause you to incur a target restriction penalty. You should not use<br>this too frequently.";
            int lineHeight;
            if (skips == 0) {
                lineHeight = 24;
            } else if (skips == 1) {
                message += " You have abandoned your target once recently.";
                lineHeight = 24;
            } else {
                message += " You have abandoned your target " + skips + " times<br>recently.";
                lineHeight = 17;
            }
            player.dialogue(
                    new MessageDialogue(message).lineHeight(lineHeight),
                    new OptionsDialogue(
                            new Option("Yes.", () -> {
                                if (target != null)
                                    skip(false);
                                player.closeDialogue();
                            }),
                            new Option("No.", player::closeDialogue)
                    )
            );
        } else {
            abandon();
        }
    }

    private void abandon(){
        if(target==null)return;
        long now=System.currentTimeMillis();
        if(skipsResetAt==0||now>=skipsResetAt){skips=0;skipsResetAt=now+1800000;}
        skips++;
        Player previous=target;
        if(previous.getBountyHunter().target==player)previous.getBountyHunter().removeTarget();
        removeTarget();
        if(skips>=10&&!DevTesting.isAccount(player)){hasPenalty(now);player.sendMessage("You have abandoned 10 targets in 30 minutes. New targets are restricted for 30 minutes.");}
        else player.sendMessage("You have abandoned "+skips+" target(s) in the last 30 minutes.");
    }

    /**
     * Logging out
     */

    public void loggedOut() {
        if (target != null) {
            target.sendMessage("Your target has logged out, you will be assigned a new one shortly.");
            abandon();
        }
    }

    /**
     * Death
     */

    public boolean deathByTarget(Player pKiller) {
        long now=System.currentTimeMillis();
        return deathByTarget(pKiller,PvpKillRewards.claim(pKiller,player,now),now);
    }

    public boolean deathByTarget(Player pKiller,boolean rewardAllowed,long ms) {
        Config.BOUNTY_HUNTER_TARGET_KILLS.set(player,0);
        Config.BOUNTY_HUNTER_ROGUE_KILLS.set(player,0);
        Player assigned=target;
        boolean reciprocal=assigned!=null&&assigned==pKiller&&pKiller.getBountyHunter().target==player;
        if (target != null) {
            if(assigned.getBountyHunter().target==player)assigned.getBountyHunter().removeTarget();
            removeTarget();
        }
        if(!rewardAllowed||player.wildernessLevel<=0||pKiller.wildernessLevel<=0){
            if(assigned!=null)assigned.sendMessage("Your target has died. You will be assigned a new one shortly.");
            return false;
        }
        BountyHunter winner=pKiller.getBountyHunter();
        winner.rewardedAccounts.add(player.getUserId(),ms);
        winner.rewardedIps.add(player.getIpInt(),ms);
        if(reciprocal){
                penaltyEnd = ms + 360000;
                winner.penaltyEnd = ms + 60000;
                pKiller.sendFilteredMessage("<col=ff0000>You've killed your target: " + player.getName());
                pKiller.sendFilteredMessage("Next target in: <col=ff0000> 1 minute");
                Item emblem = MysteriousEmblem.highest(pKiller);
                int tier=emblem==null?0:MysteriousEmblem.tier(emblem.getId());
                if(tier>0&&tier<10){
                    emblem.setId(MysteriousEmblem.itemId(tier+1));
                    pKiller.sendMessage("Your Bounty Hunter emblem upgrades to tier "+(tier+1)+".");
                }else{
                    dropEmblem(pKiller,MysteriousEmblem.itemId(1));
                    pKiller.sendMessage("A tier 1 Bounty Hunter emblem has dropped with your target's loot.");
                }
                int kills = Config.BOUNTY_HUNTER_TARGET_KILLS.get(pKiller) + 1;
                int record = Config.BOUNTY_HUNTER_TARGET_RECORD.get(pKiller);
                if (kills > record)
                    Config.BOUNTY_HUNTER_TARGET_RECORD.set(pKiller, kills);
                Config.BOUNTY_HUNTER_TARGET_KILLS.set(pKiller, kills);
                return true;
        }
        if(assigned!=null)assigned.sendMessage("Your target has died. You will be assigned a new one shortly.");
            int kills = Config.BOUNTY_HUNTER_ROGUE_KILLS.get(pKiller) + 1;
            int record = Config.BOUNTY_HUNTER_ROGUE_RECORD.get(pKiller);
            if (kills > record)
                Config.BOUNTY_HUNTER_ROGUE_RECORD.set(pKiller, kills);
            Config.BOUNTY_HUNTER_ROGUE_KILLS.set(pKiller, kills);
        return false;
    }

    public long remainingRewardCooldown(Player victim,long ms){
        long remaining=rewardedAccounts.remainingMillis(victim.getUserId(),ms,PvpKillRewards.COOLDOWN_MINUTES);
        if(!DevTesting.isPair(player,victim))
            remaining=Math.max(remaining,rewardedIps.remainingMillis(victim.getIpInt(),ms,PvpKillRewards.COOLDOWN_MINUTES));
        return remaining;
    }

    protected void dropEmblem(Player killer,int id){
        new GroundItem(id,1).owner(killer).position(player.getPosition()).spawnPrivate();
    }

    /**
     * Misc
     */

    private static long countWealth(ItemContainer container) {
        long wealth = 0;
        for (Item item : container.getItems()) {
            if (item != null)
                wealth += (long)item.getAmount() * item.getDef().protectValue;
        }
        return wealth;
    }

    private static Item findHighestEmblem(Player player) {
        return MysteriousEmblem.highest(player);
    }

    /**
     * Register Event & Interface.
     */

    public static void assignTargets(List<Player> players,long ms){
        List<Player> available=new ArrayList<>(players);
        for(Player first:available){
            if(!first.getBountyHunter().searchForTarget(ms))continue;
            List<Player> candidates=new ArrayList<>();
            for(Player second:available)
                if(second.getBountyHunter().allowAsTarget(first,ms))candidates.add(second);
            if(candidates.isEmpty()){first.getBountyHunter().clearTargetInfo();continue;}
            Player second=Random.get(candidates);
            second.getBountyHunter().target(first,ms);first.getBountyHunter().target(second,ms);
        }
    }

    static {
        World.startEvent(e -> {
            while (true) {
                long ms = System.currentTimeMillis();
                assignTargets(Wilderness.players,ms);
                e.delay(20);
            }
        });
        InterfaceHandler.register(Interface.WILDERNESS_OVERLAY, h -> h.actions[56] = (SimpleAction) p -> p.getBountyHunter().skip(true));
    }

}
