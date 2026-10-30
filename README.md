# Lletya

**An Edgeville PvP server built around freely chosen combat builds, fast gearing and Bounty Hunter.**

Lletya is an OSRS-style private server with a customized RuneLite/OpenOSRS desktop client. Our current focus is the PvP experience: choose your levels, load a setup, find a fight and earn Blood Money. The project is in active development; the feature inventory below distinguishes implemented systems from inherited content that still needs full gameplay verification.

[Join the Lletya Discord](https://discord.gg/j5YGZpNxR) · [Development roadmap](docs/roadmap.md) · [Full content audit](docs/content-audit.md)

## Recent work: 25–28 October 2026

The current development batch brings together the following changes. The feature sections below describe their final behavior, including the corrected split between the cash shop and Bounty Hunter points shop.

| Area | Delivered changes |
| --- | --- |
| Lletya rename (28 October) | Renamed modules, custom plugins, client/server text, preferences and launcher resources; added original wilderness loading artwork, an L shield icon and LLETYA wordmark, also integrated into the login background. Moved source modules and runtime state into the corrected Desktop workspace and consolidated client settings into `.lletya`; no old-name startup mappings remain. |
| Branding and local setup | Lletya names, login artwork and community links; portable JDK/Gradle launch scripts, localhost startup/login checks, readable logs and optional Discord configuration. |
| Combat builds and gearing | Free editing of seven combat skills, twelve editable presets seeded with six builds, saved prayers/spellbooks, keyboard shortcuts, native close/camera controls and bank-capacity checks that preserve attributed items and rune-pouch contents. |
| Combat and spellbooks | Maximum-hit dummy corrections, including Vesta's special and dragon claws; equipment/combat regressions; restored classic Standard, Ancient and Lunar spellbook layouts. |
| Edgeville and market interface | Restored bank/services, workshop pool and merchant placement, removal of the blocking table, category/search/scroll controls, native Buy 1/5/10/X menus, token payments and immediate closing. |
| Shop stock and prices | Replaced inherited main-shop stock with 178 ordinary entries from the new list. Kept a separate 17-item Vesta/Statius/Morrigan/Zuriel points shop, with the requested 20–500-point prices, free general entries and separate buyback rules. |
| Bounty Hunter | Stable account identities, Edgeville level 1–5 matching, reciprocal targets/hint arrows, tier-1–10 emblems, trader redemption, target-panel name/emblem fixes and visibility that survives native Wilderness scripts and interface reloads. |
| Kill rewards | 10M Blood Money base per eligible PvP kill, fractional BH points and target bonuses, plus one persistent five-minute repeat-victim gate for all generated rewards. Edgeville and named DEV testers obey the reward timer; blocked kills report the time remaining. |
| Loot keys | Skully at `(3099,3510,0)`, chest at `(3095,3506,0)`, saved settings and Value statistics, five-key carrying limit, preserved loot/attributes and a reference-style window with inventory/bank withdrawal, Item/Note, key tabs, scrolling, close and confirmed destruction. |
| Account and transaction fixes | Named DEV multi-account/same-IP testing, guarded ironman test trades, both trade confirmation screens, stale-request checks, safe emblem redemption and rejection of loot-key banking or preset cloning. |
| Reliability and review | Inactivity/six-hour logout requests removed, disconnect and metadata error handling, content/equipment audits and automated verification of the revised home layout. |

Both the server and client must be restarted to load the latest definitions and loot-key UI protocol. The latest loot-key window has automated transaction/input checks and an inspected offline render; live in-game verification is still pending. Earlier localhost checks of login, presets, spellbooks and market interactions are documented in the linked reports.

## Where the game is heading

- **PvP comes first.** Edgeville fights, assigned Bounty Hunter targets, reliable combat and useful PvP rewards are the core of the server.
- **Players control their builds.** Pure, hybrid, tribrid and max builds can be selected without grinding combat levels or buying a donor rank.
- **Getting back into a fight should be quick.** Editable presets, keyboard shortcuts, the home market and nearby recovery facilities support that loop.
- **Blood Money connects combat and the economy.** Bounty emblems, rewards, purchases and buyback use a consistent cash currency, with separate activity tokens where appropriate.
- **Combat correctness and transaction integrity guide development.** Special attacks, equipment effects, death drops, reward eligibility and purchases are covered by focused regression checks and continuing live testing.
- **Skilling is a later priority.** Existing skilling code remains, but the recent supplemental shop-stock expansion and separate Skilling Market have been reverted. Broader skilling work is deferred while we focus on PvP.

PvM and other activities provide additional things to do as their mechanics and rewards are verified. New content should support the server's direction without making unfinished systems prerequisites for ordinary PvP.

## PvP features

### Free combat-level selection

Every account, including ironmen, can raise or lower **Attack, Strength, Defence, Ranged, Prayer, Magic and Hitpoints** for free.

- Left-click a combat skill in the Skills tab to enter a level; its guide remains available through right-click.
- Use `::levels` / `::combatlevels`, or set a skill directly with commands such as `::level attack 75` and `::level hp 99`.
- Levels range from **1–99**, with **Hitpoints 10–99**. Selected levels and XP persist in character saves.
- The server blocks changes during combat, in PvP areas, with an active Bounty Hunter target and during supported duels, minigames and instances. Gear must be unequipped before lowering below its requirements.

### Twelve editable gear presets

Every account receives **12 editable slots**, starting with six defaults and six empty slots. Defaults include max hybrid, max tribrid, 52-Prayer pure, pure hybrid, max melee and max ranged setups. Default slots can be overwritten just like custom slots.

Presets save inventory, equipment, combat levels, spellbook, active prayers and quick-prayer selections. Players can save, rename, delete and swap setups; overwriting a populated slot asks for confirmation. Loading respects existing item ownership/spawning, account, combat, location and bank-capacity rules.

| Control | Action |
| --- | --- |
| Quest tab's dragon med helm | Open the gear interface |
| `Ctrl+G` on the Quest tab | Toggle the gear interface |
| `F1`–`F12` while the preset panel is visible | Load the matching slot |
| `Ctrl+Q` | Reload the latest setup |
| `::gears` | Open gear controls |
| `::gear 1` / `::preset 1` | Load a slot; slots 1–12 are supported |
| `::storegear 1` | Save the current setup to a slot |
| `::savegear 1` | Manage a slot |

A successful load closes the panel; a rejected load leaves it open. The panel supports normal close controls and the client's Escape setting.

### Edgeville Bounty Hunter

The implemented Bounty Hunter loop assigns opponents in **Edgeville Wilderness levels 1–5**, with eligible targets matched within five combat levels. Target information, hint arrows, skips, return allowances, cooldowns and streak tracking support the fight.

Eligible PvP kills have a **10 million Blood Money base reward**. Killing the same player again within **five minutes** gives no generated kill rewards, including Blood Money, BH points and emblem upgrades. This reward cooldown also applies in Edgeville and on DEV test accounts, persists across relogs and reports its remaining time when a repeated kill is blocked.

Eligible assigned-target kills award or upgrade mysterious emblems through **tiers 1–10**. Only the highest carried emblem upgrades. Non-target kills do not create or upgrade emblems. Lost emblems drop privately for the killer, downgrading one tier above tier 1.

Emblems redeem directly for Blood Money through the Emblem Trader or inventory redemption:

| Tier | Blood Money | Tier | Blood Money |
| --- | ---: | --- | ---: |
| 1 | 3,000,000 | 6 | 20,000,000 |
| 2 | 5,000,000 | 7 | 30,000,000 |
| 3 | 7,500,000 | 8 | 50,000,000 |
| 4 | 10,000,000 | 9 | 75,000,000 |
| 5 | 15,000,000 | 10 | **100,000,000** |

Matching and rewards include account/IP checks, recent-opponent restrictions and persistent repeat-reward history. Redemption rechecks inventory ownership, item state, location and payment limits before consuming an emblem. Ancient Warriors' equipment follows the server's Bounty Hunter activation rules, with an exception for maximum-hit dummy testing.

See [Bounty Hunter rules and verification](docs/bounty-hunter.md) for the full eligibility, death, skip and redemption behavior.

BH points remain separate from Blood Money. A kill against a player without an emblem awards **1 point**; emblem tiers 1–10 award **0.1953125 to 100 points**, doubling with each tier. An assigned-target kill adds **10 points**. These awards share the five-minute repeat-victim cooldown.

### Loot keys and Skully

Talk to **Skully at `(3099,3510,0)`** to enable loot keys and configure consumable/high-value/low-value ground drops. Right-click **Value** to see the saved number of fully claimed keys, value claimed and value destroyed.

Use a key on the **Loot chest at `(3095,3506,0)`** to open **Wilderness Loot Keys**. Select a key tab, withdraw one item by clicking it, or use the footer buttons to withdraw contents to inventory, deposit them directly to the bank, or confirm destruction of the selected key and remaining loot. **Item/Note**, scrolling, close and Escape controls work. Space and stack limits leave unclaimed items inside the key.

Keys preserve item quantities and attributes, hold up to 28 item entries and are limited to five carried keys. They cannot be banked or reproduced through presets. Held keys are lost on death and can transfer to an eligible killer; ironmen can destroy their contents but cannot claim them. See [loot-key rules, controls and verification](docs/loot-keys.md).

### Combat and Wilderness systems

- Melee, ranged and magic combat; weapon styles, ammunition, requirements, prayers, special attacks, equipment effects and item charges.
- Standard, Ancient and Lunar spellbooks with their classic desktop layouts; combat spells, teleports and implemented utility spells.
- Maximum-hit dummies that use maximum damage rolls, including consistent Vesta's longsword specials and dragon-claw multi-hit sequences. Existing dummy tests preserve special energy and stop after the requested attack.
- Wilderness death/loot handling, skull and risk systems, kill streaks, ELO, hotspots, blacklists and risk protection.
- [Loot keys](docs/loot-keys.md): Skully's settings and claimed-key/value totals, with a chest window for inventory/bank withdrawals, notes and confirmed destruction.
- Revenant Caves, Wilderness bosses/events, the resource area, Chaos Altar, Mage Arena-related content, keys/chests, volcano content, obelisks, levers and webs. Individual features have different verification levels.

Combat corrections and remaining equipment-parity gaps are recorded in the [combat audit](docs/combat-audit.md).

## Edgeville home and economy

The home retains Edgeville's bank and services, with a nearby workshop containing the **PvP shop**, rejuvenation pool, altar and chest.

The market provides:

- **General Market** with 178 ordinary items from the new list, replacing its old stock, and a separate **PvP Equipment** points store containing 17 Vesta, Statius, Morrigan and Zuriel items.
- One scrollable catalogue with nine categories, sidebar shortcuts, item search and server-provided prices.
- A native right-click menu with **Buy 1, Buy 5, Buy 10 and Buy X**; left-click reports the price.
- Blood Money/token payments and 40% buyback in General Market; existing free entries remain free.
- Bounty Hunter points and no buyback in PvP Equipment.
- Capacity, currency, distance and active-shop checks, including revalidation of entered quantities.
- Immediate local closing that also dismisses the underlying native shop windows.

The BH points shop uses the following prices per item:

| Equipment | BH points |
| --- | ---: |
| All Vesta and Statius melee items | 500 |
| Morrigan's throwing axe / javelin | 20 / 25 |
| Morrigan's coif | 250 |
| Morrigan's leather body / chaps | 400 each |
| Zuriel's hood | 250 |
| Zuriel's robe top / bottom | 500 each |
| Zuriel's staff | 400 |

The [item mapping and unresolved-list review](docs/pvp-equipment-unresolved.md) records which appearance-only requests still need exact identities; the active catalogue does not invent replacements for those entries.

Blood Money is the main cash currency. Legacy coin items normalize to Blood Money in item/container paths; activity tokens and refunded credits retain their own roles. Banking, direct trading, shops, a Trading Post with offers/coffer, item storage, charges, combinations and upgrades are also present. Trading Post access is restricted for ironmen.

See the [market documentation](docs/pvp-market.md) for catalogue rules and transaction checks. Economy-wide balance, legacy conversions and transaction edge cases remain part of the launch review.

## Other activities and PvM in the source

These inherited systems have substantive gameplay code. Their presence does **not** mean every encounter has been completed or every reward path verified live; the [content audit](docs/content-audit.md) records the current boundaries.

| Content | Systems present |
| --- | --- |
| Last Man Standing | Lobby/queue, game modes, supplies, fog, crates/keys, eliminations, winners and Survival Token rewards |
| Tournaments | Entry manager, queues, maps, modes, presets, fights and brackets/playlists |
| Duel Arena and free-for-all Clan Wars | Duel offers/rules/fights/settlement; FFA portals, maps and combat rules |
| Barrows | Brothers, tunnels, chest and rewards |
| Fight Caves and Inferno | Wave progression, monster scripts, Jad/Zuk encounters and reward paths |
| God Wars Dungeon | Four generals, minions, access/kill-count mechanics and altars |
| Chambers of Xeric | Raid rooms, bosses, party/session logic, raid skilling, storage and rewards |
| Boss encounters | Zulrah, Vorkath, Hydra, Cerberus, Abyssal Sire, Kraken, Thermonuclear Smoke Devil, Kalphite Queen, Corporeal Beast, Dagannoth Kings, Giant Mole, Skotizo, King Black Dragon and other PvM scripts |
| General PvM | Dragons, revenants, Slayer monsters, demonic gorillas, shamans and other monster/drop handlers |
| Private PvM instances | Twelve boss-instance types covering Mole, DKs, Kraken, KBD, Smoke Devil, Cerberus, Corp, KQ and the four GWD generals |
| Pest Control | Boats/lobbies, portals, monsters, points, rewards and XP exchanges |
| Wintertodt | Cold, braziers, pyromancers, points and reward crates; some damage/reset work remains |
| Skilling activities | Motherlode Mine, Warriors' Guild armour/cyclopes/defenders, Puro-Puro and aerial fishing |
| Clue scrolls | Tiers/rewards and selected anagram, cryptic and map steps |
| Events and progression | Daily NPC/PvP tasks, loyalty chest, seasonal Summer Shrine, scheduled event bosses, donor-zone handlers and small appreciation/viewing interactions |

## Existing skills

All **23 skills** are represented. The seven combat skills are freely selectable as described above. The following 16 noncombat skills retain their existing progression, with further development deferred:

| Skill | Existing systems |
| --- | --- |
| Agility | Gnome, Barbarian and Wilderness courses, eight rooftop courses, shortcuts and marks |
| Construction | Persistent houses, rooms, furniture/hotspots, materials, servants, portals, altars, pools and storage |
| Cooking | Cooking food, wine and guild interactions |
| Crafting | Gems, jewellery, armour, battlestaves, glass, pottery, spinning and silver work |
| Farming | Saved patches/crops, compost, tools and herb/allotment/flower/hops/bush/tree/specialised crops |
| Firemaking | Log burning and Wintertodt-related content |
| Fishing | Tools, spots, catches, guild, clue bottles and aerial fishing |
| Fletching | Arrows, bolts/tips, darts, javelins, bows, crossbows and ballista production |
| Herblore | Herb cleaning, unfinished/finished potions, tar and grinding |
| Hunter | Birds, chinchompas, salamanders, traps, implings and Puro-Puro |
| Mining | Rocks, pickaxes, essence, guild and Motherlode Mine |
| Runecrafting | Altars, Abyss, pouches, Ourania and dense essence |
| Slayer | Masters, weighted assignments, requirements, blocked/unlocked tasks, tracking and shops |
| Smithing | Smelting, bars, anvil products and forging-ring support |
| Thieving | Pickpocketing, stalls and wall safes |
| Woodcutting | Trees, axes, guild interactions and associated rewards |

## Accounts, progression and client features

- Standard, Ironman, Ultimate Ironman and Hardcore Ironman account modes, character persistence, permissions and staff controls.
- Friends/chat/clans, settings, emotes, music, travel/teleports and administrative jail support.
- A generated `::commands` catalogue filtered by the current character's permissions, with search and pagination.
- Quest-tab summary with real kills/deaths, Slayer task, rank, vote/spending information, items-on-death access and achievement navigation.
- Achievement tiers, bestiary statistics, pets, mystery boxes, chests and activity reward systems. Collection-log totals are currently **not tracked**.
- Voting, donation, gambling/dicing and web/database integration code; these require configuration and appropriate transaction verification before use.
- Lletya client branding, login artwork and application names, plus native-menu, camera-input and interface fixes.
- Automatic inactivity/six-hour logout requests and their warnings removed; manual logout and connection-loss handling remain.
- Optional live Discord announcements configured through environment variables in [.env.example](.env.example).

## Incomplete content and next priorities

The current source includes unfinished **Theatre of Blood rooms, Nightmare Zone, Boss Rush, Pyramid Plunder, Party Room and competitive/team Clan Wars**. The implemented FFA Clan Wars system is separate from that unfinished team-match work.

There are **37 registered placeholder spells**, including Vengeance Other, Spellbook Swap, Heal Other and Energy Transfer. Main Vengeance is implemented. A complete quest catalogue and collection log have not been established.

The next development priorities are:

1. Continue live PvP testing across pure, hybrid, tribrid and max builds, including specials, prayers, freezes, teleblocks, deaths and disconnects.
2. Verify the full Bounty Hunter loop with multiple eligible players, including ground loot, target cleanup and repeat-reward protections.
3. Review currency conversions, trades, Trading Post, staking, buyback and reward sources for transaction integrity and economy balance.
4. Validate complete LMS rounds and tournament brackets, including reconnect and reward behavior.
5. Finish or hide incomplete PvP support spells and establish production account permissions before public access.
6. Return to broader skilling, shop expansion and unfinished activities after the PvP core is ready.

## Development and local play

The working source is in `main/`, based on [Kronos-184-Fixed](https://github.com/Leif-Yggdrasil/Kronos-184-Fixed) and its bundled RuneLite/OpenOSRS client. Upstream notices and client licenses are retained. This is the owner's private project repository; cache assets, credentials, player saves, local toolchains and build output stay outside Git.

Use **JDK 21** for the game/update servers, **JDK 8** for the desktop client, and the checked-in Gradle wrappers. The cache and local configuration are required separately. See [local setup and launch instructions](docs/local-running.md) for the complete environment requirements.

On an already configured Windows workspace, start the servers and client using **Start Server.bat** and **Start Client.bat**, or run everything with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-local.ps1
```

Use **Stop Server.bat** to stop the local servers. DEV operation is local development configuration, including elevated account permissions; it is not the public-server configuration.

In DEV mode, **Jon**, **Jon2** and **Sales Tax** have explicit exceptions for local same-IP login/matching and ironman-mode trade testing. Their repeated matches still obey the five-minute reward cooldown. Production restrictions remain active outside DEV. See [DEV account testing](docs/dev-testing.md) for trading and Bounty Hunter instructions.

Latest publication checks, **28 October 2026**: the complete server suite (**195 tests**), **1 API test** and **55 project client tests** passed with no failures or skips. Renamed server, update-server, launcher and client JAR builds succeeded, and the cache-based home-layout check reported zero failures across 99 NPC and 62 object placements. Checks cover trade confirmations, presets, currencies, market catalogues, BH rewards/visibility, loot-key transactions and every chest-window control. The loot window and new loading background were visually inspected. Current source and filenames are checked for superseded names. Runtime saves and existing client settings were retained during the filesystem move. Earlier localhost login and selected preset/spellbook/market flows were exercised separately; the newest loot-key UI and full multiplayer behavior still need live verification after restart.

| Documentation | Purpose |
| --- | --- |
| [Roadmap](docs/roadmap.md) | Delivered behavior and development direction |
| [Command help](docs/commands.md) | Player commands, shortcuts and permissions |
| [Bounty Hunter](docs/bounty-hunter.md) | Targeting, emblems, payouts and eligibility |
| [Loot keys](docs/loot-keys.md) | Skully, settings, chest UI, withdrawals and saved Value statistics |
| [PvP market](docs/pvp-market.md) | Catalogue, payments, buyback and interface behavior |
| [Combat audit](docs/combat-audit.md) | Combat corrections, equipment checks and remaining gaps |
| [Content audit](docs/content-audit.md) | Full skill/activity inventory and implementation status |
| [Local running](docs/local-running.md) | Toolchains, configuration, starting/stopping and smoke checks |
| [Player accounts](docs/player-accounts.md) | Persistence and development account behavior |
| [Branding](docs/branding-status.md) | Client/server branding and community links |
| [Baseline review](docs/kronos-baseline-review.md) | Source provenance and historical investigation |

The original project requirements remain in [Start.txt](Start.txt).
