package net.runelite.standalone;

public final class LootKeySpriteFixture {
    private LootKeySpriteFixture() {}
    public static java.awt.image.BufferedImage read(java.nio.file.Path path)throws Exception{
        Tiles.method1200(java.nio.file.Files.readAllBytes(path));
        return AbstractWorldMapIcon.method720()[0].toBufferedImage();
    }
}
