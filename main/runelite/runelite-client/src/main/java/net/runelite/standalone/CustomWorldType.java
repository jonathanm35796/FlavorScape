package net.runelite.standalone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum CustomWorldType {

    PVP("FlavorScape", "127.0.0.1", "127.0.0.1", "127.0.0.1"),
    ECO("FlavorScape", "127.0.0.1", "127.0.0.1", "127.0.0.1"),
    BETA("FlavorScape BETA", "127.0.0.1", "127.0.0.1", "127.0.0.1"),
    DEV("FlavorScape", "127.0.0.1", "127.0.0.1", "127.0.0.1");

    private final String name;
    private final String url;
    private final String gameServerAddress;
    private final String fileServerAddress;

}
