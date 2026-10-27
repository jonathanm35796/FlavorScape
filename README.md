# FlavorScape

**An Edgeville PvP server built around freely chosen combat builds, fast gearing and Bounty Hunter.**

FlavorScape is an OSRS-style private server with a customized RuneLite/OpenOSRS desktop client. Our current focus is the PvP experience: choose your levels, load a setup, find a fight and earn Blood Money. The project is in active development; the feature inventory below distinguishes implemented systems from inherited content that still needs full gameplay verification.

[Join the FlavorScape Discord](https://discord.gg/QJDZX7U4y) · [Development roadmap](docs/roadmap.md) · [Full content audit](docs/content-audit.md)

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

Eligible assigned-target kills award or upgrade mysterious emblems through **tiers 1–10**. Only the highest carried emblem upgrades. Non-target kills do not create or upgrade emblems. Lost emblems drop privately for the killer, downgrading one tier above tier 1.

Emblems redeem directly for Blood Money through the Emblem Trader or inventory redemption:

| Tier | Blood Money | Tier | Blood Money |
| --- | ---: | --- | ---: |
| 1 | 3,000 | 6 | 20,000 |
| 2 | 5,000 | 7 | 30,000 |
| 3 | 7,500 | 8 | 50,000 |
| 4 | 10,000 | 9 | 75,000 |
| 5 | 15,000 | 10 | **100,000** |

Matching and rewards include account/IP checks, recent-opponent restrictions and persistent repeat-reward history. Redemption rechecks inventory ownership, item state, location and payment limits before consuming an emblem. Ancient Warriors' equipment follows the server's Bounty Hunter activation rules, with an exception for maximum-hit dummy testing.

See [Bounty Hunter rules and verification](docs/bounty-hunter.md) for the full eligibility, death, skip and redemption behavior.

### Combat and Wilderness systems

- Melee, ranged and magic combat; weapon styles, ammunition, requirements, prayers, special attacks, equipment effects and item charges.
- Standard, Ancient and Lunar spellbooks with their classic desktop layouts; combat spells, teleports and implemented utility spells.
- Maximum-hit dummies that use maximum damage rolls, including consistent Vesta's longsword specials and dragon-claw multi-hit sequences. Existing dummy tests preserve special energy and stop after the requested attack.
- Wilderness death/loot handling, skull and risk systems, kill streaks, ELO, hotspots, blacklists and risk protection.
- Revenant Caves, Wilderness bosses/events, the resource area, Chaos Altar, Mage Arena-related content, keys/chests, volcano content, obelisks, levers and webs. Individual features have different verification levels.

Combat corrections and remaining equipment-parity gaps are recorded in the [combat audit](docs/combat-audit.md).

## Edgeville home and economy

The home retains Edgeville's bank and services, with a nearby workshop containing the **PvP shop**, rejuvenation pool, altar and chest.

The market provides:

- **General Market** and **PvP Equipment** dropdown choices; the latter filters Ancient Warriors' equipment.
- One scrollable catalogue with nine categories, sidebar shortcuts, item search and server-provided prices.
- A native right-click menu with **Buy 1, Buy 5, Buy 10 and Buy X**; left-click reports the price.
- Blood Money and platinum-token payments at **1 platinum token = 1,000 Blood Money**.
- **40% buyback**, rounded down, for eligible catalogue items.
- Capacity, currency, distance and active-shop checks, including revalidation of entered quantities.
- Immediate local closing that also dismisses the underlying native shop windows.

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
- FlavorScape client branding, login artwork and application names, plus native-menu, camera-input and interface fixes.
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

Latest gameplay-change verification, **27 October 2026**: **127 server tests** and **32 client market/gear tests** passed. Actual localhost login and selected interface flows have also been exercised. Full live multiplayer and activity completion remain separate verification work.

| Documentation | Purpose |
| --- | --- |
| [Roadmap](docs/roadmap.md) | Delivered behavior and development direction |
| [Command help](docs/commands.md) | Player commands, shortcuts and permissions |
| [Bounty Hunter](docs/bounty-hunter.md) | Targeting, emblems, payouts and eligibility |
| [PvP market](docs/pvp-market.md) | Catalogue, payments, buyback and interface behavior |
| [Combat audit](docs/combat-audit.md) | Combat corrections, equipment checks and remaining gaps |
| [Content audit](docs/content-audit.md) | Full skill/activity inventory and implementation status |
| [Local running](docs/local-running.md) | Toolchains, configuration, starting/stopping and smoke checks |
| [Player accounts](docs/player-accounts.md) | Persistence and development account behavior |
| [Branding](docs/branding-status.md) | Client/server branding and community links |
| [Baseline review](docs/kronos-baseline-review.md) | Source provenance and historical investigation |

The original project requirements remain in [Start.txt](Start.txt).
