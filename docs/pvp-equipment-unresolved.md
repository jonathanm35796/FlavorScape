# PvP Equipment identity review

The supplied list mapped to 181 unique supported IDs: 178 ordinary items now belong to General Market, while three Vesta items belong to the separate Bounty Hunter points store alongside 14 restored base PvP items. This does not cover the entire supplied appearance-based list. Item existence, item identity and the server equipment slot were checked against the loaded local cache and the server item overlay; the selected IDs also exist in RuneLite's generated ID table. This validates the selected items, but cannot establish which source icon an ambiguous description intended.

Sources: [RuneLite item IDs](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/ItemID.java), the local `ItemID.java`, the game's loaded cache, `data/items/item_info.json`, the existing equipment audit, and `docs/Desired Market.png`. The source server's [Shops page](https://impactmmo.wiki/w/index.php?title=Shops) provides the same Melee screenshot but does not identify all the other requested icons. Web lookups did not establish unique identities for the remaining descriptions.

## Explicit interpretations included

- "Dharok's boots": Guardian boots 21733, identified from the available Melee reference. No item is named Dharok's boots in the local cache.
- Three godswords: Armadyl 11802, Bandos 11804 and Saradomin 11806, each 90M points.
- "Obsidian weapon": Tzhaar-ket-om 6528, matching the maul silhouette in the Melee reference.
- Red mystic: dark mystic, using the later 1M prices for the duplicate set. Teal mystic: standard blue mystic at 500K. Green mystic: light mystic at 500K. These colour descriptions are interpretations of the named mystic families, not verified matches to missing source screenshots.
- "Ancient book": equippable Book of darkness 12612. The literally named Ancient book 7633 is a quest item with no equipment slot.
- Blue/black wizard bottoms: Blue skirt 1011 and Black skirt 1015. Blue wizard boots use Wizard boots 2579.
- Amulet of magic variant: Amulet of magic (t) 10366. Amulet of glory variant: six-charge glory 11978. Ring of dueling: fully charged eight-charge version 2552.
- Kebbit fur: common kebbit fur 10121. Cloth material: bolt of cloth 8790. Bellows: Ogre bellows 2871. Fishing-rod variant: fly fishing rod 309.
- Repeated base Green dragonhide listings use 1753 at the final explicit base-item price, 2,250. Colour-described or unspecified hide variants were not guessed.

## Entries without a unique, verified identity

These entries are omitted from the active shop. Their descriptions can match multiple existing items, or the exact named item is absent. Exact names/IDs or the original full category screenshots are needed to finish these mappings.

| Category | Remaining descriptions |
| --- | --- |
| Melee | Abyssal whip variant; unnamed bottom-row armour pieces |
| Ranged | Arrow bundle; coloured arrow variants; blue twisted bow variant; unspecified Armadyl armour piece; unnamed ranged armour set, boots and ammunition; unspecified arrows/bolts |
| Magic | All generic spell/skill/prayer/ability icons; purple mage armour; Ancestral boots icon (no item named Ancestral boots exists); unspecified god capes and variant, ancient staff variant, gold/purple/green/yellow staves; white wizard set and black wizard boots |
| Miscellaneous | Both unspecified pizzas; banner; silver star; blue fish; every potion identified only by colour; teleport icon and scroll; dueling variant; dark blue item; silver ring; coloured amulets; purple fish; fishing tool; white resource; green item |
| Untradeables | All entries: colour-only caps/capes/gloves/helmets/body/backpack, six unnamed crossbows, red/orange/special swords, hook/horn/orb, books, blue pouch, scroll/wands, brown item, purple shard, white potion, purple hexagon and grey emblem. The descriptions do not identify unique cache IDs. |
| Cosmetics | Black/red/green/blue bunny masks (no matching cache names); black cosmetic shirt; brown top/skirt; coloured cosmetic swords/spikes/hats; unspecified gold outfit, weapon, boots and barrels; remaining black/blue cosmetic boots and outfits; rainbow staff; beige item; silver accessory; green cape |
| Resources | Colour-only seeds, herbs, roots, leaves and ingredients; orange resource; purple/raw fish; unspecified hide variants; blue/green metal bars; colour-only potions; unspecified white resources |
| Tools | Silver bowl and white materials; all colour-only pouches and horned helmets; unspecified construction material; spade variants; yellow-handled tool; white resource; brown/grey tools; basic pickaxe variant |

The current cache has ordinary Halloween masks, but that does not establish that "bunny mask" intended a Halloween mask. Likewise, a generic "green item" cannot be resolved by searching valid item IDs. These were not silently substituted.
