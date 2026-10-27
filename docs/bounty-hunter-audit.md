# Bounty Hunter implementation audit

This is the **pre-implementation baseline**. The defects below have subsequently been addressed by the [Edgeville Bounty Hunter implementation](bounty-hunter.md), including the owner's approved tier-10 payout of 100,000 Blood Money. Read that document for current behavior and verification.

Audited 26 October 2026 in the local `main/` working source. This audit changes no gameplay or account data and does not restart the running server.

**Verdict: implemented as a custom legacy Wilderness system, but not fully working or ready to certify.** The current file-login identity bug blocks natural target assignment. Nine checks identify defects or missing safeguards. This is not the modern Daimon's Crater activity.

The historical model is documented in Jagex's [18 September 2014 Bounty Hunter announcement](https://secure.runescape.com/m=news/bounty-hunter?oldschool=1). Modern Bounty Hunter instead uses a crater, lobby and its own reward systems, described in the [24 May 2023 relaunch](https://secure.runescape.com/m=news/bounty-hunter-is-back?oldschool=1). These references establish the two versions; this audit does not claim full parity with either version or every later OSRS update.

## What exists and how it behaves

| Component | Local implementation and verification |
| --- | --- |
| Entry and participation | Wilderness map listeners invoke Bounty Hunter processing. Accounts default to All Wilderness. Settings also offer Disabled and Edgeville Only. Edgeville targeting bounds are `(2993,3523)` to `(3124,3597)`, any height. Targeting changes are normally initiated outside the Wilderness. |
| Automatic matchmaking | A world event searches the Wilderness player list every 20 ticks, approximately 12 seconds. Candidates must be alive, without a target, within five combat levels, on a different account and IP, compatible with both targeting modes, without a skip/cooldown penalty, and outside the candidate's ten-minute recent-opponent history. These individual production checks pass with simulated unique account identities. Actual file-login IDs currently prevent natural matching; see finding 1. |
| Target assignment | Reciprocal opponent references, hint icons and assignment messages exist. Pairing and removal pass isolated runtime checks. Targets receive a special attack eligibility path allowing a combat difference of up to five levels, even where ordinary Wilderness-level difference would be smaller. Other general combat checks still apply. |
| Target interface | Native interface 90 has the expected widget slots. The server supplies the opponent's name, skull indicator, combat level, approximate Wilderness range, distance colour, wealth band and highest carried emblem tier. Skip, streak and overlay settings are registered. Rendering and all mouse interactions have not been exercised in a two-client encounter during this audit. |
| Risk bands | Inventory and equipment values are counted; looting-bag and rune-pouch contents are included when their container item is carried. Bands change at 100,000, 500,000, 1,100,000 and 2,500,000 protection-value units. These are carried wealth, not a complete items-lost-on-death risk calculation. Large stacks overflow before the total is widened; see finding 4. |
| Skips | Manual skipping requires roughly ten seconds out of defending combat. It removes both players' targets. Ten skips incur a 30-minute restriction; skip tracking has a 30-minute reset window. Warning/confirmation dialogues exist. Penalty duration and expiry pass. |
| Leaving and logging out | A paired player has 200 ticks, approximately two minutes, to return from a safe area, with a one-minute warning. Remaining in the Wilderness for 60 ticks clears the return countdown. Normal timeout and logout remove the reciprocal target in isolated checks. Cleanup during defending combat still calls the combat-blocked skip path and needs live edge-case testing. |
| Target kills | The death handler invokes Bounty Hunter rewards before ordinary kill rewards and item loss. A target kill upgrades the killer's highest carried emblem, or spawns tier 1 if no upgrade is possible. Tier 1 jumps from ID 12746 to 12748; higher tiers increment through 12756. Victim cooldown is six minutes; killer cooldown is one minute. Pair cleanup, upgrade, counter increment and cooldown values pass. |
| Non-target kills | Wilderness rogue kills increment a separate counter and upgrade an existing carried emblem. This is a custom deviation from the basic historical target-only upgrade description. It also lacks the normal kill-farming checks; see finding 2. |
| Blood Money kill rewards | General PvP rewards start from 50 Blood Money and have additional spree, Wilderness, event and account modifiers. A recognised Bounty Hunter target adds a 5% multiplier at its stage of that calculation. General kill rewards reject same-IP kills and apply a five-minute repeated-victim check outside the configured Edgeville exception. Those checks run after Bounty Hunter has already handled emblem rewards. |
| Emblem death behavior | Emblems are marked unprotectable by the items-kept-on-death setup. Lost tier 1/2 becomes tier 1 for the killer; lost tier 3–10 downgrades one tier. Lost emblems are spawned privately for the killer. No killer means these lost emblems do not produce that reward. Protection eligibility is checked at runtime; complete ground-drop and pickup sequences remain live-test work. |
| Emblem redemption | All ten Info/Redeem actions register successfully. Redemption directly pays Blood Money, not Bounty Hunter points. Every configured tier value passes a controlled confirmation test. Ownership/location revalidation and payment-overflow protection fail; see findings 3 and 5. |
| Bounty teleport | The Standard spellbook registers an 85 Magic spell costing one law, one death and one chaos rune. A scroll unlocks it persistently. Missing unlock, missing opponent, nearby opponent and safe-area opponent are rejected. Movement uses the normal teleport gates, including teleblock and the default level-20 Wilderness departure cap, but the spell ignores a rejected movement result; see finding 6. Successful delayed arrival, target changes in flight, clipping and rune use require live testing. |
| Scroll availability | Scroll 12846 appears in Voting Rewards for 100 vote points and Wilderness Points Exchange for 10,000 wilderness points. Its Read action and permanent unlock are present. This audit did not perform a live shop purchase. |
| Reward shops and trader | Emblem Trader NPC 315 is spawned at `(3089,3503,0)` and has skull/KDR/currency dialogue handlers. Its separate `SHOP_UUID` is empty, so it does not establish a complete dedicated Bounty Hunter reward shop. Current mysterious emblems redeem from inventory; the help text still mentions Wilderness Points/Sigmund/Rustin inconsistently. The new PvP Market is a separate Blood Money shop, not proof of a Bounty Hunter points/reward-shop implementation. |
| Persistence | Targeting preference, recent-opponent history, skips and penalties are exposed for saving. Live target references and return timers are not persisted. A Gson round trip verifies targeting, penalty and history. Kill counters, overlay setting and teleport unlock use saved configs/attributes. |
| Ancient Warriors' equipment | Twenty supported base/variant IDs have the configured activation rules: Wilderness levels 1–5 with targeting enabled for the current area; no assigned opponent is required. Outside that eligibility, equipment stays worn but loses its relevant bonuses and weapons cannot attack/cast. The max-hit dummy has a deliberate exception. Eight equipment regressions and ten dummy interaction tests pass. This is a custom restriction and is separate from matchmaking's broader Wilderness participation. |

Emblem payout schedule, in Blood Money:

| Tier | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Value | 150 | 250 | 375 | 500 | 750 | 1,000 | 1,500 | 2,500 | 3,750 | 5,000 |

## Confirmed findings

| Priority | Finding | Evidence and effect |
| --- | --- | --- |
| High | 1. File-login IDs block natural target assignment | `PlayerLogin.send()` uses `PlayerFile.loadPlayer()`. That path calls the name-only `LoginInfo.update(...)`, which does not assign `userId`; it stays at -1. `Player.init()` copies it. Matchmaking rejects equal IDs as the same account. Two distinct synthetic file-login updates both remained -1. This affects the current local login path, independently of the separate same-IP restriction. |
| High | 2. Emblem rewards bypass kill-farming protection | `BountyHunter.deathByTarget()` upgrades emblems on non-target Wilderness kills before `Killer.reward()` applies its IP/repeat checks. A same-IP simulated rogue kill upgraded a tier-1 emblem to tier 2. This proves the reward-method gap; it is not a demonstration of a live exploit or a full PvP encounter. |
| High | 3. Redemption confirmation trusts stale ownership | The redeem callback calls `item.remove()` and awards currency without checking that the referenced emblem still belongs to that player. Removing the emblem between opening and confirming still awarded 150 Blood Money. The production callback was invoked directly; normal dialogue-closing behavior may prevent some live paths, which have not been tested. |
| Medium | 4. Target wealth overflows | `countWealth()` multiplies two integers before adding to a long. A controlled 3,000,000 × 1,000 stack produced -1,294,967,296 rather than 3,000,000,000, which can select the wrong risk colour. |
| Medium | 5. Redemption lacks location and payout-capacity revalidation | Opening redemption safely and then confirming with Wilderness level 5 still paid. With a maximum-sized Blood Money stack, confirmation consumed the emblem without a full payout. These are two failed audit checks within the redemption transaction path. |
| Medium | 6. Failed teleports report success | `BountyTeleport.cast()` returns true regardless of `startTeleport()`'s boolean result. A rejected movement request still reported a successful spell; the normal spell wrapper consequently removes runes/grants XP. Actual teleblocked/level-cap client scenarios were not executed. |
| Medium | 7. Streak counters do not reset on death | A victim with four target kills retained four after being killed by its target. The code increments target/rogue counts and updates records but does not reset the corresponding count on death. They behave as cumulative counters despite the Streaks setting. |
| Low | 8. Overlay enable message is incorrect | Both branches of `BountyOverlay.select()` say the overlay will no longer be shown. Turning it on reproduces the wrong message. The setting is also not directly consulted by `BountyHunter.checkActive()`'s targeting-based visibility toggles, so re-entry/targeting transitions need visual testing. |

The table groups nine failing checks into eight findings because location revalidation and payout overflow share finding 5. No production fixes were applied as part of this verification request.

## Modern features not implemented by this system

The source and data searches did not find an implemented Daimon's Crater/lobby, its entry/coffer/loot-chest flow, a modern Bounty Hunter point economy, Earning Potential/Bounty Crates, or the modern reward/imbue progression. General Wilderness combat, the PvP equipment store and unrelated staff-bounty events do not supply those systems. Their absence is a version/scope difference rather than proof that the historical design was intended to contain them.

## Verification evidence and limits

- Baseline standalone production-method audit: **35 checks, 26 pass, 9 fail, 0 errors**. Original evidence remains in `.tools/bounty-hunter-verification.log`. The current `.tools/bounty-hunter-audit.tsv` is regenerated against the implementation.
- Existing JUnit checks: **18 pass, 0 fail/error**: eight `BountyHunterEquipmentTest` and ten `MaxHitDummyInteractionTest` cases.
- Gradle build succeeds. Its success does **not** mean the audit is clean: the standalone audit records failures in its ledger and exits normally so the separate JUnit checks can run.
- Full execution log: `.tools/bounty-hunter-verification.log`.
- Probe source: `main/flavorscape-server/src/test/java/io/ruin/tools/BountyHunterAudit.java`.
- The audit uses isolated simulated players and cached definitions. It suppresses background world event processing while invoking individual production methods. It does not certify automatic matchmaking's live event, actual packet/render timing, complete combat deaths, ground-item ownership/pickup, simultaneous deaths, reconnects, or successful target teleport paths.
- No real players were killed, no accounts were edited, and the server/client were not restarted. The running binary was not substituted with the current build.

Reproduction from `main/`, with the installed JDK 21 and workspace Gradle home:

```powershell
.\gradlew.bat :flavorscape-server:bountyHunterAudit :flavorscape-server:test `
  --tests 'io.ruin.model.combat.BountyHunterEquipmentTest' `
  --tests 'io.ruin.model.combat.MaxHitDummyInteractionTest' `
  --init-script ../.tools/bounty-hunter-audit.init.gradle --console=plain --no-daemon
```

The local init script registers the audit JavaExec task with test runtime dependencies. The TSV ledger must be checked separately from Gradle's exit status.

Recommended repair order: distinct persistent account identities; centralised kill-reward eligibility; atomic and revalidated redemption; teleport result propagation; 64-bit wealth calculations; streak and overlay corrections. Then run a two-client matrix covering assignment, skips, return/logout timers, target/rogue deaths, emblem downgrade/redemption and successful/blocked teleports before claiming end-to-end readiness.
