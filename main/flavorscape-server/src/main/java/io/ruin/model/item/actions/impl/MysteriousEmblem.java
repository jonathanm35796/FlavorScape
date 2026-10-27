package io.ruin.model.item.actions.impl;

import io.ruin.api.utils.NumberUtils;
import io.ruin.api.utils.ServerWrapper;
import io.ruin.cache.ItemDef;
import io.ruin.model.entity.player.Player;
import io.ruin.model.inter.dialogue.ItemDialogue;
import io.ruin.model.inter.dialogue.YesNoDialogue;
import io.ruin.model.item.Item;
import io.ruin.model.item.actions.ItemAction;
import io.ruin.model.item.containers.Inventory;
import static io.ruin.cache.ItemID.BLOOD_MONEY;

/** One authoritative tier/payout table shared by kills, inventory redemption and the trader. */
public final class MysteriousEmblem {
    private static final int[] IDS={12746,12748,12749,12750,12751,12752,12753,12754,12755,12756};
    private static final int[] VALUES={3000,5000,7500,10000,15000,20000,30000,50000,75000,100000};
    public static int tier(int id){for(int i=0;i<IDS.length;i++)if(IDS[i]==id)return i+1;return 0;}
    public static int value(int id){int tier=tier(id);return tier==0?0:VALUES[tier-1];}
    public static int itemId(int tier){return tier<1||tier>10?-1:IDS[tier-1];}
    public static int downgradedId(int id){int tier=tier(id);return tier==0?-1:itemId(Math.max(1,tier-1));}
    public static Item highest(Player player){
        Item best=null;
        for(Item item:player.getInventory().getItems())
            if(item!=null&&tier(item.getId())>0&&(best==null||tier(item.getId())>tier(best.getId())))best=item;
        return best;
    }
    public static void info(Player player){
        String[] lines=new String[17];
        lines[0]="Kill your assigned Edgeville Bounty Hunter target to earn a tier 1 emblem.";
        lines[1]="Carry an emblem on eligible target kills to upgrade your highest emblem.";
        lines[2]="Emblems upgrade one tier at a time, up to tier 10.";
        lines[3]="Non-target kills do not create or upgrade emblems.";
        lines[4]="On death, lost emblems downgrade and drop for your killer.";
        lines[5]="Redeem safely from your inventory or use an emblem on the Emblem Trader.";
        lines[6]="";
        for(int i=0;i<10;i++)lines[i+7]="Tier "+(i+1)+": "+NumberUtils.formatNumber(VALUES[i])+" Blood Money";
        player.sendScroll("Bounty Hunter emblems",lines);
    }
    public static void offerRedemption(Player player,Item item){
        if(!canRedeem(player,item,item.getId()))return;
        final int id=item.getId(),amount=value(id);
        player.dialogue(new YesNoDialogue("Redeem Bounty Hunter emblem?",
                "Exchange tier "+tier(id)+" for "+NumberUtils.formatNumber(amount)+" Blood Money?",item,()->redeem(player,item,id)));
    }
    private static boolean canRedeem(Player player,Item item,int expectedId){
        Inventory inventory=player.getInventory();int slot=item.getSlot();
        if(value(expectedId)==0||item.getId()!=expectedId||item.getAmount()!=1||slot<0||slot>=inventory.getItems().length
                ||inventory.get(slot)!=item||item.getContainer()!=inventory){
            player.sendMessage("That emblem is no longer in your inventory. Please select it again.");return false;
        }
        if(player.wildernessLevel>0||player.pvpAttackZone||player.getCombat().isDead()||player.isLocked()
                ||player.getCombat().isAttacking(16)||player.getCombat().isDefending(16)){
            player.sendMessage("You must be safely out of combat to redeem an emblem.");return false;
        }
        if((long)inventory.count(BLOOD_MONEY)+value(expectedId)>Integer.MAX_VALUE){
            player.sendMessage("Bank some Blood Money before redeeming this emblem.");return false;
        }
        return true;
    }
    private static void redeem(Player player,Item item,int id){
        if(!canRedeem(player,item,id))return;
        Inventory inventory=player.getInventory();int slot=item.getSlot(),amount=value(id);
        // Removing this nonstackable emblem guarantees a slot for the currency if needed.
        inventory.set(slot,null);
        int added=inventory.add(BLOOD_MONEY,amount);
        if(added!=amount){
            if(added>0)inventory.remove(BLOOD_MONEY,added);
            inventory.set(slot,item);throw new IllegalStateException("Prechecked emblem payment failed");
        }
        ServerWrapper.log("BH emblem redeemed: "+player.getName()+" tier="+tier(id)+" BloodMoney="+amount);
        player.dialogue(new ItemDialogue().one(BLOOD_MONEY,"You redeem your tier "+tier(id)+" emblem for "
                +NumberUtils.formatNumber(amount)+" Blood Money."));
    }
    static {
        for(int id:IDS){
            ItemDef def=ItemDef.get(id);def.sigmundBuyPrice=value(id);def.neverProtect=true;
            ItemAction.registerInventory(id,"info",(player,item)->info(player));
            ItemAction.registerInventory(id,"redeem",MysteriousEmblem::offerRedemption);
        }
    }
}
