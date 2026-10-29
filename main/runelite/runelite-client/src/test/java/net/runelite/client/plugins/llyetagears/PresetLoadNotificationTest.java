package net.runelite.client.plugins.llyetagears;

import net.runelite.standalone.LlyetaCommands;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PresetLoadNotificationTest {
    private static final int GROUP = 32766 << 16;
    @Before public void reset() { LlyetaCommands.clear(); }

    @Test public void previewRefreshDoesNotCloseTheInterface() {
        LlyetaCommands.receive(GROUP, "FS_GEARS_BEGIN");
        LlyetaCommands.receive(GROUP | 1, "Custom preset|-1|MODERN|Current stats");
        LlyetaCommands.receive(GROUP | 18, "FS_GEARS_END");
        assertFalse(LlyetaCommands.takePresetLoaded());
    }
    @Test public void serverCompletionClosesOnce() {
        LlyetaCommands.receive(GROUP | 20, "unknown");
        assertFalse(LlyetaCommands.takePresetLoaded());
        LlyetaCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        assertTrue(LlyetaCommands.takePresetLoaded());
        assertFalse(LlyetaCommands.takePresetLoaded());
    }
    @Test public void logoutClearsPendingCompletion() {
        LlyetaCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        LlyetaCommands.clear();
        assertFalse(LlyetaCommands.takePresetLoaded());
    }

    @Test public void staleCompletionDoesNotClosePresets() throws Exception {
        LlyetaGearsPlugin plugin = new LlyetaGearsPlugin();
        set(plugin, "open", true);
        LlyetaCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        plugin.handlePresetLoadNotification();
        assertTrue((boolean) get(plugin, "open"));
    }

    @Test public void requestedLoadCompletionClosesPresets() throws Exception {
        LlyetaGearsPlugin plugin = new LlyetaGearsPlugin();
        set(plugin, "open", true);
        set(plugin, "awaitingPresetLoad", true);
        LlyetaCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        plugin.handlePresetLoadNotification();
        assertFalse((boolean) get(plugin, "open"));
        assertFalse((boolean) get(plugin, "awaitingPresetLoad"));
    }

    @Test public void closingCancelsLateLoadCompletion() throws Exception {
        LlyetaGearsPlugin plugin = new LlyetaGearsPlugin();
        set(plugin, "open", true);
        set(plugin, "awaitingPresetLoad", true);
        plugin.closePanel();
        set(plugin, "open", true);
        LlyetaCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        plugin.handlePresetLoadNotification();
        assertTrue((boolean) get(plugin, "open"));
        assertFalse((boolean) get(plugin, "awaitingPresetLoad"));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = LlyetaGearsPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = LlyetaGearsPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
