package io.ruin.model.activities.wilderness;

import io.ruin.model.entity.player.Player;
import io.ruin.model.item.containers.Equipment;

/** Activation rules for the Ancient Warriors' equipment in this server's Wilderness BH system. */
public final class BountyHunterEquipment {
    public static final int MAX_WILDERNESS_LEVEL = 5;
    private BountyHunterEquipment() {}

    public static int baseItemId(int itemId) {
        switch (itemId) {
            case 23615: return 22613;
            case 23617: return 22647;
            case 23620: return 22622;
            default: return itemId;
        }
    }

    public static boolean isRestricted(int itemId) {
        switch (baseItemId(itemId)) {
            case 22610: case 22613: case 22616: case 22619: // Vesta
            case 22622: case 22625: case 22628: case 22631: // Statius
            case 22634: case 22636: case 22638: case 22641: case 22644: // Morrigan
            case 22647: case 22650: case 22653: case 22656: // Zuriel
                return true;
            default:
                return false;
        }
    }

    public static boolean isActive(Player player) {
        if (player == null) return false;
        if (player.getCombat() != null && player.getCombat().isTestingMaxHitDummy()) return true;
        return isInBountyHunter(player);
    }

    private static boolean isInBountyHunter(Player player) {
        BountyHunter bh = player.getBountyHunter();
        return player.wildernessLevel > 0 && player.wildernessLevel <= MAX_WILDERNESS_LEVEL
                && bh != null && bh.targeting != null
                && bh.targeting.isActive(player);
    }

    public static boolean hasActiveItem(Player player, int itemId) {
        int slot = io.ruin.cache.ItemDef.get(itemId).equipSlot;
        return isActive(player) && slot >= 0
                && baseItemId(player.getEquipment().getId(slot)) == baseItemId(itemId);
    }

    public static boolean canUseWeapon(Player player, boolean message) {
        return canUseWeapon(player, null, message);
    }

    public static boolean isMaxHitDummy(io.ruin.model.entity.Entity target) {
        return target != null && target.npc != null && target.npc.getId() == 2668;
    }

    public static boolean canUseWeapon(Player player, io.ruin.model.entity.Entity target, boolean message) {
        if (!isRestricted(player.getEquipment().getId(Equipment.SLOT_WEAPON)) || isInBountyHunter(player) || isMaxHitDummy(target))
            return true;
        if (message)
            player.sendMessage("Ancient Warriors' weapons can only be used in Bounty Hunter at Wilderness levels 1-5, or on the max hit dummy.");
        return false;
    }
}
