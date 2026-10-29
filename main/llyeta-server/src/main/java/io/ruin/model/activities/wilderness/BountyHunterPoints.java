package io.ruin.model.activities.wilderness;

import io.ruin.model.entity.player.Player;
import io.ruin.model.item.Item;
import io.ruin.model.item.actions.impl.MysteriousEmblem;

/** Fixed point accounting preserves all nine halvings without rounding away low tiers. */
public final class BountyHunterPoints {
    public static final int UNITS_PER_POINT = 512;
    public static final long MAX_UNITS = 10_000_000_000_000L * UNITS_PER_POINT;
    private BountyHunterPoints() {}

    public static long tierUnits(int tier) {
        return tier < 1 || tier > 10 ? UNITS_PER_POINT : (100L * UNITS_PER_POINT) >> (10 - tier);
    }

    public static double balance(Player player) {
        return player.bountyHunterPointUnits / (double) UNITS_PER_POINT;
    }

    public static long add(Player player, long units) {
        long added = Math.max(0, Math.min(units, MAX_UNITS - player.bountyHunterPointUnits));
        player.bountyHunterPointUnits += added;
        return added;
    }

    public static void rewardKill(Player killer, Player victim, boolean target) {
        Item emblem = MysteriousEmblem.highest(victim);
        long units = tierUnits(emblem == null ? 0 : MysteriousEmblem.tier(emblem.getId()));
        if (target) units += 10L * UNITS_PER_POINT;
        long added = add(killer, units);
        killer.sendMessage("You receive " + format(added) + " Bounty Hunter points"
                + (target ? " (including +10 for your bounty target)" : "") + ".");
    }

    public static String format(long units) {
        return java.math.BigDecimal.valueOf(units).divide(java.math.BigDecimal.valueOf(UNITS_PER_POINT))
                .stripTrailingZeros().toPlainString();
    }
}
