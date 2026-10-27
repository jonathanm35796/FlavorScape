package net.runelite.client.plugins.flavorscapegears;

import net.runelite.standalone.FlavorScapeCommands;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PresetLoadNotificationTest {
    private static final int GROUP = 32766 << 16;
    @Before public void reset() { FlavorScapeCommands.clear(); }

    @Test public void previewRefreshDoesNotCloseTheInterface() {
        FlavorScapeCommands.receive(GROUP, "FS_GEARS_BEGIN");
        FlavorScapeCommands.receive(GROUP | 1, "Custom preset|-1|MODERN|Current stats");
        FlavorScapeCommands.receive(GROUP | 18, "FS_GEARS_END");
        assertFalse(FlavorScapeCommands.takePresetLoaded());
    }
    @Test public void serverCompletionClosesOnce() {
        FlavorScapeCommands.receive(GROUP | 20, "unknown");
        assertFalse(FlavorScapeCommands.takePresetLoaded());
        FlavorScapeCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        assertTrue(FlavorScapeCommands.takePresetLoaded());
        assertFalse(FlavorScapeCommands.takePresetLoaded());
    }
    @Test public void logoutClearsPendingCompletion() {
        FlavorScapeCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        FlavorScapeCommands.clear();
        assertFalse(FlavorScapeCommands.takePresetLoaded());
    }

    @Test public void staleCompletionDoesNotClosePresets() throws Exception {
        FlavorScapeGearsPlugin plugin = new FlavorScapeGearsPlugin();
        set(plugin, "open", true);
        FlavorScapeCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        plugin.handlePresetLoadNotification();
        assertTrue((boolean) get(plugin, "open"));
    }

    @Test public void requestedLoadCompletionClosesPresets() throws Exception {
        FlavorScapeGearsPlugin plugin = new FlavorScapeGearsPlugin();
        set(plugin, "open", true);
        set(plugin, "awaitingPresetLoad", true);
        FlavorScapeCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        plugin.handlePresetLoadNotification();
        assertFalse((boolean) get(plugin, "open"));
        assertFalse((boolean) get(plugin, "awaitingPresetLoad"));
    }

    @Test public void closingCancelsLateLoadCompletion() throws Exception {
        FlavorScapeGearsPlugin plugin = new FlavorScapeGearsPlugin();
        set(plugin, "open", true);
        set(plugin, "awaitingPresetLoad", true);
        plugin.closePanel();
        set(plugin, "open", true);
        FlavorScapeCommands.receive(GROUP | 20, "FS_GEARS_LOADED");
        plugin.handlePresetLoadNotification();
        assertTrue((boolean) get(plugin, "open"));
        assertFalse((boolean) get(plugin, "awaitingPresetLoad"));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = FlavorScapeGearsPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = FlavorScapeGearsPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
