# Source investigation — preliminary

## Selected foundation (2026-10-25)

The owner selected Kronos-184-Fixed for local evaluation and approved a baseline import/push conditional on licensing review. Bundled RuneLite/OpenOSRS is accepted as the technical client foundation. See [Kronos baseline review](kronos-baseline-review.md) for current evidence, unresolved redistribution permissions, and build inspection. The earlier candidate comparison below is historical; no broad rediscovery is needed.

These are discovery findings, not a completed source evaluation or adoption recommendation. No repository has been cloned or built locally.

| Candidate | Verified from published pages | Still to verify |
| --- | --- | --- |
| [RS Mod](https://github.com/rsmod/rsmod) | Repository describes an OSRS server emulator and displays an ISC license. | Build requirements, actual combat implementation, content coverage, standalone client, cache requirements, maintenance and security review. |
| [RSBox candidate fork](https://github.com/vanicosrs/rsbox) | Page identifies a fork of Rune-Status/rsbox-rsbox. README describes Java/Kotlin/Gradle and MIT licensing. | Exact license contents, fork age and provenance, dependencies, working client, gameplay completeness and successful build. README claims about the latest revision are not evidence of current compatibility. |
| [Tarnish](https://github.com/Jire/tarnish) | Repository contains game-client and game-server directories and a Gradle wrapper. [Resource listing](https://rsps.org/resources/tarnish) describes OS-Royale ancestry, revision 214 data, and reserved Tarnish branding. | Exact source license and redistribution permissions, dependency integrity, buildability, combat correctness, networking and client customization. Availability is not proof that all bundled assets may be redistributed. |

Checked 2026-10-25. Repository pages were available through web research. The owner's Llyeta repository could not be fetched; its visibility and authentication requirements remain unknown.

## Required evaluation before recommendation

- Expand discovery through original repositories and Rune-Server release threads; record provenance and exact commits.
- Inspect build scripts and dependency repositories before executing them. Record JDK and wrapper versions and complete build output.
- Establish matching server, standalone client, cache revision, and any unavailable external dependencies.
- Trace tick scheduling, packet processing, movement, combat timing, attack switching, special attacks, equipment changes, projectiles, freezes, death and item loss.
- Exercise PvP behavior locally; source structure or README feature lists cannot establish OSRS accuracy.
- Inventory working gameplay and distinguish implemented content from stubs.
- Inspect authentication, packet bounds, persistence, privileged commands, bundled credentials, and external service calls.
- Evaluate client settings, hotkeys, interfaces, performance controls, and packaging.
- Review source licenses separately from game assets, branding, and third-party redistribution rights.
- Compare verified outcomes and implementation effort, then request owner approval of the recommended foundation before major implementation.
