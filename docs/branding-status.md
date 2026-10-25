# FlavorScape branding

Applied to the main/ working copy on 2026-10-25. The upstream reference checkout and historical provenance remain unchanged.

Updated server/client names, window and splash titles, login and update messages, announcements, help text, NPC labels, custom item descriptions, module names, internal project-specific identifiers, preference names, resource references, and launcher branding. Module directories are now flavorscape-api, flavorscape-server, flavorscape-update-server, flavorscape-launcher, and flavorscape-webhooks. Startup scripts use the new Gradle task names. Character data stayed with the renamed server directory.

Cached NPC/item/object display names are translated when definitions load, without rewriting the binary cache. The real OSRS Kronos seed and farming plants retain their names and IDs. Legacy text tokens remain in these translation routines so old cache strings can be recognized.

Client settings were copied from the user's .kronos directory to .flavorscape without overwriting existing destination settings. Old settings were retained for recovery. Edited files and character data were backed up under the ignored .tools/branding-backup-* directory before the restart, after observing a fresh player autosave.

User-facing old website links now point to the owner's FlavorScape GitHub project. Optional forum integration, hiscores, and launcher downloads require FlavorScape configuration instead of contacting old branded services. The local launch script remains the supported way to start this development setup.

Intentionally retained old-name references:

- Original copyright holders, authors, @project credits, licenses, source repository URLs, and historical investigation notes.
- The actual upstream Kronos-Resources jav_config.ws URL, which is still a runtime dependency. Renaming that URL would break startup.
- Genuine OSRS farming content and explicit legacy-string translation code.
- Reference research sources, old backups/settings, binary cache, and historical generated logs/build artifacts. These are not current source branding.

Artwork was edited with the built-in imagegen tool and inspected before integration. Full generated assets are in main/branding/login-background.png, app-icon.png, and launcher-wordmark.png. Runtime files are main/runelite/runelite-client/src/main/resources/flavorscape.png, img/leftTitleBackground.png, img/rightTitleBackground.png, and main/flavorscape-launcher/src/main/resources/logo.png. Geometric resizing and splitting adapt the generated PNGs to the existing resource dimensions. The prompt set is recorded in main/branding/README.md.

Both server components and the desktop client compile under the existing portable toolchains after the rename. The rebranded server logs FlavorScape World, and the client frame reports title=FlavorScape. The actual-client Localtest probe passed: game state 30, a player at (3262,3259,0), and 16 received ticks over ten seconds. Both listeners remain on 127.0.0.1. The existing Jon character's JSON hash matches its pre-restart backup. The start script now waits for both listeners before opening the client, fixing an observed early-login race. Servers and the client were left running after verification.

Working game source and artwork remain local under ignored main/ pending the existing redistribution review. No upstream source or unverified game assets are published by these documentation/tooling changes.
