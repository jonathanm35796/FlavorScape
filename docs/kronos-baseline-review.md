# Kronos baseline review

## Owner-authorized private source backup, 2026-10-27

The owner explicitly requested pushing the current project to the existing private `jonathanm35796/FlavorScape` repository. The blanket `main/` ignore has been replaced by exclusions for cache assets, player/runtime data, local credentials, toolchains and generated output. Upstream client notices/licenses are retained. Embedded legacy Discord webhooks and commented payment credentials were removed from source before preparing the backup; optional live announcements now read environment variables documented in `.env.example`. This private backup does not resolve the historical public-redistribution questions recorded below.

## Latest milestone: localhost login passed from main/

On 2026-10-25, the separate main/ working copy built and launched successfully with the portable toolchains. A real desktop-client integration probe logged in the disposable Localtest account, reached state 30 with a local player at (2038,3577,0), and observed 16 incoming game ticks over ten seconds. Both listeners were confirmed bound to 127.0.0.1. Logs are .tools/main-GameServer.log, main-UpdateServer.log, and main-Client.log. See local-running.md for commands.

The only additional game-source change for this verification was a null guard in CustomUtils.loadPkIcons: base cache sprites load asynchronously, so custom icons must wait until the array exists. The startup null-sprite exception did not recur. External item feed JSON warnings remain nonblocking for login. Visual inspection, gameplay interaction, and PvP correctness remain unverified. Source redistribution permission remains unresolved; no Kronos source is published.

## Local checkout findings, resumed 2026-10-25

- Both repositories cloned successfully over HTTPS. Exact upstream HEAD is `66ee03dbbae53ccc652f6e49e9aa4191034b3c8e`, matching the earlier web observation.
- Recursive license/notice filename inventory found only runelite/LICENSE and the client emoji license. Searching Java/text/Markdown server files for copyright and redistribution language found no applicable server grant. This is evidence of an unresolved permission gap, not a legal determination.
- Source remains in ignored research-sources/Kronos-184-Fixed. No Kronos source or cache is staged for publication.
- Confirmed missing root projects kronos-server-live and kronos-client. Build configuration still includes them.
- Important launch defect: kronos-api NettyServer.start uses bootstrap.bind(port) when local=true. The game server passes World.isDev() as that argument, so DEV selects all interfaces despite logging 127.0.0.1. Patch the isolated checkout to explicit loopback before any launch, and inspect other listeners.
- Root Gradle wrapper JAR SHA-256 is 70239e6ca1f0d5e3b2808ef6d82390cf9ad58d3a3a0d271677a51d1b89475857, matching the official Gradle 6.5 wrapper checksum at https://services.gradle.org/distributions/gradle-6.5-wrapper.jar.sha256. Its requested distribution is 8.14; pin the official distribution checksum before executing it.

The sections below record the earlier web-only inspection. Network access is now resolved; server redistribution permission is still unresolved.

Inspected 2026-10-25. Publication is blocked pending redistribution evidence and terminal GitHub connectivity. No source was imported, committed, or pushed.

## Selected foundation and client direction

The owner selected https://github.com/Leif-Yggdrasil/Kronos-184-Fixed and its release at https://rune-server.org/threads/kronos-184-central-server-removed-kilim-replaced.708268/ for evaluation. Keep the bundled RuneLite/OpenOSRS foundation; build and verify locally before FlavorScape branding or gameplay changes. Broad source discovery is not required.

GitHub's web commit history showed commit `66ee03dbbae53ccc652f6e49e9aa4191034b3c8e` (Update README.md, July 5, 2026):
https://github.com/Leif-Yggdrasil/Kronos-184-Fixed/commit/66ee03dbbae53ccc652f6e49e9aa4191034b3c8e

This is a web-observed candidate revision, not a locally fetched or verified HEAD. Reconcile against Git before importing.

## Licensing gate

- The root and Kronos-master directory listings inspected did not show a project-wide license. The server directory listing also did not show one. This is not a recursive file/header audit; cloning failed.
- The bundled client has a BSD 2-Clause license at https://github.com/Leif-Yggdrasil/Kronos-184-Fixed/blob/main/Kronos-master/runelite/LICENSE. Source notices, conditions, and disclaimer must be retained; binary distributions must reproduce them in accompanying materials. The client README also directs readers to individual file headers.
- That client license does not establish redistribution permission for the server, deobfuscated game code, cache, website, artwork, bundled binaries, or other contributors' modifications.
- The inspected release text credits Advocatus and the Kronos, Runite, OS-Fatality, and OS-Scape teams. It invites contributions but does not supply explicit redistribution terms for the entire bundle.
- Specific unresolved requirement: locate applicable server/source licenses and provenance, or obtain a redistribution grant from the relevant rights holders. Audit file-level notices and third-party code once the isolated checkout is available. Public availability and credits alone are insufficient evidence for this import.
- The repository contains a Kronos-master/Cache directory. Do not import it or other game assets whose rights have not been established. No assets were downloaded here.

## Build inspection through public source pages

No scripts or wrappers were executed.

- Server root build.gradle sets Java source/target 21 and Kotlin JVM target 21. Kotlin plugin is 2.1.21.
- Server and client wrapper properties both request Gradle 8.14-all. Neither inspected properties file specifies distributionSha256Sum. Verify wrapper JAR provenance and distribution integrity before execution.
- Client build.gradle.kts sets Java source/target 1.8. This verifies the compilation target, not successful JDK 8 build/runtime compatibility.
- Server settings.gradle includes kronos-server-live and kronos-client, which were absent from the inspected top-level directory listing. Determine whether these are stale/optional projects and whether aggregate tasks fail before changing anything.
- Server repositories include mavenLocal, an old Bintray endpoint, and Lombok edge releases. Client repositories include mavenLocal, mvnrepository.com/artifact, RuneLite hosting, raw GitHub hosting, and an optional NEXUS-URL environment override. Review dependency resolution and actual availability before builds; buildability remains unverified.
- DEV admin/password-bypass behavior remains an upstream README claim, not a traced code finding. Review listener binding and authentication before launching; keep development services local.

Sources:
- https://raw.githubusercontent.com/Leif-Yggdrasil/Kronos-184-Fixed/main/Kronos-master/build.gradle
- https://raw.githubusercontent.com/Leif-Yggdrasil/Kronos-184-Fixed/main/Kronos-master/settings.gradle
- https://raw.githubusercontent.com/Leif-Yggdrasil/Kronos-184-Fixed/main/Kronos-master/gradle/wrapper/gradle-wrapper.properties
- https://raw.githubusercontent.com/Leif-Yggdrasil/Kronos-184-Fixed/main/Kronos-master/runelite/build.gradle.kts
- https://raw.githubusercontent.com/Leif-Yggdrasil/Kronos-184-Fixed/main/Kronos-master/runelite/gradle/wrapper/gradle-wrapper.properties

## Resume sequence

1. Restore terminal GitHub HTTPS access; then reassess CLI authentication without exposing tokens. Do not assume the cached credential is invalid solely from an offline check.
2. Clone into ignored research-sources/Kronos-184-Fixed and verify the exact commit, recursive license/header inventory, provenance, bundled binaries, configuration, and assets.
3. Fetch FlavorScape separately and inspect its history. The owner previously observed main at a75b93d6510013f67fdae2fbbe5b7231dfb997e7; that was not reverified here. Preserve remote history and local documentation, including Start.txt. Do not initialize an unrelated baseline or force-push.
4. Only once redistribution evidence supports it, import reviewed source with upstream notices and attribution; exclude tools, build output, secrets, personal settings, player data, and unverified assets. Review staged contents, commit, and push. The owner already authorized this conditional baseline push.
5. Install isolated required JDKs, inspect remaining scripts/dependencies, then build and launch locally. Login, gameplay, PvP, networking, and tick behavior are entirely unverified.

## Additional verified local code findings

- DEV administrator assignment and password bypass are confirmed in kronos-server/src/main/java/io/ruin/model/entity/player/PlayerFile.java, not merely README claims. Account JSON is saved through a single-thread save executor.
- Server.worker uses a 600 ms period. ProcessWorker uses a single-thread scheduled executor with scheduleAtFixedRate. This establishes intended scheduling only; actual tick stability and combat timing remain untested.
- The bundled Cache directory contains the dat2 file and index files. Presence does not prove cache compatibility or redistribution rights; it stays ignored with the entire research checkout.
- Before launch, the isolated NettyServer.java was patched from bootstrap.bind(port) to bootstrap.bind(host, port), and the root wrapper properties received the official Gradle 8.14 all-distribution SHA-256. These are uncommitted local evaluation changes, not published upstream source.

## Build and startup results

- Installed portable Temurin 21.0.12.1+1 and 8u504-b01 in .tools/, verifying ZIP checksums against Adoptium metadata. No administrator installation or global Java configuration change.
- Server and update-server classes compiled successfully on JDK 21 using Gradle 8.14. Missing optional project directories did not prevent these targeted tasks; aggregate deployment tasks remain unverified.
- Client classes initially failed on missing netscape.javascript.JSObject under Temurin 8. The isolated checkout now replaces three browser-plugin API imports/call sites with DesktopBrowserBridge, which explicitly rejects unavailable browser-page JavaScript calls. Existing callers already handle failures. Client classes then compiled successfully. This is a local evaluation adaptation, not an upstream build pass without changes.
- Local server properties were created from examples, with world_address set to loopback. Game server completed startup in 12.5 seconds. OS listener inspection confirmed 127.0.0.1:13302 and 127.0.0.1:7304. An actual revision-184 update handshake returned the expected success byte 0.
- Client process launched through the root RuneLite run task. Runtime log records an external item-stat JSON parsing warning and repeated NullPointerException entries in CustomUtils.loadPkIcons while sprites load; these entries stopped after the initial loading interval. Their user-visible impact remains unverified.
- Desktop inspection helper initialization timed out, preventing visual verification in this session. Login, basic gameplay, and PvP timing/behavior have NOT passed verification.
- Logs: .tools/server-build.log, client-build.log (initial failure), client-build-retry.log (success), local-servers.log, game-server.log, client-run.log. These remain local and ignored.

### Reproduce local startup

Use separate PowerShell sessions from the workspace. Set JAVA_HOME to the relevant portable JDK and GRADLE_USER_HOME to the workspace's .tools/gradle-home. From research-sources/Kronos-184-Fixed/Kronos-master, run .\gradlew.bat :kronos-update-server:run or :kronos-server:run with JDK 21. From its runelite directory, run .\gradlew.bat run with JDK 8. The DEV client configuration already points to 127.0.0.1. Do not launch duplicate instances while ports are in use.

Server/source redistribution evidence is still required before the source baseline can be published. Only FlavorScape's own documentation/setup files have been pushed.
