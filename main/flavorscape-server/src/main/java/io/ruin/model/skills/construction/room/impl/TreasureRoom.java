package io.ruin.model.skills.construction.room.impl;

import com.google.gson.annotations.Expose;
import io.ruin.api.utils.NumberUtils;
import io.ruin.cache.Color;
import io.ruin.model.entity.npc.NPC;
import io.ruin.model.entity.player.Player;
import io.ruin.model.inter.dialogue.ItemDialogue;
import io.ruin.model.inter.dialogue.MessageDialogue;
import io.ruin.model.inter.dialogue.OptionsDialogue;
import io.ruin.model.inter.utils.Option;
import io.ruin.model.map.Bounds;
import io.ruin.model.map.Direction;
import io.ruin.model.map.object.GameObject;
import io.ruin.model.map.object.actions.ObjectAction;
import io.ruin.model.skills.construction.Buildable;
import io.ruin.model.skills.construction.ChallengeMode;
import io.ruin.model.skills.construction.Hotspot;
import io.ruin.model.skills.construction.room.Room;

import static io.ruin.cache.ItemID.BLOOD_MONEY;

public class TreasureRoom extends Room {

    private static final int MAX_AMOUNT = 5_000_000;
    private NPC boss;
    @Expose private int loot = 0;
    private int killerId = -1;

    @Override
    protected void onBuild() {
        super.onBuild();
        getHotspotObjects(Hotspot.TREASURE).forEach(o -> ObjectAction.register(o, 1, this::treasureChestInteract));
    }

    private void treasureChestInteract(Player player, GameObject obj) {
        Buildable chestType = getBuilt(Hotspot.TREASURE);
        if (chestType == null) {
            return;
        }
        if (obj.id == chestType.getBuiltObjects()[0]) { // closed chest
            if (getHouse().getChallengeMode() == ChallengeMode.OFF && !player.isInOwnHouse()) {
                player.sendMessage("You can't open the chest until challenge mode is enabled.");
            } else if (!player.isInOwnHouse() && boss != null && !boss.getCombat().isDead()) {
                player.sendMessage("You must kill the treasure's guardian before you can open this chest!");
            } else {
                player.animate(832);
                obj.setId(obj.id + 1);
            }
        } else {
            if (!player.isInOwnHouse() || killerId == player.getUserId()) {
                if (killerId == -1 || player.getUserId() != killerId) {
                    player.sendMessage("Only the player who killed the treasure's guardian can loot the chest.");
                } else if (loot <= 0) {
                    player.dialogue(new MessageDialogue("The chest is empty!"));
                } else {
                    String found = NumberUtils.formatNumber(loot);
                    player.animate(832);
                    player.dialogue(new ItemDialogue().one(BLOOD_MONEY, "You search the chest and find " + found + " Blood Money!"));
                    player.getInventory().addOrDrop(BLOOD_MONEY, loot);
                    loot = 0;
                    getHouse().getMap().swRegion.players.forEach(p -> p.sendMessage(Color.DARK_GREEN.wrap(player.getName() + " has looted " + found + " Blood Money from a treasure chest")));
                    renderHotspot(getHotspotIndex(Hotspot.TREASURE));
                    killerId = -1;
                }
            } else {
                if (loot == MAX_AMOUNT) {
                    player.dialogue(new MessageDialogue("The chest already contains the maximum amount of Blood Money.<br>Would you like to withdraw them?"),
                            new OptionsDialogue(new Option("Yes", () -> {
                                int added = player.getInventory().add(BLOOD_MONEY, loot);
                                loot -= added;
                                obj.setId(chestType.getBuiltObjects()[0]);

                            }),
                                    new Option("No")
                            ));
                } else {
                    player.dialogue(new ItemDialogue().one(BLOOD_MONEY, loot == 0 ? "The chest is currently empty." : "The chest contains " + NumberUtils.formatNumber(loot) + " Blood Money."),
                            new OptionsDialogue(new Option("Add Blood Money", () -> {
                                player.integerInput("Enter amount to add to the chest (Maximum " + NumberUtils.formatNumber(MAX_AMOUNT) + "):", amt -> {
                                    if (amt <= 0)
                                        return;
                                    amt = Math.min(MAX_AMOUNT - loot, Math.min(amt, player.getInventory().getAmount(BLOOD_MONEY)));
                                    if (amt == 0)
                                        player.dialogue(new MessageDialogue("You don't have any Blood Money!"));
                                    else {
                                        player.getInventory().remove(BLOOD_MONEY, amt);
                                        loot += amt;
                                        player.dialogue(new ItemDialogue().one(BLOOD_MONEY, "The chest now contains " + NumberUtils.formatNumber(loot) + " Blood Money."));
                                    }
                                    obj.setId(chestType.getBuiltObjects()[0]);
                                });
                            }),
                                    new Option("Withdraw Blood Money", () -> {
                                        int added = player.getInventory().add(BLOOD_MONEY, loot);
                                        loot -= added;
                                        obj.setId(chestType.getBuiltObjects()[0]);
                                    }),
                                    new Option("Cancel")));
                }
            }
        }

    }

    @Override
    public void enableChallengeMode() {
        super.enableChallengeMode();
        if (getBuilt(Hotspot.TREASURE_BOSS) != null) {
            GameObject spawnPoint = getHotspotObjects(Hotspot.TREASURE_BOSS).get(0); // only 1
            boss = getHouse().addNPC(new NPC(DungeonGuardedRoom.GUARD_NPC_IDS.get(getBuilt(Hotspot.TREASURE_BOSS))).spawn(spawnPoint.x, spawnPoint.y, spawnPoint.z, Direction.getFromObjectDirection(spawnPoint.direction), 0));
            boss.walkBounds = new Bounds(getBaseAbsolutePosition().getX(), getBaseAbsolutePosition().getY(), getBaseAbsolutePosition().getX() + 7, getBaseAbsolutePosition().getY() + 7, getBaseAbsolutePosition().getZ());
            boss.deathEndListener = (entity, killer, killHit) -> {
                if (killer != null && killer.player != null) {
                    killerId = killer.player.getUserId();
                    killer.player.sendMessage(Color.DARK_GREEN.wrap("Congratulations, you've slain the treasure guardian! You can now claim the treasure."));
                }
            };
            spawnPoint.remove();
        }
    }

    @Override
    public void disableChallengeMode() {
        super.disableChallengeMode();
        if (boss != null) {
            boss.remove();
            renderHotspot(getHotspotIndex(Hotspot.TREASURE_BOSS));
        }
    }
}
