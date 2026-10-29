package net.runelite.standalone;

/** Send gear requests through the same channel as normal chat commands. */
public final class LlyetaCommands {
    public static final int MAX_PRESETS = 12;
    private static String[] pending = new String[MAX_PRESETS];
    private static volatile String[] previews = new String[MAX_PRESETS];
    private static volatile String[] summary = new String[]{"0", "0", "0", "No task", "Player", "0", "0"};
    private static final java.util.concurrent.atomic.AtomicBoolean presetLoaded = new java.util.concurrent.atomic.AtomicBoolean();
    private LlyetaCommands() {}
    public static boolean receive(int hash, String value) {
        if (LlyetaLootKeyState.receive(hash,value)) return true;
        if (LlyetaMarketState.receive(hash,value)) return true;
        if ((hash >>> 16) != 32766) return false;
        int row = hash & 65535;
        if (row == 0) pending = new String[MAX_PRESETS];
        else if (row >= 1 && row <= MAX_PRESETS) pending[row - 1] = value;
        else if (row == 18) previews = pending.clone();
        else if (row == 19) {
            try {
                String[] update = new com.google.gson.Gson().fromJson(value, String[].class);
                if (update != null && update.length == 7) summary = update;
            } catch (RuntimeException ignored) { }
        }
        else if (row == 20 && "FS_GEARS_LOADED".equals(value)) presetLoaded.set(true);
        return true;
    }
    public static String[] previews() { return previews.clone(); }
    public static String[] summary() { return summary.clone(); }
    public static boolean takePresetLoaded() { return presetLoaded.getAndSet(false); }
    public static void clear() { pending = new String[MAX_PRESETS]; previews = new String[MAX_PRESETS]; presetLoaded.set(false); }
    public static void send(String command) {
        if (Client.gameState == 30)
            WorldMapSection0.method3906(command);
    }

    /** Client-thread counterpart of the server's shop interface-close packets. */
    public static void closeMarketWidgets() {
        java.util.List<InterfaceParent> shops = new java.util.ArrayList<>();
        for (InterfaceParent parent = (InterfaceParent) Client.interfaceParents.method6348(); parent != null;
                parent = (InterfaceParent) Client.interfaceParents.method6345()) {
            if (parent.group == 300 || parent.group == 301) shops.add(parent);
        }
        for (InterfaceParent parent : shops) FontName.method5748(parent, true);
        ScriptEvent.method808();
    }

    public static void closeLootKeyWidgets() {
        java.util.List<InterfaceParent> windows = new java.util.ArrayList<>();
        for (InterfaceParent parent = (InterfaceParent) Client.interfaceParents.method6348(); parent != null;
                parent = (InterfaceParent) Client.interfaceParents.method6345()) {
            if (parent.group == 155) windows.add(parent);
        }
        for (InterfaceParent parent : windows) FontName.method5748(parent,true);
        ScriptEvent.method808();
    }
}
