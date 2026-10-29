package net.runelite.client.plugins.lletyagears;

import net.runelite.standalone.LletyaCommands;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PresetLoadNotificationTest {
    private static final int GROUP = 32766 << 16;
    @Before public void reset() { LletyaCommands.clear(); }

    @Test public void previewRefreshDoesNotCloseTheInterface() {
        LletyaCommands.receive(GROUP, "FS_GEARS_BEGIN");
        LletyaCommands.receive(GROUP | 1, "Custom preset|-1|MODERN|Current stats");
        LletyaCommands.receive(GROUP | 18, "FS_GEARS_END");
        assertFalse(LletyaCommands.takePresetLoaded());
    }
    @Test public void serverCompletionClosesOnce() {
        LletyaCommands.receive(GROUP | 20, "unknown");
        assertFalse(LletyaCommands.takePresetLoaded());
        LletyaCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        assertTrue(LletyaCommands.takePresetLoaded());
        assertFalse(LletyaCommands.takePresetLoaded());
    }
    @Test public void logoutClearsPendingCompletion() {
        LletyaCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        LletyaCommands.clear();
        assertFalse(LletyaCommands.takePresetLoaded());
    }

    @Test public void staleCompletionDoesNotClosePresets() throws Exception {
        LletyaGearsPlugin plugin = new LletyaGearsPlugin();
        set(plugin, "open", true);
        LletyaCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        plugin.handlePresetLoadNotification();
        assertTrue((boolean) get(plugin, "open"));
    }

    @Test public void requestedLoadCompletionClosesPresets() throws Exception {
        LletyaGearsPlugin plugin = new LletyaGearsPlugin();
        set(plugin, "open", true);
        set(plugin, "awaitingPresetLoad", true);
        LletyaCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        plugin.handlePresetLoadNotification();
        assertFalse((boolean) get(plugin, "open"));
        assertFalse((boolean) get(plugin, "awaitingPresetLoad"));
    }

    @Test public void closingCancelsLateLoadCompletion() throws Exception {
        LletyaGearsPlugin plugin = new LletyaGearsPlugin();
        set(plugin, "open", true);
        set(plugin, "awaitingPresetLoad", true);
        plugin.closePanel();
        set(plugin, "open", true);
        LletyaCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        plugin.handlePresetLoadNotification();
        assertTrue((boolean) get(plugin, "open"));
        assertFalse((boolean) get(plugin, "awaitingPresetLoad"));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = LletyaGearsPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = LletyaGearsPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
