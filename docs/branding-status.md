# Lletya branding

The owner's confirmed spelling is **Lletya**, corrected on 28 October 2026. The current project is at `C:\Users\Jonathan\Desktop\Lletya`; all current project modules, custom Java classes/plugins, resource filenames, Gradle task names, environment variables, client/server text, preference paths, project links and launcher artwork use this spelling.

The GitHub repository and origin are [jonathanm35796/Lletya](https://github.com/jonathanm35796/Lletya). Server modules are `lletya-api`, `lletya-server`, `lletya-update-server`, `lletya-launcher` and `lletya-webhooks`. The server's data and private local configuration moved with the module directory. No old-name runtime directory mapping is needed by the startup scripts.

## Local state

Both game listeners were stopped before moving the runtime directories. Player files were retained in `main/lletya-server/server-data`; private server configuration remains excluded from Git. Superseded local build/data copies are retained under neutral directory names in the ignored `.tools/retired-module-artifacts` area.

Client settings are now `.lletya` in both the user-home and platform preference locations. Existing current settings and device preferences take precedence; missing settings from the older location were merged without overwriting existing files. Additional originals are preserved in the ignored `.tools/name-correction-user-settings` backup. The one-time legacy migration code has been removed from the application after completing this filesystem migration.

Windows can keep the now-empty original Desktop folder locked while an editor or tool process still has it as its working directory. The project files have moved to the correct folder. `scripts/cleanup-empty-workspace.ps1` safely retries removing an empty original directory after its handles are released; it never deletes files or recursively removes a directory. Open the new workspace path for further work. The owner will handle the original folder manually; the previously running cleanup helper was stopped at their request.

## Artwork

The wilderness login background now includes LLETYA at the top in the same silver/crimson lettering as the launcher. The armored statue, courtyard, notice board, purple portal and dark red sky remain the scene, without game-UI overlays or lava. The original title-free scene is preserved separately in `main/branding/login-background-scene.png`. The silver launcher wordmark now reads **LLETYA**, in the exact order L-L-E-T-Y-A. The L shield icon remains appropriate. The spelling correction used the built-in imagegen tool; originals and prompts are recorded in [the artwork documentation](../main/branding/README.md). `scripts/update-branding.ps1` regenerates native login halves and correctly sized app/launcher resources.

## Verification and history

Current verification is recorded in the README. Current source and filesystem names are checked case-insensitively for both superseded spellings. Git commit history, third-party author credits, upstream OSRS content and recovery snapshots remain historical records; current runtime branding uses Lletya.
