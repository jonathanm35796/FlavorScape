# Combat audit — 26 October 2026

The server was **not fully OSRS-accurate**. This audit corrects shared combat formulas and confirmed equipment/special-attack discrepancies, and records the remaining gaps. It does **not** certify every PvP/PvM encounter, item passive, or weapon interaction.

## Equipment results

Dummy follow-up, 2026-10-27: maximum damage is now selected by the dummy's real pre-defend listener before accuracy and dragon-claw splitting. Vesta's longsword retains its full special damage range even when its ordinary accuracy roll pre-blocks the hit. Dragon claws use the highest legal total roll and its corresponding four hits consistently; with a base max of 40, the existing split rules produce **39 / 19 / 9 / 10**. Fixed follow-up hits and rolled-damage modifiers keep their normal semantics. Cannons still deal zero to the dummy. Ordinary targets retain normal random rolls/misses. The earlier dummy energy-refund and one-attack behavior remain unchanged.

Verification: **127 server tests** and **32 client market/gear tests** passed (`.tools/dummy-shop-revert-server-tests.log`, `.tools/dummy-shop-revert-client-tests.log`). New regression tests exercise the actual dummy listener and both real special handlers over repeated attacks, plus modifiers, fixed hits, cannon behavior and ordinary-target misses. This batch also reverts the supplemental shop items and Skilling Market; live visual verification requires restarting client/server.

The effective item overlay contains **3,333 equippable IDs**. Later duplicate definitions replace earlier definitions, just as they do in the production loader.

| Result | Equipment IDs |
| --- | ---: |
| All 14 checked static bonuses match | 2,767 |
| Identity, variant, or slot needs review | 196 |
| ID absent from the reference equipment dataset | 370 |
| Confirmed bonus differences still outstanding among matched items | 0 |

**1,424 bonus values across 653 equipment IDs were corrected.** These include ammunition, armour, jewellery, and magic-damage rebalances. The audit checks five offensive bonuses, five defensive bonuses, melee strength, ranged strength, magic damage, and prayer. Matching requires the same ID and slot plus a compatible cache name or recognized variant. It never assigns a modern Wiki item's stats to a differently named custom item simply because its ID matches.

The reference is the [OSRS Wiki team's equipment dataset](https://github.com/weirdgloop/osrs-dps-calc/blob/main/cdn/json/equipment.json), retrieved on 26 October 2026. Its `magic_str` values are tenths of one percentage point; the comparison converts those units. Snapshot SHA-256: `E3D55D0FE2B1C78E114D31DC9517B67B53D0BFC8080273BB4C7DD9F392DE4696`. Raw reference files remain under `.tools/combat-audit/`.

Review the [complete equipment ledger](combat-audit/equipment-audit.tsv), [base-item corrections](combat-audit/bonus-corrections.tsv), [variant corrections](combat-audit/bonus-corrections-variants.tsv), and [duplicate definitions](combat-audit/duplicate-item-definitions.tsv). A `BONUSES_MATCH` result covers those static numbers only. `NO_WIKI_ID` means the dataset has no comparable entry; it does not prove that the item is custom or absent from the Wiki itself.

## Shared calculations corrected

- Removed the unexplained 22% magic and 12% melee accuracy increases in PvP.
- Removed the additional 45%-per-armour-piece magic interference penalty; negative equipment bonuses already affect the attack roll.
- Floored prayer-adjusted levels before applying stance/base bonuses; corrected the melee/ranged max-hit calculation and removed the offensive bonus incorrectly granted to longrange.
- NPC rolls use the NPC base adjustment. NPC magic defence uses Magic rather than a player-style Defence/Magic blend, with the Verzik/Ice demon exceptions.
- Player magic defence floors its weighted components separately and applies the full stance/base adjustment.
- Spellbook casts use their normal effective accuracy; accurate powered staves retain their distinct stance bonus.
- Moved Void accuracy/strength bonuses to effective levels before roll/max-hit rounding. Mixed normal/elite bodies can retain ordinary Void effects. Elite mage damage is added with the other magic-damage bonuses.
- Added fractional magic-damage support through the actual data loader, equipment totals, combat getters, and equipment display. Updated the existing Mystic Lore, Mystic Might, and Augury damage bonuses.
- Added ordered multiplicative modifiers for verified native effects and special attacks, preserving legacy modifier APIs used by custom content.

Formula references: [Melee DPS](https://oldschool.runescape.wiki/w/Damage_per_second/Melee), [Magic](https://oldschool.runescape.wiki/w/Magic), and the Wiki team's [player attack calculator](https://github.com/weirdgloop/osrs-dps-calc/blob/main/src/lib/PlayerVsNPCCalc.ts) and [NPC attack calculator](https://github.com/weirdgloop/osrs-dps-calc/blob/main/src/lib/NPCVsPlayerCalc.ts). Current magic bonuses are documented under [Magic damage](https://oldschool.runescape.wiki/w/Magic_Damage).

## Equipment and special-attack corrections

| Area | Corrected behavior |
| --- | --- |
| Dragon warhammer | A miss no longer drains Defence; successful damage drains 30% of current Defence after accuracy is resolved. |
| Dragon dagger | 15% accuracy modifier, slash target defence independent of selected offensive bonus, and staggered PvM hits. |
| Dragon claws | Removed the invented accuracy/minimum-damage bonuses; up to four accuracy rolls, outcome-dependent damage splits, and all-miss fallback. Each hitsplat receives target mitigation independently. |
| Godswords | Corrected Bandos accuracy and drain order; applied fixed slash target defence. Armadyl/Bandos damage modifiers floor at their separate stages. Saradomin restoration respects minimum recovery and overkill damage. |
| Other dragon specials | Corrected longsword damage; separated fixed target defence from selected attacking bonus. Halberd second-hit accuracy is reduced rather than increasing target Defence, and its primary target is not struck again by the adjacent-target loop. |
| Abyssal weapons | Removed the bludgeon's unsupported accuracy increase; corrected dagger energy to 25% and linked-hit success to the accuracy result. Unimbued black masks no longer receive imbued Slayer bonuses. |
| Salve | Recognizes all four existing base/enhanced/imbued variants, corrects their percentages, and gives Salve precedence over Slayer. |
| Dragon hunter weapons | Multiplicative target modifiers; crossbow damage is 25% and accuracy 30%. |
| Twisted bow | Uses the higher of target Magic and magic attack bonus, caps its input, retains reductions against low-magic targets, and applies integer-rounded scaling. |
| Tridents | Use current Magic, including drains, and floor damage after bonuses rather than rounding to nearest. |
| Armadyl crossbow | 50% energy; consumes ammunition and routes its doubled proc chance through the existing enchanted-bolt handlers. |
| Magic shortbows | Snapshot uses visible Ranged and ammunition strength, excluding other damage bonuses; accuracy follows the Wiki calculator. |
| Dark bow | Removed the unsupported accuracy boost; dragon-arrow damage caps at 48 after rolling. |
| Bolts | Existing proc handlers accept the Armadyl modifier. Sapphire uses Ranged rather than Prayer for its calculation; dragonstone adds level-based damage and excludes dragon targets. |
| Ancient mace | Crush target defence, prayer restoration on NPCs as well as players, and restoration above the base Prayer level. |
| Dharok/Berserker necklace | Apply their damage scaling after the roll. Dharok uses base HP and missing HP, grants no accuracy bonus, and recognizes the correct degraded helmet IDs. |
| Elysian | Rounds the damage reduction instead of rounding the remaining damage. |
| Tome of fire | Distinct current PvP/PvM damage modifiers and correct last-charge handling. |
| Dragon spear | Prevented a null NPC access when the target is a player. |

References: [special attacks](https://oldschool.runescape.wiki/w/Special_attack), [dragon dagger](https://oldschool.runescape.wiki/w/Dragon_dagger), [abyssal dagger](https://oldschool.runescape.wiki/w/Abyssal_dagger), [Salve comparison](https://oldschool.runescape.wiki/w/Salve_amulet_%28e%29), [twisted bow](https://oldschool.runescape.wiki/w/Twisted_bow), [magic shortbow](https://oldschool.runescape.wiki/w/Magic_shortbow), [dark bow](https://oldschool.runescape.wiki/w/Dark_bow), [toxic blowpipe](https://oldschool.runescape.wiki/w/Toxic_blowpipe), [ancient mace](https://oldschool.runescape.wiki/w/Ancient_mace), [bolts](https://oldschool.runescape.wiki/w/Bolts), and [Tome of Fire](https://oldschool.runescape.wiki/w/Tome_of_Fire).

## Follow-up effects and integration pass

The first pass checked every effective equipment ID's static bonuses, but did not finish every item's executable effects. Those are different checks. The follow-up implements the effects below and adds tests that load real equipment data and invoke production prayer and hit listeners.

| Follow-up area | Behavior implemented or corrected |
| --- | --- |
| Elysian / Justiciar | 70% Elysian activation; additive Justiciar reduction using the attacked style's defence bonus; Justiciar restricted to PvM; minimum-one reduction; poison/dragonfire excluded. Healing and recoil use their separate damage stages. |
| Spectral | NPC attack prayer drain halved, including Cerberus, K'ril, Olm and other existing attack scripts. PvP drain and ordinary active-prayer drain remain separate. |
| Prayer | Real activation/exclusivity affects attack, strength, ranged, magic and defence. Inactive-prayer deactivation no longer subtracts a boost. Prayer-bonus equipment extends duration. Protection prayers distinguish PvP/PvM and attack type. |
| Real armour / weapons | Integration tests change actual robes, plate armour, shields, boots, weapons, amulets and ammunition, recalculate equipment, and inspect resulting accuracy, defence and maximum damage. Negative magic bonuses reach zero accuracy without an invented extra armour penalty. |
| Barrows | Added Ahrim Strength drain, Karil Agility drain and Torag current-run-energy drain; full sets require their working weapons. Verac's PvM proc gains its extra point of damage. |
| Amulet of the damned | Ahrim rolled-damage boost, Karil follow-up hit, Guthan overheal, missing-health Torag defence, Verac prayer bonus and Dharok reflection. These additions do not certify the separate ancient-autocast interface behavior. |
| Dinh's bulwark | Defence-derived offensive strength, five-tick attack speed, block restriction, eight-tick switching delays, PvM blocking reduction, and registered multi-target special with NPC offensive-stat drain. Live target selection/switching scenarios remain to be exercised. |
| Brimstone ring | Magic attacks can reduce the target's defence roll by 10%, with 25% activation. |
| Arclight / Darklight | Corrected demon multipliers, stab-defence special and stat drains; Arclight charges consume on successful ordinary melee attacks and exclude its special. Last-charge conversion cannot go negative. |
| Powered staves | Sanguinesti damage follows current Magic, equipment and prayer rather than fixed 34. Its healing runs on damage completion. Trident/staff charge hooks distinguish powered attacks from holding the weapon; final charges convert correctly. |
| God spells / gauntlets | Chaos gauntlets add to bolt-spell base damage before equipment scaling. Charge requires the matching god cape. Claws of Guthix drains Defence rather than Magic. |
| Bolts | Registered missing opal, jade, pearl and topaz handlers; unenchanted ammunition cannot proc despite sharing enum entries. Added onyx undead exclusion and pre-shield healing amount. Exact jade resistance and pearl affinity identity lists remain review items. |
| Poison / venom | Central successful-hit eligibility and PvP/PvM serpentine probabilities; ordinary poisoned weapons/ammunition apply their initial poison strengths. Tentacle special uses 50% poison chance, eight-tick bind and 25% accuracy. Its ordinary listener no longer adds a second poison roll to the special. |
| Charged shields | Per the private-server rule, dragonfire shield, ward and ancient wyvern shield retain their full configured defence bonuses regardless of stored charges; charge-based defence scaling was reverted at the user's request. Ancient shield no longer absorbs ordinary dragonfire to recharge. Removed a second random roll from ordinary discharge and corrected ancient discharge maximum to 15. Discharge accuracy/protection/freeze details still need reconciliation. |
| Other special attacks | Anchor drains Defence, Attack, Ranged then Magic, including NPCs, from the pre-mitigation roll. Dragon crossbow consumes ammunition and chains decreasing damage, with multi-combat and target-count limits. Ballista special consumes ammunition; dragon thrownaxe schedules the next attack one tick later. Dragon battleaxe drains current levels; dragon 2h includes its primary target and caps PvP targets. |
| Recoil / wilderness | Recoil uses floor(10% damage)+1. Existing wilderness weapons use multiplicative 50% modifiers; Thammaron's obsolete 100% accuracy/25% damage bonus becomes 50%/50%. Its modern powered-staff firing mode is still absent. |

References for this pass: [spirit shields](https://oldschool.runescape.wiki/w/Spirit_shields), [Justiciar](https://oldschool.runescape.wiki/w/Justiciar_armour), [Dinh's bulwark](https://oldschool.runescape.wiki/w/Dinh%27s_bulwark), [Barrows equipment](https://oldschool.runescape.wiki/w/Barrows_equipment), [amulet of the damned](https://oldschool.runescape.wiki/w/Amulet_of_the_damned), [Arclight](https://oldschool.runescape.wiki/w/Arclight), [Darklight](https://oldschool.runescape.wiki/w/Darklight), [Sanguinesti staff](https://oldschool.runescape.wiki/w/Sanguinesti_staff), [Brimstone ring](https://oldschool.runescape.wiki/w/Brimstone_ring), [serpentine helm](https://oldschool.runescape.wiki/w/Serpentine_helm), [venom](https://oldschool.runescape.wiki/w/Venom), [poison](https://oldschool.runescape.wiki/w/Poison), [dragonfire shield](https://oldschool.runescape.wiki/w/Dragonfire_shield), [ancient wyvern shield](https://oldschool.runescape.wiki/w/Ancient_wyvern_shield), [dragonfire ward](https://oldschool.runescape.wiki/w/Dragonfire_ward), [abyssal tentacle](https://oldschool.runescape.wiki/w/Abyssal_tentacle), [anchor](https://oldschool.runescape.wiki/w/Barrelchest_anchor), [dragon crossbow](https://oldschool.runescape.wiki/w/Dragon_crossbow), [dragon battleaxe](https://oldschool.runescape.wiki/w/Dragon_battleaxe), and [special attacks](https://oldschool.runescape.wiki/w/Special_attack).

## Remaining work before full parity can be claimed

1. **566 equipment identities need manual classification.** These are 196 identity/slot/variant reviews and 370 absent reference IDs. Many are custom collisions or historical variants. Matching static bonuses does not certify executable effects. Scythe of Vitur was previously listed as a missing working-item mechanic; it is not defined as equippable in this build's item overlay, so its absence must not be confused with a broken enabled item.
2. **Unverified effect details remain.** Jade Agility resistance currently uses a linear roll; the Wiki confirms resistance exists but does not establish that formula. Pearl fiery/water staff detection and onyx undead detection use name/ID classifications rather than a complete verified affinity dataset. Spectral odd-point rounding, dragonfire discharge accuracy/protection/freeze, Obsidian modifier ordering, all degraded/ornament variants and every special's launch/impact timing are not certified. Kandarin diary rewards have no progression implementation here; the diary proc boost is absent. New Barrows effects have deterministic eligibility tests, but every damned-amulet combination has not been tested through a complete encounter.
3. **Modern spell/NPC systems remain incomplete.** Elemental weaknesses, standard elemental-spell scaling, split ranged defences, modern Thammaron powered attacks, every NPC stat/immunity/phase and boss prayer exception have not been reconciled. Generic NPC formulas do not validate every boss script. Poison's existing severity/tick progression also needs a separate audit beyond initial application.
4. **Live scenario coverage remains necessary.** Equipment switching between launch/impact, attack range/speed, projectile queues, stacked specials, charge use, boss exceptions, simultaneous deaths, recoil and vengeance need a live PvP/PvM matrix. Automated tests below exercise the production calculation and listeners but do not represent an executed in-client encounter matrix.

Existing custom modifiers are intentionally not silently removed. The server's admin strength adder, custom upgrades, custom equipment, and selected activity exceptions still change combat. The Ruby-bolt cap used for custom player HP events also remains. These must be treated as documented deviations when comparing a custom fight with OSRS.

## Verification and reproduction

Run from the workspace root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-combat.ps1
```

This builds the server, runs the server JUnit suite, and regenerates the equipment audit using the locally downloaded reference. Regression checks cover rounding, stances, player/NPC magic defence, Void, Dharok, fixed special target defence, warhammer miss/current-stat drains, modifier stacking, fractional magic damage, tridents, twisted bow scaling, claw splits, Snapshot and Elysian rounding. A production-loader test reads the real item, weapon and shield overlays and checks representative corrected items and fractional bonuses.

**Validation result: 52 tests passed, zero failures/errors; Gradle build successful.** Thirty-eight combat/data tests and fourteen existing server checks pass. The follow-up includes real prayer activation, armour/weapon/ammunition changes, PvP/PvM protection, shield activation boundaries and full defence independent of stored charges, Barrows equipment requirements, Arclight charge eligibility, god spells, venom eligibility, enchanted-ammunition registration and anchor drain order. Existing Lombok, Kotlin, and Gradle deprecation warnings remain. The equipment ledger also records each registered special handler and its declared energy cost; activation-only handlers can consume energy inside their activation method instead.

The audit is read-only. The separate `scripts/apply-combat-bonus-corrections.ps1` migration consumes a reviewed differences ledger, verifies the old values, and preserves the original item overlay under `.tools/combat-audit/item_info.before-bonus-corrections.json`. Do not run it on an unreviewed or stale downloaded reference.

Changes are in the local `main/` working source. They were not published and the running server was not restarted. The existing workspace rule that keeps the source ignored pending redistribution review still applies.
