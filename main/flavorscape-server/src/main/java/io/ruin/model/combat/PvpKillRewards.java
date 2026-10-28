package io.ruin.model.combat;

import io.ruin.model.entity.player.DevTesting;
import io.ruin.model.entity.player.Player;
import io.ruin.utility.TimedList;

/** One persistent reward cooldown shared by cash, points, emblems and other kill bonuses. */
public final class PvpKillRewards {
    public static final long COOLDOWN_MINUTES = 5L;
    private PvpKillRewards() {}

    public static long remainingMillis(Player killer, Player victim, long now) {
        if(killer.bmUserLogs == null)killer.bmUserLogs = new TimedList();
        if(killer.bmIpLogs == null)killer.bmIpLogs = new TimedList();
        long remaining = killer.bmUserLogs.remainingMillis(victim.getUserId(),now,COOLDOWN_MINUTES);
        if(!DevTesting.isPair(killer,victim))
            remaining = Math.max(remaining,killer.bmIpLogs.remainingMillis(victim.getIpInt(),now,COOLDOWN_MINUTES));
        // Honor reward history saved by earlier Bounty Hunter builds, using the new five-minute limit.
        return Math.max(remaining,killer.getBountyHunter().remainingRewardCooldown(victim,now));
    }

    public static boolean claim(Player killer, Player victim, long now) {
        if(killer == null || victim == null || killer == victim || killer.getUserId() <= 0
                || victim.getUserId() <= 0 || killer.getUserId() == victim.getUserId())return false;
        if(killer.getIpInt() == victim.getIpInt() && !DevTesting.isPair(killer,victim))return false;
        long remaining = remainingMillis(killer,victim,now);
        if(remaining > 0) {
            long seconds = (remaining + 999) / 1000;
            killer.sendMessage("No kill rewards for " + victim.getName() + ". Reward cooldown: "
                    + (seconds / 60) + ":" + String.format(java.util.Locale.ROOT,"%02d",seconds % 60) + " remaining.");
            return false;
        }
        killer.bmUserLogs.add(victim.getUserId(),now);
        killer.bmIpLogs.add(victim.getIpInt(),now);
        return true;
    }
}
