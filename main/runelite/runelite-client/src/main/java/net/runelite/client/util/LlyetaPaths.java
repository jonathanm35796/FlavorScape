package net.runelite.client.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** Keeps existing local preferences on the first launch under the new name. */
public final class LlyetaPaths
{
    private static final String LEGACY_DIRECTORY = ".flavor" + "scape";
    private static final String LEGACY_PREFERENCES = "flavor" + "scape_preferences.dat";

    private LlyetaPaths() { }

    public static File directory(File parent)
    {
        File destination = new File(parent, ".llyeta");
        try
        {
            migrate(new File(parent, LEGACY_DIRECTORY).toPath(), destination.toPath());
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Could not migrate Llyeta settings; original settings were retained.", ex);
        }
        return destination;
    }

    static void migrate(final Path source, final Path destination) throws IOException
    {
        Path marker = destination.resolve(".settings-migrated");
        if (Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS))
        {
            return;
        }
        if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS))
        {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
                {
                    Files.createDirectories(destination.resolve(source.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    Path target = destination.resolve(source.relativize(file));
                    if (attrs.isRegularFile() && !Files.exists(target, LinkOption.NOFOLLOW_LINKS))
                    {
                        Files.copy(file, target);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Path oldPreferences = destination.resolve(LEGACY_PREFERENCES);
        Path preferences = destination.resolve("llyeta_preferences.dat");
        if (Files.isRegularFile(oldPreferences, LinkOption.NOFOLLOW_LINKS)
            && !Files.exists(preferences, LinkOption.NOFOLLOW_LINKS))
        {
            Files.copy(oldPreferences, preferences);
        }
        if (Files.isDirectory(destination, LinkOption.NOFOLLOW_LINKS))
        {
            Files.createFile(marker);
        }
    }
}
