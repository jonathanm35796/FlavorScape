# Content audit and PvP readiness

Audited 2026-10-26 against the working source in `main/`. Free combat-level selection is implemented. The server has substantial PvP, skilling and PvM code, but several named activities are unfinished; it should not be advertised as having every OSRS activity fully working.

Shop update reverted 2026-10-27 at the owner's request. The supplemental skilling/activity stock and separate Skilling Market have been removed; the original market catalogue and two dropdown choices are restored. The content inventory and free combat-level controls remain.

## Scope and evidence

The inventory covers **1,420 server Java/Kotlin files**, grouped into **159 components**, and **2,411 JSON/YAML data files**. All data files parsed successfully. All **7,707 positive literal NPC/object spawn references** checked resolved to cached definitions. This checks references, including world-specific rows; it does not mean every spawn is active on the PvP world or accessible to players.

Every source file is indexed in the attached ledgers. Gameplay families were reviewed for registration, entry points, session logic, rewards and obvious incomplete implementations. This is a source/data audit, not a live completion of every skill, boss, minigame or economy transaction. Cache assets, a teleport, an interface, a class name or a successful build alone do not establish playable content. Empty lifecycle hooks and `return null` markers are review leads, not automatically defects.

Inventory artifacts:

- [Every source file](content-audit/source-files.tsv), including component, registration and review-marker counts.
- [Component totals](content-audit/components.tsv).
- [Every data file and parse result](content-audit/data-files.tsv).
- [Spawn references and cached-definition checks](content-audit/spawn-references.tsv).
- [The 37 registered placeholder spells](content-audit/placeholder-spells.tsv).
- [Inventory summary](content-audit/inventory-summary.txt).

## Free combat levels: delivered

Every account, including ironmen, can select its own Attack, Strength, Defence, Ranged, Prayer, Magic and Hitpoints levels without payment or a donor rank.

- **Left-click a combat skill** in the Skills tab to enter a level. Right-click offers its skill guide.
- **`::levels`** or **`::combatlevels`** opens the selector.
- **`::level attack 75`**, **`::level defence 1`** or **`::level hp 99`** sets a specific skill.
- Mac's existing reset-level action opens the same free selector.
- Skills can go up or down: **1–99**, except **Hitpoints 10–99**. XP is set to that level's threshold and persists through the normal character save.

Changes require being outside combat, PvP areas and active activities. The server checks again when the number is submitted: recent attacks/defence, death/locks, Wilderness, PvP combat zones, an active Bounty Hunter target, duels, tournament/LMS participation and queues, supported minigame/session fields, instances and instance heights block changes. Players must unequip gear whose requirements would be broken by lowering a level. Prayers, quick prayers, bonuses and displayed combat level are refreshed. Noncombat skills keep their normal progression.

Implementation: [shared server editor](../main/llyeta-server/src/main/java/io/ruin/model/stat/CombatLevelEditor.java), [Skills-tab routing](../main/llyeta-server/src/main/java/io/ruin/model/inter/handlers/TabStats.java), and [client skill actions](../main/runelite/runelite-client/src/main/java/net/runelite/standalone/CustomInterfaceEdits.java). Restart both client and server to activate these changes. No running account was edited and no live server was restarted during this audit.

## All 23 skills

“Implemented” below means substantive handlers and mechanics exist in source. Individual recipes, XP rates, unlocks, pathing and every interaction have not all been tested in-game.

| Skill | Implementation found | PvP assessment |
| --- | --- | --- |
| Attack | Melee accuracy, weapon requirements, attack styles and combat XP. | Freely selectable; equipment/combat regression coverage exists. |
| Strength | Melee damage, strength requirements, styles and XP. | Freely selectable; weapon-specific parity remains in the [combat audit](combat-audit.md). |
| Defence | Defensive rolls, armour requirements and XP. | Freely selectable; require gear removal before incompatible reductions. |
| Hitpoints | Health, damage, healing, death and XP. | Freely selectable from 10; changing levels is blocked during combat. |
| Ranged | Weapon/ammunition handling, accuracy/damage, styles and XP. | Freely selectable; specialised weapon interactions need continuing PvP testing. |
| Prayer | Bone offerings, standard prayers, draining, overheads, quick prayers, Redemption and Retribution. | Freely selectable; active/quick prayers are revalidated when levels change. |
| Magic | Standard/Ancient/Lunar spellbooks, combat spells, teleports and many utility spells. | Freely selectable, but **37 registered spells are placeholders**; see launch findings. |
| Agility | Gnome, Barbarian and Wilderness courses; Al Kharid, Ardougne, Canifis, Draynor, Falador, Rellekka, Seers and Varrock rooftops; shortcuts and marks. | Optional progression; course availability and lap rewards need live checks. |
| Construction | Persistent houses, rooms, hotspots/furniture, materials, servants, portals, altars, pools and storage interactions. | Large optional system; do not assume complete room/furniture parity. |
| Cooking | Food cooking, wine and guild interactions. | Optional supplies source; review food availability against shop prices. |
| Crafting | Gems/amethyst, jewellery, armour, battlestaves, glass, pottery, spinning and silver work. | Optional production; recipe/reward balance not certified. |
| Farming | Saved patches/crops, compost, tools; herbs, allotments, bushes, hops, trees, fruit/spirit trees and specialised patches. | Optional long-term progression; growth timing/relogin requires live testing. |
| Firemaking | Log burning; Wintertodt has separate activity logic. | Optional; review fire placement and activity rewards. |
| Fishing | Fishing tools/spots/catches, clue bottles, guild and aerial fishing. | Optional supplies source. |
| Fletching | Arrows, bolts/tips, darts, javelins, bows, crossbows and ballista production. | Optional; ammunition supply/cost balance matters for PvP. |
| Herblore | Herb cleaning, tar, potion production and grinding. | Optional; potion access and outputs require economy review. |
| Hunter | Birds, chinchompas, salamanders, traps, implings and Puro-Puro support. | Optional; trap lifecycle and catch rewards need live checks. |
| Mining | Rocks, pickaxes, essence and guild; Motherlode Mine support. | Optional resource progression. |
| Runecrafting | Altars, Abyss, pouches, Ourania, dense essence and related staff interactions. | Optional rune production; utility spell placeholders are separate. |
| Slayer | Masters, weighted assignment, requirements, blocked/unlocked tasks, task tracking and shops. | Optional PvM progression; reward rates and task completion need live checks. |
| Smithing | Smelting, bars, anvil products and forging-ring support. | Optional equipment production. |
| Thieving | Pickpocketing, stalls and wall safes. | Currency source; check reward rates against the Blood Money economy. |
| Woodcutting | Trees, axes and guild interactions. | Optional resource progression. |

## Minigames, activities and PvM

| Content family | Source assessment | Remaining verification or limitation |
| --- | --- | --- |
| Bounty Hunter | Assigned Edgeville targets, emblem tiers 1–10, target upgrades, private downgraded death drops and Blood Money redemption. | Tier payouts: 3M, 5M, 7.5M, 10M, 15M, 20M, 30M, 50M, 75M, **100M**. See [BH rules/tests](bounty-hunter.md); live multi-player anti-farming and disconnect scenarios remain important. |
| Wilderness PvP | PvP combat/death handling, skull/risk systems, kill streaks/ELO, hotspots, blacklists and risk protection. | Live cross-account damage, death-loot ownership, protection and combat-escape tests still required. |
| Wilderness content | Boss/event handlers, Revenant Caves, resource area, Chaos Altar, Mage Arena-related content, Larran/other keys, supply/bloody chests, volcano, obelisks, levers and webs. | Individual entrances, drops, restrictions and reward balance not universally exercised. |
| Last Man Standing | Lobby/queue, session modes, fog, crates/keys, supplies, elimination/winner and reward paths. | Substantial game logic; needs a real multi-player round. Rewards use Survival Tokens. |
| Tournaments | Manager, queues, maps, modes, presets, fights and playlists; enabled, entry fees disabled, scheduled interval three hours. | Manager NPC 7316 has an `ALL` spawn at Edgeville (3078,3495), in addition to an ECO-only spawn elsewhere. Full brackets/reconnect/rewards need live checks. |
| Duel Arena / staking | Offer/rule stages, combat and settlement code. | High-priority transaction, disconnect and item-ownership review before enabling staking. |
| Clan Wars | Free-for-all portals, map and combat rules exist. | Competitive/team Clan Wars is **partial**: `ClanWarsSettings` contains constants, not a complete team-match manager. |
| Barrows | Brothers, tunnels, chest and rewards. | Full run not completed during this audit. |
| Fight Caves | Wave progression, monsters, Jad and rewards. | Full run and death/re-entry need live checks. |
| Inferno | Wave and monster scripts, including Zuk. | Full encounter completion and reward safety unverified. |
| God Wars Dungeon | Four generals, minions, access/kill count and altars. | Individual mechanics and instance access require live checks. |
| Chambers of Xeric | Rooms, bosses, party/session and reward code. | Substantial implementation; full raid completion/party edge cases unverified. |
| Theatre of Blood | Dungeon scaffolding plus some room/boss code. | **Unfinished** rooms and spectator/death handling; do not advertise a complete raid. |
| Other bosses | Scripts for Zulrah, Vorkath, Hydra, Cerberus, Abyssal Sire, Kraken, Thermonuclear Smoke Devil, Kalphite Queen, Corporeal Beast, Dagannoth Kings, Giant Mole, Skotizo and other encounters. | Presence of scripts does not certify every phase, drop or OSRS mechanic; see combat audit for equipment coverage. |
| General PvM | Broad monster AI/drop support including dragons, revenants, Slayer monsters, demonic gorillas and shamans. | Drops, safespots and farming rates need per-area testing. |
| PvM instances | Twelve boss-instance types: Mole, DKs, Kraken, KBD, Smoke Devil, Cerberus, Corp, KQ and the four GWD generals; portals, fees/rank rules. | Session cleanup, exits, fees and reward ownership need testing. ECO kill-count integration has TODOs. |
| Pest Control | Boats/lobbies, game/portal/monster logic, points, rewards and XP exchanges. | Full group round and point/reward abuse checks pending. |
| Wintertodt | Cold, braziers, pyromancers, points and reward crates. | Some damage/reset events remain TODOs; verify full rounds before promotion. |
| Motherlode Mine | Pay-dirt and washing/reward flow. | Live capacity/relogin/collection checks pending. |
| Warriors' Guild | Animated armour, tokens, cyclopes and defenders. | Entry/token consumption and defender progression need live checks. |
| Puro-Puro | Area/impling support linked to Hunter. | Catch/escape/pathing checks pending. |
| Clue scrolls | Tier/reward support with anagram, cryptic and map steps. | Selected steps implemented; complete OSRS clue coverage not established. |
| Nightmare Zone | Preparation overlay and reward-chest interface. | **Partial**: no complete dream/boss-session system found in this component. |
| Boss Rush | Timer, points/pool fields and basic start state. | **Partial**: no complete wave/spawn/reward loop found. |
| Pyramid Plunder | Session skeleton. | **Stub**: empty `start(Player)` and empty object-registration block. |
| Party Room | Balloon/local-drop helper. | **Partial**: no complete deposit/chest/party loop found; unused count argument and debug message remain. |
| Seasonal/scheduled events | Summer Shrine enabled, tokens/rewards, timed event manager and event-boss logic. | Review activation schedules and currency/reward rates. |
| Daily tasks / loyalty | Daily NPC/PvP tasks, loyalty chest and donor-zone handlers. | Eligibility, reset times and rewards need live checks. |
| Appreciation / legacy viewing | Caretaker thank-you interaction, old tournament viewing orb. | Small interactions; not complete independent minigames. |
| Jail | Jail/control support. | Administrative feature, not player progression. |

## Other in-game systems

| System | What exists / audit boundary |
| --- | --- |
| PvP market and shops | Custom Edgeville catalogue, search/categories, native buy menu, Blood Money/platinum purchasing and restricted 40% buyback; general shops and custom item overlays. Targeted market tests pass; global shop arbitrage has not been exhaustively checked. |
| Gear presets | Twelve editable slots for every account, six initial defaults, saved inventory/equipment/levels/spellbook/prayers, F1–F12 selection while open. Preset tests pass; ordinary inventory ownership/spawn rules still apply. |
| Banking / inventory / trading | Containers, bank functions, direct trading, item requirements, charges, combinations, storage and currency normalization. A complete duplication/rollback audit is separate from this inventory. |
| Trading Post | Offers, browsing, coffer and item transactions, exposed through booth/area handlers; ironmen blocked. Coffer withdrawal captures the balance before the amount prompt rather than rereading it at submission; review delayed-callback and transaction integrity. This is a review finding, not a demonstrated duplication exploit. |
| Rewards and sinks | Blood Money, platinum, activity tokens, mystery boxes, chests, pets, voting/donation rewards, gambling/dicing-related code. Full issuance/sink balance remains unverified; legacy conversions below need attention. |
| Achievements / account summary | Achievement tiers, bestiary/stats, kills/deaths, Slayer, votes/spending and summary navigation. Collection-log totals are not tracked and the UI says so. No full quest catalogue/completion system was established by this audit. |
| Travel and access | Home, PvP/ECO teleport data, NPC/object transports, wilderness levers/obelisks, private-instance portals. Data presence is not proof of every destination's live reachability. |
| Account / social / settings | Account modes, permissions, commands, chat/clans/friends, interfaces, settings, emotes and music. Numerous music TODO markers concern region/unlock metadata; they do not establish that all music playback is broken. |
| External integrations | Login/account persistence plus optional database, vote, donation, webhook and web-service integration code. Service configuration and delivery have not been validated or invoked during this audit. |

## Findings to resolve for a PvP launch

1. **Finish or hide incomplete PvP support spells.** The 37 `TodoSpell` registrations send a TODO message rather than casting. This includes **Vengeance Other, Spellbook Swap, Heal Other and Energy Transfer**, alongside many Lunar teleports/utility spells and Telekinetic Grab. Main Vengeance is implemented. The complete list is in the [spell ledger](content-audit/placeholder-spells.tsv). Source: `model/skills/magic/spells/TodoSpell.java` and spellbook registrations.
2. **Remove or repair legacy Emblem Trader currency conversion.** `model/entity/npc/actions/edgeville/EmblemTrader.java` registers Blood Money-on-NPC-315 twice. The later registration replaces the earlier one. Its old conversion requires at least 25 million, calculates `amount / 25000 / 1000`, removes the offered stack and grants item **13215**, rather than the market's platinum token. The earlier conversion also contains inconsistent arithmetic. These are separate from the newly repaired BH emblem redemption and conflict with the market's **1 platinum : 1,000 Blood Money** rule. Left unchanged in this audit; repair/disable before launch.
3. **Keep unfinished activities out of the launch offering.** ToB's Sotetseg, Xarpus, Verzik and Vasilias rooms have empty object registration and null spectator spots. The death handler uses that spectator list, creating a null-dereference risk when invoked. Bloat's barrier entry callbacks are empty. Nightmare Zone, Boss Rush, Pyramid Plunder, Party Room and competitive Clan Wars also lack complete gameplay loops. ToB's Mysterious Stranger shop choices remain TODOs.
4. **Review transactions and economy issuance next.** Prioritize staking, Trading Post, direct trades, buyback/shops, legacy currency exchange, death drops and high-output PvM/chests. Parsing all data and passing unit tests does not prove a balanced or duplication-free economy.
5. **Use production account permissions before public access.** Current DEV login grants elevated Owner/Admin permissions and bypasses normal password checks. Local operation is loopback-oriented; see [account documentation](player-accounts.md). Confirm deployment mode and ordinary-player login behavior before opening the server.
6. **Complete live multi-player PvP verification.** Test pure/zerker/max builds, level changes at dialogue submission, BH pairing/skips/relog/death, loot ownership, prayer/protection, food/potions, freeze/teleblock/escape, disconnects, presets and tournament/LMS boundaries. Existing tests cover subsets, not an entire live economy or competition.

For the intended PvP server, the strongest launch scope is Edgeville/Bounty Hunter, the PvP market, free build selection and editable presets, then validated LMS/tournaments. Skilling and tested PvM can remain optional progression. Avoid making unfinished raids or skilling grinds prerequisites for ordinary PvP supplies and build selection.

## Verification and reproduction

- Full server suite: **122 tests passed**, no failures/errors (`.tools/content-audit-server-tests.log`).
- Follow-up combat-editor/command checks: **18 tests passed**, including all seven skills, ironmen, level boundaries, delayed submission, combat/PvP/activity restrictions, equipment requirements, prayers/combat recalculation, XP persistence and skill-tab routing (`.tools/combat-level-controls-tests.log`). This follow-up includes one newly added test after the full-suite run; the counts overlap.
- Client market/gear/skill-action checks: **32 tests passed**, no failures/errors (`.tools/content-audit-client-tests.log`).
- Inventory: zero JSON/YAML parse errors and zero unresolved cache definitions among the spawn references checked (`.tools/content-inventory.log`).
- No full live playthrough or multi-client combat session was performed for this request.

The inventory generator is `main/llyeta-server/src/test/java/io/ruin/tools/ContentInventoryAudit.java`. It runs with the server test runtime classpath, the server module as working directory, and JVM option `--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED`. The checked-in [Gradle init script](../scripts/content-inventory.init.gradle) adds the task without changing production startup:

```powershell
# From the workspace root, with the project's JDK 21 and Gradle environment set:
Set-Location main
.\gradlew.bat -I ..\scripts\content-inventory.init.gradle :llyeta-server:contentInventoryAudit --no-daemon --console=plain
```

This regenerates the ledgers from current source/data. It does not change player saves or start a game server.
