package io.ruin.model.entity.player;

import io.ruin.model.World;
import java.util.Locale;
import java.util.Set;

/** Explicit local testing identities. Staff rank alone never enables these exceptions. */
public final class DevTesting {
    private static final Set<String> ACCOUNTS = Set.of("jon", "jon2", "sales tax");
    private DevTesting() {}

    public static boolean isAccount(String name) {
        return World.isDev() && name != null
                && ACCOUNTS.contains(normalize(name));
    }
    private static String normalize(String name) { return name.replace('_',' ').trim().toLowerCase(Locale.ROOT); }
    public static boolean isAccount(Player player) { return player != null && isAccount(player.getName()); }

    public static boolean isPair(Player first, Player second) {
        return first != null && second != null && first != second
                && first.getUserId() > 0 && second.getUserId() > 0 && first.getUserId() != second.getUserId()
                && isAccount(first) && isAccount(second) && !normalize(first.getName()).equals(normalize(second.getName()));
    }

    public static boolean atWildernessIpLimit(int ip, Iterable<Player> players) {
        int count=0;
        for(Player player:players)
            if(!isAccount(player)&&player.getIpInt()==ip&&++count>=2)return true;
        return false;
    }
    public static boolean canLogin(String name,int ip,Iterable<Player> wildernessPlayers) {
        return isAccount(name)||!atWildernessIpLimit(ip,wildernessPlayers);
    }
}
