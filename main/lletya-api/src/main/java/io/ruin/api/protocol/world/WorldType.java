package io.ruin.api.protocol.world;

public enum WorldType {
    ECO("Lletya", "https://github.com/jonathanm35796/Lletya"),
    BETA("Lletya BETA", "https://github.com/jonathanm35796/Lletya"),
    PVP("Lletya", "https://github.com/jonathanm35796/Lletya"),
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