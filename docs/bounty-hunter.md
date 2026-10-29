# Edgeville Bounty Hunter

## Kill points and PvP Equipment (2026-10-27)

Eligible PvP kills now have a base reward of **10,000,000 Blood Money**, with existing kill bonuses retained. They also award Bounty Hunter points based on the victim's highest carried emblem: tier 10 gives 100, then 50, 25, 12.5, 6.25, 3.125, 1.5625, 0.78125, 0.390625 and 0.1953125 for tiers 9 through 1. A victim without an emblem gives 1 point. Killing your assigned bounty adds 10 points. A shared **five-minute reward cooldown** blocks all generated kill rewards from the same victim, including Blood Money, BH points, emblem upgrades and bonus drops. It applies in Edgeville and to named DEV test accounts. Blocked attempts show the remaining minutes/seconds and do not extend the cooldown; a different eligible opponent can still pay immediately. Reward history persists across relogs. Normal same-account/IP checks remain, with the named DEV same-IP exception retained. Ordinary death loot is unchanged.

Points persist with the account in exact units of 1/512 point. Spend them through the market's **PvP Equipment** selection. Emblem redemption still pays the Blood Money amounts below; these kill points are separate from redemption.

The 10M base reward and shared five-minute cooldown are verified by production reward-path tests, including Edgeville, named DEV pairs, relogged histories and expiry at exactly five minutes. New [loot keys](loot-keys.md) package ordinary death loot separately from generated kill rewards.

Implemented locally on 26 October 2026. This is the assigned-target, tier-1–10 emblem system requested for FlavorScape. Emblems redeem directly for Blood Money. On 28 October 2026, all tier redemption payouts were increased by 1,000 times, bringing tier 10 to 100,000,000 Blood Money.

## Playing

1. Enter Edgeville Wilderness with targeting enabled. Accounts now default to **Edgeville Only**, covering Wilderness levels 1–5 within `(2993,3523)` to `(3124,3597)`.
2. The matchmaker checks about every 12 seconds for an eligible player within five combat levels. Both players receive each other as their target, with the normal target panel and hint arrow.
3. Kill your assigned target to receive a private tier-1 emblem drop. Pick it up and carry it on further eligible target kills to upgrade it one tier at a time.
4. At tier 10, return safely to the Emblem Trader at `(3089,3503,0)`, below Edgeville bank. **Talk-to → Trade in my highest emblem** opens a confirmation for **100,000,000 Blood Money**. You can also use a specific emblem on the trader or choose Redeem in your inventory.

Non-target kills do not create or upgrade emblems. Only the highest carried emblem upgrades, once per eligible target kill. Tier 10 never becomes an invalid tier 11; another eligible target kill while the highest carried emblem is tier 10 drops a new tier 1.

| Tier | Blood Money |
| --- | ---: |
| 1 | 3,000,000 |
| 2 | 5,000,000 |
| 3 | 7,500,000 |
| 4 | 10,000,000 |
| 5 | 15,000,000 |
| 6 | 20,000,000 |
| 7 | 30,000,000 |
| 8 | 50,000,000 |
| 9 | 75,000,000 |
| 10 | **100,000,000** |

Lost emblems cannot be protected. Tier 2–10 downgrades one tier and drops privately for the killer; tier 1 remains tier 1 when dropped. This uses the existing production death/drop path with a shared tier table.

## Matching, skips and rewards

- Accounts receive stable positive internal IDs from the file-login service, fixing the former shared `-1` identity that prevented matching.
- The matchmaker rejects the same account, the same IP, dead players, disabled/out-of-area players, pairs outside the five-level combat range, and players with an active target or penalty. Recent opponents are excluded for ten minutes in either direction.
- All generated kill rewards reject same-account/IP kills and a victim account or IP already rewarded within five minutes. Named DEV pairs retain their same-IP allowance, but their victim-account reward cooldown still applies. Reward history persists across relogs. These checks apply even when targets are injected or changed outside ordinary matching.
- The existing All Wilderness and Disabled settings remain available. New accounts and the previous default All Wilderness configuration migrate once to Edgeville Only. Explicit later changes persist.
- Manual skip requires about ten seconds out of defending combat. Ten skips within the tracking window impose a 30-minute target penalty. Automatic logout, disabled-mode and return-timeout cleanup cannot be blocked by that manual combat restriction.
- Leaving the active targeting area starts the existing two-minute return allowance. A minute warning is retained. Remaining back in the active area for 60 ticks clears the return countdown.
- Eligible target kills retain the existing one-minute winner and six-minute victim target cooldowns. Ordinary Blood Money kill rewards and their separate modifiers remain in the general PvP reward system.
- Target and rogue streaks reset on death; best records persist. Target-risk multiplication now uses 64-bit arithmetic. Overlay visibility respects the player's overlay setting, and the enable message is corrected.
- Bounty teleport retains its scroll, level and rune requirements, and now propagates failed movement/teleport requests so blocked casts do not report success to the rune-removal wrapper.

Normal same-IP rejection also applies to localhost clients. In **DEV only**, distinct accounts **Jon**, **Jon2** and **Sales Tax** have explicit same-IP and repeat-matching exceptions. Both participants must be named test accounts; these exceptions are disabled in LIVE/BETA. The five-minute reward cooldown applies in every world stage. See [DEV testing instructions](dev-testing.md).

Target UI repair, 2026-10-27: names/location now update real text widgets in the current cache (90:53/54), instead of a rectangle/sprite at the old indices. Native emblem/wealth fields remain controlled by the cache's script, using the target's highest emblem tier. The client unhides the target container as well as its parent, and assignment refreshes visibility. Both players receive their target's hint arrow. Wilderness range labels now correctly show 1–5.

Wilderness-entry visibility repair, 2026-10-27: native script 866 can hide the target panel's parent again after the server enables it. The client now retains the server's overlay visibility for 90:6 and 90:18 across script updates and widget reloads. Other Wilderness widgets keep their native visibility behavior. Restart the client to load this repair.

Verification for this visibility repair: five client regressions pass, including execution of the native hide opcode, disabled-overlay behavior and interface reloads; all seven DEV targeting/name/emblem checks pass, and the client JAR builds successfully. Rendered in-game confirmation remains pending after the client restart. Logs: `.tools/bounty-overlay-client-tests.log` and `.tools/bounty-overlay-server-tests.log`.

## Redemption transactions

Confirmation rechecks the exact item object, slot, tier, amount, inventory ownership, combat/death/lock state and safe location. Removing, replacing, swapping or upgrading the emblem invalidates an older confirmation. A repeated confirmation cannot pay twice. Blood Money integer overflow is rejected before the emblem is consumed. A full inventory can redeem successfully because the emblem frees a slot for the payment.

The authoritative tier IDs and values are in `MysteriousEmblem.java`; kills, death downgrades, help text and the trader share them. Inventory and trader redemption use the same validation and transaction code.

## Scope of the Impact reference

Impact's published [command guide](https://impactmmo.wiki/w/index.php?stable=1&title=Commands) describes Edgeville Bounty Hunter, and its [home guide](https://impactmmo.wiki/w/index.php?stable=1&title=Home_Area) describes an Emblem Trader and Bounty Points. A complete authoritative Impact tier-value and eligibility specification was not available during implementation. FlavorScape therefore implements the owner's stated assigned-target/emblem loop and approved Blood Money payouts; it is not claimed as a verified exact copy of every Impact mechanic.

## Verification

The standalone audit exercises 35 production-method checks, including all previously reported failure cases. The lifecycle suite covers automatic pair assignment, first-emblem drop, tier-1–10 progression, highest-only upgrades, tier-10 cap, non-target/repeated/same-IP rejection, persistent histories, trader Talk-to and item-use redemption, full inventory, stale/repeated confirmations, all death-downgrade mappings and persistent account IDs. Existing equipment and dummy regressions cover the earlier Ancient Warriors' gear rules.

Final verification: **35/35 audit checks and 33/33 JUnit tests pass**, with a successful server build. The JUnit total is fifteen lifecycle/identity/trader cases, eight equipment cases and ten dummy interaction cases. Logs are recorded in `.tools/bounty-hunter-implementation-tests.log`; the current audit ledger is `.tools/bounty-hunter-audit.tsv`. The standalone audit now exits unsuccessfully if any check fails. The earlier [audit report](bounty-hunter-audit.md) is retained as the pre-implementation baseline.

These checks use isolated players, actual matching/reward/transaction methods and cached definitions. The tier-1 drop is captured at the production drop boundary and pickup is simulated. Complete two-client fighting, rendered target UI and ground-item pickup timing still require in-game verification. No running player account was edited or killed, and the game server was not restarted during implementation. Restart the game server to apply the update.
