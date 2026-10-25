# Environment status

## Commands catalogue update: 2026-10-25

- ::commands is now generated from all active server command switch cases and filtered using the same access policy as execution. ::staffcommands opens the accessible staff/Owner subset. Lists support paging and search.
- Eight permission/index tests passed. An actual-client probe verified the full menu, a save search, and the staff menu through game packets and interface widgets after a fresh-save backup/restart.
- DEV automatic Owner grants remain unchanged. Legacy dice/flower override entry points are now Owner-only and cannot be enabled by an ordinary account using the old hidden password.
- See commands.md for syntax, policy details, and the distinction from client-only/plugin commands.

## FlavorScape branding update: 2026-10-25

- Working module directories and Gradle tasks now use flavorscape-* names. The run-local.ps1 command is unchanged and waits for both server listeners before starting the client.
- Existing player files were preserved under main/flavorscape-server/server-data/_saved/characters. A fresh autosave was observed and the saved files backed up before the first branding restart.
- Client window, splash/login text, resource artwork, help text, world names, and project-specific identifiers were rebranded. Client settings were copied to .flavorscape without removing the originals.
- Original copyright/attribution, historical evidence, genuine OSRS Kronos farming content, and the upstream jav_config.ws dependency remain intact. See branding-status.md for scope and asset paths.
- Rebuilt the renamed components and passed actual-client localhost login again: state 30, a player present, and 16 game ticks received in ten seconds. Runtime logs confirm the FlavorScape window/server titles and loopback-only listeners. The saved Jon character matches the pre-restart backup byte for byte.

## Current local milestone: 2026-10-25, working copy in main

- All components built from main/ using the portable JDKs and pinned Gradle wrappers. Runtime dependencies also resolved successfully.
- Game and update servers started from main/ and listen only on 127.0.0.1:13302 and 127.0.0.1:7304. The earlier research-checkout processes were stopped.
- The actual desktop client from main/runelite completed a localhost DEV login as disposable account Localtest. The integration probe observed game states 10 (login), 20 (connecting), 25 (loading), and 30 (logged in), then confirmed a player at WorldPoint(2038,3577,0) and 16 received game ticks over ten seconds.
- Client and servers were left running after verification. Visual inspection is still unavailable because the desktop helper times out; the successful login result comes from client-thread integration instrumentation, not a screenshot.
- main/ includes a null guard for asynchronously loaded PK sprites, eliminating the earlier startup exception in this run. External item-stat/price feeds still emit JSON warnings; these did not prevent login.
- Use scripts/run-local.ps1 and scripts/stop-local.ps1 as described in local-running.md. No extra global Gradle, Maven, database, or IDE is needed for local DEV operation.
- Combat accuracy, movement and PvP interactions have not been verified. Source publication still awaits server redistribution terms.

Earlier session notes below are historical.

## Resumed session: 2026-10-25

- HTTPS GitHub access now works. GitHub CLI authentication is valid as jonathanm35796; no new credentials were needed. SSH reaches host-key verification, but HTTPS is sufficient.
- Cloned FlavorScape and Kronos into ignored research-sources/. FlavorScape main is a75b93d6510013f67fdae2fbbe5b7231dfb997e7 (one initial README commit).
- Connected this workspace to that existing history by copying the isolated FlavorScape clone's Git metadata, preserving all local files. No force-push or unrelated history was created.
- Kronos checkout is exactly 66ee03dbbae53ccc652f6e49e9aa4191034b3c8e. It remains excluded from publication pending server licensing evidence.
- Portable JDK 21 setup and build verification are in progress under ignored .tools/. Earlier network and authentication failures below are historical.

## Latest check: 2026-10-25, baseline session

- Git 2.56.0.windows.3 is installed at C:\Program Files\Git\cmd\git.exe. GitHub CLI is installed at C:\Program Files\GitHub CLI\gh.exe.
- Git ls-remote against FlavorScape and an isolated Kronos clone both failed to connect to github.com:443. No remote history was fetched.
- gh auth status identifies jonathanm35796 but reports an invalid token. Because terminal connectivity also fails, credential validity is not independently established. Do not log out or replace credentials solely on this result.
- java and javac were not found on PATH; common Java, Eclipse Adoptium, and user .jdks directories did not yield an installation. No JDK was installed.
- Public web research works. No source import, build, local service launch, commit, or push occurred. Workspace remains without a .git directory.
- See [Kronos baseline review](kronos-baseline-review.md) for licensing blockers and the resume sequence. The older notes below describe the previous session.

Checked 2026-10-25 in C:\Users\Jonathan\Desktop\FlavorScape.

- Workspace reads and writes work. Original Start.txt preserved.
- Initially the folder contained only Start.txt; no local Git repository or game source existed.
- Git, GitHub CLI, Java, javac, Gradle, Maven, and Node were absent from PATH. Git and Java were also absent from the common locations checked.
- VS Code is present under the user's local Programs directory.
- The Python command resolves to a WindowsApps alias; a functioning Python installation has not been verified.
- winget resolves to a WindowsApps executable but execution fails with 'The file cannot be accessed by the system'.
- An HTTPS request to the GitHub repository API failed with 'Unable to connect to the remote server'.
- Web research works separately. It does not establish terminal network access or authenticated repository access.

## Remaining setup

1. Run scripts/setup-tools.ps1 in an ordinary PowerShell session with working network and Windows Package Manager.
2. Reopen the shell and authenticate GitHub CLI using browser sign-in.
3. Verify access to jonathanm35796/FlavorScape and inspect its branches/history.
4. Initialize or clone safely, preserving Start.txt and these files; configure commit identity if missing, then commit and push the reviewed project setup.
5. Download isolated source candidates into ignored research-sources/, record exact commits, inspect build requirements, and install the required JDK versions.
6. Execute builds and local client/server checks before recommending a source.

No tools have been installed, no Git commit or push has occurred, and no candidate has passed a build or gameplay check.

## Local toolchain verified during resume

- Portable Temurin JDK 21.0.12.1+1: .tools/jdk21/jdk-21.0.12.1+1 (server).
- Portable Temurin JDK 8u504-b01: .tools/jdk8/jdk8u504-b01 (client).
- Both ZIP SHA-256 values matched Adoptium's API metadata before extraction. No system Java settings or administrator installation was needed.
- Gradle 8.14 wrappers run successfully. Gradle cache is isolated at .tools/gradle-home.
- :kronos-server:classes and :kronos-update-server:classes completed successfully with JDK 21 (upstream warnings remain).
- Both servers started with explicit loopback binding: 127.0.0.1:13302 and 127.0.0.1:7304. Revision-184 JS5 handshake returned byte 0, as expected.
- Initial client compilation failed because Temurin 8 lacks netscape.javascript.JSObject. A local desktop-only compatibility bridge disables browser-page callbacks; :runelite-client:classes then completed successfully with JDK 8. No gameplay behavior is claimed verified.
- Build/startup logs are under ignored .tools/. Research source, cache, local configuration, and generated player data remain excluded from the FlavorScape repository.
