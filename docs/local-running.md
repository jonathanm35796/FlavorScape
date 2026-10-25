# Run FlavorScape locally

Verified 2026-10-25: both servers and the actual client ran from main/. The Localtest login reached game state 30 with a player at (2038,3577,0), and 16 game ticks arrived over ten seconds. This establishes a working local login; visual inspection and PvP testing remain separate work.

The working source is `main/`, based on the selected Kronos release. The reference clone is separate. Server and client dependencies are resolved by the checked-in Gradle wrappers and cached under `.tools/gradle-home`.

Installed portable toolchains:

- Server/update server: Temurin JDK 21.0.12.1+1, `.tools/jdk21/jdk-21.0.12.1+1`.
- Desktop client: Temurin JDK 8u504-b01, `.tools/jdk8/jdk8u504-b01`.
- Gradle 8.14, downloaded and checksum-verified by the project wrappers.

No global Gradle, Maven, database, or additional IDE is required for this local DEV configuration.

From `C:\Users\Jonathan\Desktop\FlavorScape`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-local.ps1
```

This builds the components and starts the update server, game server, and desktop client. Helper consoles stay hidden; the client has its normal window. Game and update listeners use `127.0.0.1:13302` and `127.0.0.1:7304`. The client DEV configuration already points to localhost. Use a disposable test username and password; DEV accounts receive administrator privileges and skip password verification.

Stop the launched components:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\stop-local.ps1
```

Compile without launching:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-local.ps1 -BuildOnly
```

Run the actual client with an automated localhost login probe:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-local.ps1 -VerifyLogin
```

The probe uses the disposable `Localtest` account. It refuses non-loopback game/update addresses, waits for the login screen, submits a local DEV login on the client thread, and checks for a player and advancing game ticks after ten seconds in the logged-in state. It leaves the client open. Look for `LOCAL_SMOKE PASS` or `LOCAL_SMOKE FAIL` in `.tools/main-Client.log`. This checks the login path, not combat accuracy or UI appearance.

Logs are `.tools/main-server-build.log`, `.tools/main-client-build.log`, `.tools/main-UpdateServer.log`, `.tools/main-GameServer.log`, and `.tools/main-Client.log`. Local data and downloaded binaries stay excluded from GitHub.
