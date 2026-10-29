package net.runelite.standalone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum CustomWorldType {

    PVP("Lletya", "127.0.0.1", "127.0.0.1", "127.0.0.1"),
    ECO("Lletya", "127.0.0.1", "127.0.0.1", "127.0.0.1"),
    BETA("Lletya BETA", "127.0.0.1", "127.0.0.1", "127.0.0.1"),
    DEV("Lletya", "127.0.0.1", "127.0.0.1", "127.0.0.1");

    private final String name;
    private final String url;
    private final String gameServerAddress;
    private final String fileServerAddress;

}
