package io.ruin.api.protocol.world;

public enum WorldType {
    ECO("Llyeta", "https://github.com/jonathanm35796/Llyeta"),
    BETA("Llyeta BETA", "https://github.com/jonathanm35796/Llyeta"),
    PVP("Llyeta", "https://github.com/jonathanm35796/Llyeta"),
    DEV("Development", "http://127.0.0.1");

    WorldType(String worldName, String websiteUrl) {
        this.worldName = worldName;
        this.websiteUrl = websiteUrl;
    }

    private String worldName, websiteUrl;

    public String getWorldName() {
        return worldName;
    }

    public String getWebsiteUrl() { return websiteUrl; }
}