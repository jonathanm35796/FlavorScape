# FlavorScape Roadmap

Updated after implementation and verification on 2026-10-28. Record completed work, desired behavior, and future requests here; update this file at the end of a work batch.

## Current implementation

Implemented locally in `main/`:

Publication verification on 2026-10-28: 193 server tests, 1 API test and 55 project client tests pass, with no skips or failures. Both JAR builds and the cache-based home-layout check succeed. The latest loot-key interface has an inspected offline preview; live verification after server/client restart remains pending.

| Request | Delivered behavior |
| --- | --- |
| Named DEV test accounts | DEV-only allowlist for Jon, Jon2 and Sales Tax enables local multi-account login, same-IP BH pairing and repeat matching, plus ironman-mode trades between named testers. The five-minute repeat-victim reward cooldown still applies. Production and ordinary-account protections remain. Same-IP trading is verified through both screens; stale request timers are guarded. [Instructions](dev-testing.md). |
| Bounty target panel / shop access | Corrected target name/location cache widget indices, unhid the native target panel and retained native emblem/wealth scripts. Server visibility now survives native Wilderness scripts and widget reloads. Matching publishes hint arrows and the correct 1–5 Wilderness label. Removed the shop's table and verified direct NPC access. |
| Shop catalogue replacement | General Market opens with 178 ordinary entries from the new list, using Blood Money/tokens and 40% buyback. PvP Equipment contains only 17 Vesta/Statius/Morrigan/Zuriel items, priced at the requested 20–500 BH points with no buyback. The old inherited stock and supplemental 1,311-item expansion are excluded. [Catalogue, prices and verification](pvp-market.md). |
| PvP kill rewards | 10M Blood Money base, fractional BH points and assigned-target bonuses. A single persistent five-minute repeat-victim gate blocks all generated rewards in every world stage, including Edgeville and DEV testers. Blocked attempts show remaining time without extending the timer. |
| Loot keys and Skully | Skully at `(3099,3510,0)` exposes settings and saved Value statistics; the chest at `(3095,3506,0)` opens a reference-style window with inventory/bank withdrawal, notes, key selection, scrolling, close and confirmed destruction. Five-key carrying limit, attributed-loot persistence, partial claims, stack limits, death transfer and bank/preset protections are covered by automated checks. [Rules and verification](loot-keys.md). |
| Dummy maximum-hit rolls | The real dummy listener now selects maximum damage before accuracy and multi-hit splitting. Vesta's special always retains its full damage range even when its ordinary accuracy roll fails. Dragon claws consistently display the maximum legal four-hit sequence. Ordinary target combat rolls remain random. |
| Content audit / free combat levels | All 23 skills and activity families reviewed; source/data ledgers cover 1,420 server files and 2,411 data files. Every account can freely raise/lower seven combat skills through Skills-tab clicks, `::levels` and `::level attack 75`, with combat/PvP/activity and equipment checks. HP 10–99, other combat skills 1–99; noncombat progression preserved. [Findings, controls and verification](content-audit.md). Client/server restart required; this batch has automated verification, not a live multi-player playthrough. |
| Edgeville Bounty Hunter | Assigned targets, tier-1–10 emblems, target-only upgrades and private downgraded death drops. Every tier redeems directly for Blood Money; tier 10 pays **100,000,000**. Emblem Trader Talk-to and item-use both offer redemption. Account identities, repeat-reward checks and confirmation validation have been repaired. [Rules, payout table and verification](bounty-hunter.md). |
| Classic spellbooks | Login uses the built-in unfiltered desktop grid shown in `Standard.png`, `ancidents.png` and `Lunars.png`: Standard seven columns, Ancient four, Lunar six. Unavailable spells stay in their normal positions. Old saved filter settings no longer restore the redistributed layout at login. The magic-tab context-menu filter toggle remains available for the current session. |
| Edgeville workshop / PvP market | Original bank restored. The room within `(3091,3506,0)`–`(3098,3511,0)` contains the pool moved to the northeast and a Shanomi-style merchant with its blocking table removed. Trading **PvP shop** opens the General Market / PvP Equipment selector, category headers, sidebar shortcuts, search, server prices and native Buy 1/5/10/X menus. General Market accepts Blood Money and platinum tokens at 1,000:1, with 40% catalogue buyback. PvP Equipment uses BH points. [Details and verification](pvp-market.md). |
| Gear interface | The Quest tab follows the supplied Summary card layout. Click the **dragon med helm** in its bottom button row to open gear presets; The standard **X** button dismisses them, with the normal hover sprite. Gear controls appear only while the Quest summary is selected, and tab changes hide the panel without discarding its open state. Only a completed load requested from this panel closes it; stale load notifications are ignored. Escape and the X button cancel any pending load-close signal so a late response cannot close the next opening. The panel stays logically open through temporary summary-widget rebuilds, fits inside the game viewport above the chatbox, and has equipment slots, counts, selection, loading, and a concise **Presets** hover label. |
| Default setups | The six requested setups seed slots 1–6 in a unified list of twelve editable presets. Each account receives independent copies with equipment, inventories, combat levels and spellbooks. Existing saved presets retain their slots during migration; defaults fill vacant slots 1–6. The initial defaults retain their PvP spawning rules. |
| Editable preset slots | Every account can use all twelve slots without a donator rank. **Save current** works on every slot, including defaults, and captures inventory, equipment, combat levels, spellbook, active prayers and quick-prayer selections. Replacing a populated slot requires confirmation. **Manage selected preset** provides naming, deletion and swapping. Edits persist; deleted defaults do not reappear on login. Edited defaults use normal saved-gear ownership rules. |
| Quick controls | Click the dragon med helm or use `Ctrl+G` on the Quest tab to open presets. With the panel visible, `F1`–`F12` load matching slots; held keys do not repeat requests. Normal function-key tab controls remain available outside the panel and during amount/name entry. `Ctrl+Q` reloads the most recently loaded preset. `Esc` closes the gear interface only when the existing **Esc closes interfaces** option is enabled. |
| Safe replacement | Loading checks combat, location, account restrictions, and bank capacity before depositing the current setup. Saved loadouts use the existing bank/spawn rules. The gear interface closes when the server confirms a completed load; rejected loads leave it open. |
| Blood Money | Coin awards, costs, drops, shops, and reward paths use Blood Money. Legacy coin items, including noted/alternate coins, normalize when created or loaded into saved containers. Numeric amounts are preserved. Activity tokens and refunded credits keep their own currencies. Currency labels were updated. |
| Logout timer | Removed the automatic inactivity logout request and idle/six-hour logout warnings, including the idle logout notification setting. Manual logout and lost-connection handling remain available. |
| Server errors | Repaired the unconfigured Refunded Credit Exchange, removed the shop enum-null assumption, consumed route callbacks before invocation to prevent repeated failures, and handled ordinary socket resets as disconnects. The launcher renders native logs without PowerShell error wrapping and permits reading live logs. |
| Client metadata errors | Item-stat parsing skips unknown upstream fields such as `ge_limit`; non-success price/stat responses return empty optional data instead of JSON stack traces. Client logs use `.flavorscape/logs` with the current non-deprecated rolling policy. The legacy RuneLite price URL currently returns 404, so market-price data remains unavailable. |

Verification: client/server builds and all 15 API/server regression tests pass. Actual localhost client checks loaded all six presets, compared equipment and levels, saved/restored a custom inventory and active prayer, exercised both shortcuts, and clicked open/close controls. Saved character JSON contains the custom inventory, equipment, and prayer selections. The fixed-size interface was visually inspected. Startup and these interactions produced no shop exceptions. Logout removal was checked in code; a six-hour session was not timed.

Editable-preset follow-up (2026-10-26): server/client builds pass along with fifteen server preset/command tests and thirteen client gear tests (`.tools/editable-presets-server-tests.log`, `.tools/editable-presets-client-tests.log`). Coverage includes independent defaults, preserving legacy saved slots, persistence of edits/deletions and default spawning behavior, all twelve F-key mappings, key-repeat suppression, hidden-panel/input-dialogue guards and save/load/manage mapping for slots 1 and 12. The localhost smoke probe uses the unified slot numbering. Restart the client and game server to apply these changes; this follow-up has automated verification, with in-game visual verification still pending.

Reference-layout follow-up verified: the helm opens the panel, other tabs hide/close it, panel bounds stay within the actual game viewport, and the Achievements button plus journal Back navigate correctly. The Summary uses real kills/deaths, Slayer task, account rank, spending, and vote counts. Risk opens the existing items-on-death view. Collections progress displays **Not tracked**, because this source has no collection-log counter; screenshot example totals are not fabricated.

Input follow-up verified: Quest and gear overlays no longer consume mouse releases, which could leave native camera drag held after a release over the interface. Middle mouse and Alt-left camera controls pass through normally; ordinary interface left-clicks remain captured. Six targeted client tests pass for mouse handling and load notifications. Actual native-client checks confirmed held buttons reset over both overlays, successful loading closes the panel, and rejected loading keeps it open. All 15 API/server regression tests also pass.

## Past work

The local baseline, FlavorScape branding, Edgeville home, launch scripts, and command catalogue are described in [local running](local-running.md), [branding status](branding-status.md), and [command help](commands.md). The owner authorized the current source backup to the private FlavorScape GitHub repository on 2026-10-27. Cache assets, credentials, saves, local toolchains and generated output remain excluded.

## Desired behavior and original requests

### Quest-tab combat presets

Make the Quest tab work like the supplied screenshots: preserve the existing summary panel and add a compact, scrollable gear-preset panel alongside it. Show the preset count, each preset's equipment preview/slots, and clear selection and load actions. Keep the layout readable at the client's fixed tab size.

Include the screenshot's quick controls: `Ctrl+G` to open/close the gear panel and `Ctrl+Q` to load the latest gear preset. Show a tooltip or equivalent hint for these actions.

#### Visual reference screenshots

The supplied references show the existing Quest-tab summary (statistics, Slayer task, collections, and membership) and the same tab with a gear panel open beside it. The open panel has a `Gears 8/6` header, a scrollable numbered preset list with equipment previews and combat-style labels, and the summary remains visible. Match the compact classic client framing and avoid obscuring the existing quest information.

The supplied [Quest Tab.png](<Quest Tab.png>) reference is archived as [quest-tab-summary.png](roadmap-assets/quest-tab-summary.png). The separate open-gear reference is still unavailable as a workspace file; `docs/roadmap-assets/quest-tab-gear-presets.png` remains its intended archive path.

Provide these loadouts:

- Hybrid (max)
- Tribrid (max)
- Pure (52 Prayer)
- Pure hybrid (52 Prayer)
- Max melee
- Max range

Define each preset's equipment, inventory, spellbook, combat levels, skill levels, and prayer consistently. Applying a preset should replace the player's current setup safely and respect the intended PvP environment. The preset list and equipment previews should remain consistent with the selected preset and saved gear.

Follow-up requests: use a clickable button to open the interface, and provide custom boxes that save the current inventory, equipment, and prayer setup. Both are included above. Noncombat skill levels are preserved.

### Blood Money as the only coin currency

Replace standard coins with Blood Money for all currency generated or awarded in-game. Audit every source, including ground drops, pickpocketing, NPC drops, shops and sales, PvM/PvP rewards, activities, and other reward systems. Ensure currencies cannot bypass the change through inventory or bank rewards, item notes, trading, or alternate award paths. Preserve intended item costs and verify that no gameplay source still grants standard coins.

### Remove logout timer and notification

Remove the logout countdown/timer and its associated warning notification so players are not logged out by that timer. Check both the client display and server-side logout behavior so they stay consistent.

## Future requests

Workshop/market verification on 2026-10-26: nine server transaction/catalogue tests and nine client tests pass; both builds pass. Cache-based layout validation reports 97 active NPCs, 62 dynamic objects and zero failures for the corrected room placement, restored bankers and counter access. Actual-client NPC trade, purchase, exact 40% buyback, category navigation, search and close now pass. Market/workshop screenshots were visually inspected. Follow-up fixes correct tooltip line breaks and prevent hover errors when shop slots exceed the optional item-attribute array. Existing clients must restart for these client fixes. See `docs/pvp-market.md` for logs and screenshots.

Classic spellbook verification on 2026-10-26: server build and two regression tests pass, covering new accounts, old saved filter settings, spellbook switching and reset after a session filter toggle. Actual localhost login now verifies the classic-layout default and server-driven switching through all three books. The rendered grids contain 70 Standard spells in seven columns, 26 Ancient spells in four columns and 45 Lunar spells in six columns. Screenshots in `.tools/spellbooks/` were visually compared with the supplied references; `SPELLBOOK_SMOKE PASS` is recorded in `.tools/main-Client-84196.log`. The reusable opt-in probe is documented in `docs/local-running.md`.

No additional feature requests are queued. Archive the separate open-gear reference when that file becomes available. Record new requests here and move them into completed history after implementation and verification.

Market appearance follow-up (2026-10-26): matched `Desired Market.png` with square framed controls and close/dropdown/scroll buttons, segmented borders, a centered bold market title and larger category text. Removed the market hover tooltip and item highlight shown in `Overlay.png`; native action tooltips are suppressed while the market is open. Seven market client tests and the full actual-client transaction/navigation smoke check pass. The hover-state screenshot is `.tools/market-verified.png`. Restart existing clients to load this update.

Old School/dropdown follow-up (2026-10-26): selector clicks now toggle the menu; outside clicks and Escape dismiss it without closing the shop. Replaced the segmented frame with a simpler brown panel and native cached close/scroll-arrow sprites. Scrollbar painting and input share the native-style geometry; grabbing the thumb preserves its position, track clicks page, arrow clicks move four pixels, wheel steps move 45 pixels, and scrolling stops at the catalogue bottom. Eleven market client tests pass. The actual-client probe verifies dropdown dismissal, both shop selections, wheel/drag behavior and all existing transactions/navigation (`.tools/main-Client-79464.log`). Latest screenshot: `.tools/market-verified.png`. Existing clients need a restart.

Market purchase controls (2026-10-26): removed the 1/5/10/All footer buttons and quantity mode. Left-click only reports the authoritative price in general chat. Right-click offers Buy 1, Buy 5, Buy 10 and Buy X; Buy X uses the native numeric dialogue and revalidates the shop and purchase limits when submitted. Obsolete Buy 50/All command amounts are rejected. Twelve server market tests and twelve client market tests pass, including the Windows right-click modifier regression. Connected characters were saved and logged out before the local servers restarted with the new protocol. Existing clients need a restart.
