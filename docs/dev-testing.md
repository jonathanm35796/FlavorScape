# DEV account testing

The exceptions apply only when `World.stage` is **DEV**, and only to **Jon**, **Jon2** and **Sales Tax**. Names are case-insensitive; `Sales_Tax` is also accepted. Staff rank alone does not enable them. **LIVE and BETA keep normal restrictions, including for these names.**

## Same-computer testing

- Named accounts can log in even when other local accounts are already in the Wilderness. They do not consume ordinary Wilderness IP slots in DEV.
- Two distinct named DEV accounts can receive each other as Bounty Hunter targets on the same IP and repeat matches. Recent-opponent and target-penalty checks do not block these tests. The five-minute reward cooldown still applies: repeated kills of the same account give no Blood Money, BH points, emblem upgrades or other generated rewards until it expires.
- Both participants must be named test accounts for pair-based exceptions. Pairing with an ordinary account does not bypass same-IP/repeat-reward checks.
- Distinct valid identities, the combat bracket, active area, alive state and target availability still apply. Exceptions do not allow self-targeting, bypass authentication/bans or disable normal trade inventory safeguards.
- Named DEV pairs can trade while testing ironman modes. Ordinary ironman trading restrictions remain elsewhere.

The policy is [DevTesting.java](../main/flavorscape-server/src/main/java/io/ruin/model/entity/player/DevTesting.java), shared by login, Wilderness IP accounting, Bounty Hunter, kill rewards and ironman trade guards. No player save was edited.

## Trading

Direct trading has **no general same-IP/device prohibition**. Tests exercise the real request flow, both native confirmation screens, transfers, decline/refund behavior and stale/repeated clicks between same-IP players. A stale request with a missing timer is now guarded.

Stand near each other in a safe area, use **Trade with**, accept the request, offer items and accept on **both screens**. For an initial test, use Blood Money or ordinary items without custom attributes. Capacity, locks, jail, duels, tournament signups, item attributes and supply-chest restrictions retain their normal behavior.

## Bounty Hunter

1. Outside the Wilderness, give both accounts combat levels within **five levels** of each other. Loading the same default max preset is a convenient starting point.
2. Enable targeting and the Bounty Hunter overlay in the account toggles.
3. Walk north of Edgeville's ditch into Wilderness levels **1–5** with both accounts. Default targeting is Edgeville Only.
4. Allow approximately **12 seconds** for matching. With three accounts, two form a pair and the third waits for another eligible player.
5. Both matched players receive a hint arrow and the upper-right panel with the target's name, combat/Wilderness information and highest carried emblem tier/icon. With no carried emblem, the native empty state is shown. Walk into view to see the pointer over the target's head.
6. Kill the assigned target and pick up the private tier-1 drop. Carry it through eligible target kills to upgrade it through tier 10. Wait five minutes between rewarded kills of the same account, or use another eligible opponent. Named DEV pairs can still repeat matches; normal death/respawn and active-target requirements remain.

The old server sent names/location to widgets **90:47/48**, which are a rectangle and sprite in this cache. It now uses text widgets **90:53/54**. Native script 12 owns the emblem/wealth fields, so those are left to the script; config 4162 supplies tiers 0–10. The client now unhides target container **90:18** as well as its parent, and assignment republishes visibility. Wilderness labels now correctly show 1–5.

## Reference behavior

Jagex's [2014 announcement](https://secure.runescape.com/m=news/bounty-hunter?oldschool=1) describes Wilderness matching near the player's combat level, a top-right target interface, location hints and ten emblem tiers. Its [2023 version](https://secure.runescape.com/m=news/bounty-hunter-is-back?oldschool=1) uses Daimon's Crater and a default five-level range: a different ruleset from the classic Wilderness version requested here.

Impact's [command guide](https://impactmmo.wiki/w/index.php?stable=1&title=Commands) documents Edgeville BH areas; its [home guide](https://impactmmo.wiki/w/index.php?stable=1&title=Home_Area) describes emblem exchange. These support the general model but do not establish every Impact UI/eligibility detail. FlavorScape keeps the owner's Edgeville rules and approved Blood Money payouts, including **100,000 for tier 10**.

## Verification

**138 server tests and 33 client tests pass.** Checks cover DEV/production boundaries, repeated same-IP emblem progression, normal same-IP rejection, level/area/self-account guards, target names/hints/visibility, all ten emblem tiers, actual cache text types and script-owned fields, both trade screens, refunds and repeat-click safety. The client test invokes the real visibility hook on native containers.

The cache-based home check confirms **97 active NPCs, 61 dynamic objects and zero failures**, including no shop table and a direct merchant approach. Logs: `.tools/dev-trade-bounty-tests.log` and `.tools/dev-bounty-client-tests.log`.

Restart **both server and client**. Full live multiplayer combat and final rendered UI inspection remain separate from these automated checks.
