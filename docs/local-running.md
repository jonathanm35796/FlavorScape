# Run Lletya locally

Verified 2026-10-25: both servers and the actual client ran from main/. The Localtest login reached game state 30 with a player at (2038,3577,0), and 16 game ticks arrived over ten seconds. This establishes a working local login; visual inspection and PvP testing remain separate work.

The working source is `main/`, based on the selected Kronos release. The reference clone is separate. Server and client dependencies are resolved by the checked-in Gradle wrappers and cached under `.tools/gradle-home`.

Installed portable toolchains:

- Server/update server: Temurin JDK 21.0.12.1+1, `.tools/jdk21/jdk-21.0.12.1+1`.
- Desktop client: Temurin JDK 8u504-b01, `.tools/jdk8/jdk8u504-b01`.
- Gradle 8.14, downloaded and checksum-verified by the project wrappers.

No global Gradle, Maven, database, or additional IDE is required for this local DEV configuration.

From `C:\Users\Jonathan\Desktop\Lletya`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-local.ps1
```

This builds the components and starts the update server and game server, waits for both loopback listeners, then opens the desktop client. Helper consoles stay hidden; the client has its normal window. Game and update listeners use `127.0.0.1:13302` and `127.0.0.1:7304`. The client DEV configuration already points to localhost. Use a disposable test username and password; DEV accounts receive administrator privileges and skip password verification.

For visible, double-clickable launchers, open `Start Server.bat` to build and start the update/game servers in separate consoles, then open `Start Client.bat` for a client console and client window. Server/client output is shown live and also written under `.tools`. Keep the consoles open while using each component; use `Stop Server.bat` to stop the recorded server session.

To open additional clients while the servers are running, run this command in a separate PowerShell window for each client:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-local.ps1 -Component Client
```

Each client launch writes to its own `.tools/main-Client-<pid>.log` file.

Server modules now live at main/lletya-server and main/lletya-update-server. Client settings use the user's .lletya folder; earlier .kronos settings were copied during the branding migration and retained for recovery.

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

The probe uses the disposable `Localtest` account. It refuses non-loopback game/update addresses, waits for the login screen, submits a local DEV login on the client thread, and checks for a player and advancing game ticks after ten seconds in the logged-in state. It leaves the client open. Look for `LOCAL_SMOKE PASS` or `LOCAL_SMOKE FAIL` in the corresponding `.tools/main-Client-<pid>.log`. This checks the login path, not combat accuracy or UI appearance.

Logs are `.tools/main-server-build.log`, `.tools/main-client-build.log`, `.tools/main-UpdateServer.log`, `.tools/main-GameServer.log`, and `.tools/main-Client-<pid>.log` (one per client process). Local data and downloaded binaries stay excluded from GitHub.

Run the API/server regressions for currency migration, shops, routes, presets, and the command catalogue:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-roadmap.ps1
```

For the extended actual-client gear check, use a disposable DEV account that has completed onboarding and has an empty custom box. Set `LLETYA_SMOKE_USERNAME` to that account and `LLETYA_VERIFY_ROADMAP=1`, then launch `-Component Client -VerifyLogin` against running local servers. This changes the test character's gear, levels, bank, and saved presets: it loads all six built-ins, saves/loads a custom inventory and prayer, and tests shortcuts and the gear button. Optional `LLETYA_VERIFY_HOME=spawn` plus `LLETYA_HOME_SCREENSHOT=<workspace PNG path>` captures the final frame after returning home. Look for `GEARS_SMOKE PASS`; the client remains open.

For the Quest layout check alone, set `LLETYA_VERIFY_QUEST_TAB=1` instead of `LLETYA_VERIFY_ROADMAP`. It verifies tab visibility, clicks the dragon helm, checks that the panel fits within the game viewport, and tests Achievements/Back navigation. Look for `QUEST_TAB_SMOKE PASS`.

`LLETYA_VERIFY_CAMERA_INPUT=1` checks the native mouse handler: presses outside either interface and releases over it must reset held-button state, and middle/Alt-left camera gestures must pass through. `LLETYA_VERIFY_PRESET_CLOSE=1` checks that a rejected load keeps the panel open and a successful load closes it. Run these probes separately on a disposable account with `-Component Client -VerifyLogin`; the preset-close probe loads built-in gear. Look for `CAMERA_INPUT_SMOKE PASS` or `PRESET_CLOSE_SMOKE PASS`.

The six targeted client regressions can be run from `main/runelite` with JDK 8 using `:runelite-client:test --tests 'net.runelite.client.plugins.lletyagears.*'`.

For the classic spellbook check, set `LLETYA_VERIFY_SPELLBOOKS=1`, `LLETYA_SMOKE_USERNAME` to a disposable DEV account that has completed onboarding, and `LLETYA_SPELLBOOK_SCREENSHOTS` to a workspace directory. Launch `-Component Client -VerifyLogin`. The probe checks the login default, switches books through server commands, verifies all 70 Standard, 26 Ancient and 45 Lunar spell widgets in seven, four and six columns, and captures each rendered grid. Look for `SPELLBOOK_SMOKE PASS`. The account finishes on Lunar and the client remains open.

The market probe (`LLETYA_VERIFY_MARKET=1`) now confirms the inventory-empty dialogue on its disposable account before testing. It verifies that left-click only prints the price, then buys 1, 5 and 10 through the context menu and types 7 into the Buy X numeric dialogue. It also checks 40% buyback, selector dismissal, both shops, scrolling, search and close. The numeric dialogue screenshot is saved alongside `LLETYA_MARKET_SCREENSHOT` with `.buyx.png` appended. Use only a disposable character: this probe clears its inventory and purchases items.

### Bounty Hunter equipment

Vesta's, Statius's, Morrigan's and Zuriel's equipment can remain equipped anywhere. Their equipment bonuses and weapon effects activate only at Wilderness levels **1 through 5**, with Bounty Hunter targeting enabled for the player's current area. This server uses Wilderness-based Bounty Hunter rather than a separate crater; `All Wilderness` targeting covers levels 1–5, while `Edgeville Only` also requires being inside its Edgeville bounds. An assigned target is not required for activation.

At level 6 or above, outside the Wilderness, or with targeting disabled, those items contribute no equipment bonuses or magic damage. Their weapons cannot attack or cast spells in that state. Normal equipment remains active, and item weight still applies. Crossing the boundary refreshes the bonuses without requiring the player to re-equip. This replaces the former 25% out-of-Wilderness penalty for these 17 base items and also covers the cache's three additional weapon variants (23615, 23617 and 23620). Those variants inherit their base weapon's equipment definition; noted and placeholder items remain unequippable. Inactive equipment does not lose degradation charges when hit.

The max hit dummy (NPC 2668) is an exception: Ancient Warriors' weapons can be used there anywhere, and their equipment bonuses are temporarily enabled for the test attack. Players can arm a special before clicking the dummy. Sufficient starting energy is still required; the test refunds only the energy spent and stops after one attack. Testing does not enable the gear against other targets, and Vesta's spear cannot splash nearby targets during a dummy test.
