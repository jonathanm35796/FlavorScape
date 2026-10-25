# FlavorScape

Reviving FlavorScape using an established OSRS-style private-server source and a compatible standalone client.

The original requirements are preserved in [Start.txt](Start.txt). Source selection must precede major implementation, and the owner must approve the recommended foundation.

The owner has selected [Kronos-184-Fixed](https://github.com/Leif-Yggdrasil/Kronos-184-Fixed) for evaluation, including its bundled RuneLite/OpenOSRS client foundation. Baseline publication is pending verification of server redistribution permissions. See the [baseline review](docs/kronos-baseline-review.md) for evidence and next steps. FlavorScape client customization follows a working local client/server.

## First milestone

The working Kronos source is in `main/`; make future server/client changes there. The separate upstream reference clone remains in `research-sources/`. See [workspace layout](docs/workspace-layout.md). Source remains locally ignored pending redistribution permission.

Run a compatible client and server locally. Then improve PvP behavior and customize client settings incrementally while preserving working systems.

## Environment setup

For the installed local toolchain, use the [local startup instructions](docs/local-running.md) to build, start, stop, or verify a localhost login.

From ordinary Windows PowerShell in this directory, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-tools.ps1
```

This installs Git and GitHub CLI through Windows Package Manager. Reopen PowerShell afterward, then authenticate using `gh auth login` and select GitHub.com, HTTPS, and browser sign-in. Do not put tokens in project files.

Java and build-tool versions will be selected from the shortlisted repositories' actual requirements. Use their checked-in build wrappers rather than installing arbitrary global Gradle versions.

The workspace follows the existing GitHub `main` history. Kronos is cloned under ignored `research-sources/` for local evaluation; its source is not published because server redistribution terms remain unresolved. Never overwrite remote history.

See [environment status](docs/environment-status.md) for verified limitations and [source investigation](docs/source-investigation.md) for preliminary findings.
