# Working source layout

`main/` is the FlavorScape development directory. It is a separate copy of the selected Kronos source, not a new implementation.

- Upstream: https://github.com/Leif-Yggdrasil/Kronos-184-Fixed
- Base commit: `66ee03dbbae53ccc652f6e49e9aa4191034b3c8e`
- Copied project: upstream `Kronos-master/`
- Reference clone: `research-sources/Kronos-184-Fixed/`

Make future game and client changes in `main/`. Keep the reference clone for upstream comparison. The working copy has no nested Git repository; the FlavorScape repository remains at the workspace root.

Working modules now use flavorscape-* directory and Gradle project names. This is the selected Kronos foundation with FlavorScape branding; the original source identity and credits remain documented. See [branding status](branding-status.md).

The copy includes the earlier local loopback binding fix, pinned Gradle distribution checksums, and desktop browser compatibility bridge. Build output, Gradle/Kotlin caches, runtime logs, and generated server player data were excluded. Fresh loopback server.properties files were generated from upstream examples. Bundled cache/assets are available locally for evaluation; their redistribution rights remain unverified.

`main/` is temporarily ignored by the root Git repository because server redistribution permission remains unresolved. This also means edits there are not backed up to GitHub. Existing license notices remain in the working copy. See kronos-baseline-review.md for verification results and outstanding issues.

Launches now use `main/` for the server/update server and `main/runelite/` for the client. Earlier research-checkout processes were stopped, and a real localhost client login has passed from main. Use the scripts in [local startup instructions](local-running.md).
