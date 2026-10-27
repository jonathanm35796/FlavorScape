package io.ruin.api.protocol.world;

public enum WorldType {
    ECO("FlavorScape", "https://github.com/jonathanm35796/FlavorScape"),
    BETA("FlavorScape BETA", "https://github.com/jonathanm35796/FlavorScape"),
    PVP("FlavorScape", "https://github.com/jonathanm35796/FlavorScape"),
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