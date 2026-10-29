package net.runelite.standalone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum CustomWorldType {

    PVP("Llyeta", "127.0.0.1", "127.0.0.1", "127.0.0.1"),
    ECO("Llyeta", "127.0.0.1", "127.0.0.1", "127.0.0.1"),
    BETA("Llyeta BETA", "127.0.0.1", "127.0.0.1", "127.0.0.1"),
    DEV("Llyeta", "127.0.0.1", "127.0.0.1", "127.0.0.1");

    private final String name;
    private final String url;
    private final String gameServerAddress;
    private final String fileServerAddress;

}
