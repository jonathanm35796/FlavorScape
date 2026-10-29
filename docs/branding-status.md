# Llyeta branding

Renamed on 28 October 2026 using **Llyeta**, the spelling in the owner's final instruction. The working checkout remains at its existing Desktop path so the open workspace continues to work.

The current source uses Llyeta for client/server/world names, window and splash titles, loading and login text, announcements, help, custom item/NPC names, Gradle modules, launcher properties, preference filenames, internal custom classes, plugin packages, environment variable names and raster branding. Modules are `llyeta-api`, `llyeta-server`, `llyeta-update-server`, `llyeta-launcher` and `llyeta-webhooks`; local scripts use those task names.

The original loading scene features a wilderness courtyard, armored statue, notice board and purple portal under a dark red sky. It contains no UI overlays, lettering or lava. The launcher has a matching silver LLYETA wordmark and the application has an L shield icon. Full-resolution originals and prompts are in [branding](../main/branding/README.md). Runtime derivatives preserve the native login split and application resource sizes; regenerate them with `scripts/update-branding.ps1`.

## Existing saves and settings

The local game and update server were already running when this rename began. Their active data and ignored builds remain in the previous module directories until shutdown; source files moved to the new module names. Before launching Llyeta, stop the existing servers with `Stop Server.bat`. The startup script then moves the previous `server-data`, `server.properties` and `local.properties` into the new modules, only while both ports are stopped, and refuses duplicate source/destination state. Build-only commands do not migrate or modify live data. Direct Gradle `run` commands do not perform this migration; use the supported startup scripts for the first launch.

On the first launch of the renamed client, local profiles and preferences are copied to `.llyeta` in the user-home and platform preference locations. Existing destination files are never overwritten; original settings remain available. The device preference filename is migrated too. A completion marker makes this a one-time migration. The migration runs at application startup, without side effects from merely loading client classes in tests.

## Links and historical content

The owner's Discord invite stays [Llyeta Discord](https://discord.gg/QJDZX7U4y). The GitHub repository is now [jonathanm35796/Llyeta](https://github.com/jonathanm35796/Llyeta), and this checkout's origin and current project links use the new URL. An existing checkout's physical path, existing active runtime data, saved historical logs/backups, explicit compatibility strings, third-party copyright/author credits and upstream service URLs stay intact. Genuine OSRS farming names and cached IDs stay intact. Legacy cached text is translated when loaded.

## Verification

Current validation is recorded in the README. The first WMI-based listener check was denied by Windows and treated the result as no listeners; a guard check moved runtime state prematurely. The original state was restored without overwriting newer saves, and its snapshot remains in the ignored `.tools/aborted-live-migration-runtime` directory. All 20 original runtime files were accounted for after restoration. The replacement check uses native `netstat`, rejects listener-query failures, and was verified to block migration while both original listeners remained open. No verification-only server process remains. The renamed client and server require a restart for live verification.
