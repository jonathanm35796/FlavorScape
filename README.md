# FlavorScape

Reviving FlavorScape using an established OSRS-style private-server source and a compatible standalone client.

[Join the FlavorScape Discord](https://discord.gg/QJDZX7U4y).

The original requirements are preserved in [Start.txt](Start.txt). Source selection must precede major implementation, and the owner must approve the recommended foundation.

The owner selected [Kronos-184-Fixed](https://github.com/Leif-Yggdrasil/Kronos-184-Fixed), including its bundled RuneLite/OpenOSRS client foundation. The current source is included in the owner's private FlavorScape repository. Cache assets, runtime credentials, player saves and build output stay local. See the [baseline review](docs/kronos-baseline-review.md) for upstream provenance and historical findings.

## First milestone

The working source is in `main/`; make future server/client changes there. The separate upstream reference clone remains in ignored `research-sources/`. See [workspace layout](docs/workspace-layout.md).

The working application now uses FlavorScape branding and flavorscape-* module names. See [branding status](docs/branding-status.md) for artwork, migration, and preserved upstream references.

The in-game `::commands` catalogue is generated from active server handlers and filtered by the same permissions used for execution. See [command help](docs/commands.md) for paging, search, and maintenance.

Run a compatible client and server locally. Then improve PvP behavior and customize client settings incrementally while preserving working systems.

The [roadmap](docs/roadmap.md) records the implemented gear interface and custom presets, Blood Money migration, logout-timer removal, and future requests.

The [combat audit](docs/combat-audit.md) records OSRS Wiki equipment comparisons, combat corrections, regression checks, and remaining parity gaps.

The [content audit](docs/content-audit.md) inventories all server source/data, reviews all 23 skills and activity families, identifies unfinished content and PvP launch priorities, and documents free combat-level selection for every account.

## Environment setup

For the installed local toolchain, use the [local startup instructions](docs/local-running.md) to build, start, stop, or verify a localhost login.

From ordinary Windows PowerShell in this directory, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-tools.ps1
```

This installs Git and GitHub CLI through Windows Package Manager. Reopen PowerShell afterward, then authenticate using `gh auth login` and select GitHub.com, HTTPS, and browser sign-in. Do not put tokens in project files.

Java and build-tool versions will be selected from the shortlisted repositories' actual requirements. Use their checked-in build wrappers rather than installing arbitrary global Gradle versions.

The workspace follows the existing private GitHub `main` history. Keep cache assets, secrets, player data, local toolchains and generated output out of commits. Upstream notices and client licenses remain with the source. Never overwrite remote history.

See [environment status](docs/environment-status.md) for verified limitations and [source investigation](docs/source-investigation.md) for preliminary findings.
