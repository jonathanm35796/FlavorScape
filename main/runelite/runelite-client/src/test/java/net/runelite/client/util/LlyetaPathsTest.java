package net.runelite.client.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class LlyetaPathsTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void migratesProfilesAndPreferencesWithoutOverwritingExistingSettings() throws Exception
    {
        Path original = temporary.newFolder("original").toPath();
        Path destination = temporary.newFolder("current").toPath();
        Files.createDirectory(original.resolve("profiles"));
        Files.write(original.resolve("profiles/account.properties"), "profile".getBytes(StandardCharsets.UTF_8));
        Files.write(original.resolve("settings.properties"), "old".getBytes(StandardCharsets.UTF_8));
        Files.write(destination.resolve("settings.properties"), "current".getBytes(StandardCharsets.UTF_8));
        Files.write(original.resolve("flavor" + "scape_preferences.dat"), "identity".getBytes(StandardCharsets.UTF_8));
        LlyetaPaths.migrate(original, destination);
        assertEquals("profile", read(destination.resolve("profiles/account.properties")));
        assertEquals("current", read(destination.resolve("settings.properties")));
        assertEquals("identity", read(destination.resolve("llyeta_preferences.dat")));
        assertEquals("old", read(original.resolve("settings.properties")));
        Files.write(destination.resolve("llyeta_preferences.dat"), "new identity".getBytes(StandardCharsets.UTF_8));
        LlyetaPaths.migrate(original, destination);
        assertEquals("new identity", read(destination.resolve("llyeta_preferences.dat")));
    }

    @Test public void cleanInstallDoesNotRequireLegacySettings() throws Exception
    {
        Path root = temporary.getRoot().toPath();
        LlyetaPaths.migrate(root.resolve("missing"), root.resolve("new"));
        assertFalse(Files.exists(root.resolve("missing")));
    }

    private static String read(Path path) throws Exception
    {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
