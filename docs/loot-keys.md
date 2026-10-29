# Wilderness loot keys

Implemented 28 October 2026. Skully stands at **(3099,3510,0)**, with **Talk-to** and **Value** options. The **Loot chest** is at **(3095,3506,0)**. Talk to Skully to enable keys for free; they default to disabled. His settings also control whether consumables, stacks above a chosen value, or stacks below a chosen value remain on the ground. Enter zero to disable a value threshold.

With keys enabled, eligible tradeable items lost by a defeated player are collected after the existing items-kept-on-death and item-transformation rules. Each key contains up to 28 of the most valuable item entries; overflow drops normally. Item amounts and attributes, including charges, are preserved. Existing special emblem drops retain their separate handling. Kill-award Blood Money and BH points remain direct rewards. Keys package actual death loot; they do not bypass the five-minute restriction on generated kill rewards.

Players can carry five keys. A generated key drops privately if the inventory is full. Keys cannot be banked, traded or cloned through gear presets. On death, held keys transfer to an eligible killer with keys enabled and capacity available; otherwise those held keys are lost. Ironmen can collect keys but can only destroy their contents.

## Loot window

Use a key on the chest, or choose **Loot** on the chest, to open **Wilderness Loot Keys**. The window uses the supplied reference's key tabs, loot grid, scrollbar and footer controls:

- Select a key tab to view that key's value and contents.
- Click an item to withdraw one, or use the inventory button to withdraw everything that fits.
- **Item / Note** sets the inventory withdrawal form. Items without a valid note, and items carrying attributes, stay unnoted.
- The bank button deposits contents directly into the bank, preserving attributes and using unnoted IDs.
- The trash button asks for confirmation before permanently deleting the selected key and remaining loot.
- The close button and Escape dismiss the window. Wheel scrolling, arrow buttons, track clicks and thumb dragging work in the loot grid.

The key remains when inventory or bank space is insufficient, including when a currency stack reaches its integer limit. Its contents and value decrease only by the quantities actually withdrawn. A full claim can use the slot freed by consuming the key. A key is counted as claimed once its contents have been fully withdrawn; partially claimed value is recorded as it is withdrawn.

Right-click **Value** on Skully shows the saved total of claimed keys, value claimed and value destroyed. Totals use 64-bit accounting. Loot value uses the greater of each item's existing cache value and configured protection value; Blood Money is valued at one per unit. Values are fixed when a key is created, rather than changing while it is held.

## Implementation and verification

Server IDs: Skully **15000**, loot chest **50005**, loot key **30330**. The cache predates modern loot keys, so client definitions reuse its hooded Doomsayer model, bank-key model and Deadman chest model. These are separate definitions and do not change the original NPC, key or chest. The new client plugin uses the cache's native withdrawal/trash sprites and RuneScape fonts. A hidden Barrows-loot modal hosts the window; ordinary Barrows behavior is restored when the loot-key session closes.

The server owns key contents, selected-key identity, bank/inventory writes and statistics. UI actions carry a session nonce and revision. Every action rechecks location, combat/lock state, key ownership and the saved contents. Stale requests, repeat clicks and changed-payload requests cannot withdraw twice. Trash confirmation also revalidates the selected key and session revision.

Restart both the game server and clients to load the new definitions and UI protocol. Do not use this window from an older client. No running account inventory was changed during development. Rendered live-client verification remains separate from the automated transaction tests and offline preview.

The behavior reference is Jagex's [Loot Keys announcement](https://secure.runescape.com/m=news/nex-achievements-and-loot-keys?oldschool=1); the activation price, existing emblem handling and value currency here follow Llyeta's implementation above.

Test logs: `.tools/loot-keys-server-tests.log`, `.tools/loot-keys-client-tests.log`. The optional offline render uses `LLYETA_RENDER_LOOT_KEYS=1` and writes `.tools/loot-key-window-preview.png`. Its fixture icons are downloaded into `.tools/loot-key-ui-assets`; the running client uses its own cache and does not need those downloads.

Final verification: **73 server tests and 16 client tests pass**, with zero failures or skips. Both JAR builds succeed. The cache-based home-layout check passes with 99 NPC placements, 62 object placements and zero access failures, including both requested coordinates. The offline window preview was rendered with native fonts and button sprites and visually inspected against the supplied reference. Tests exercise every footer button, notes, key selection, item withdrawal, scrolling/close, partial inventory/bank capacity, stack limits, stale/forged requests, trash confirmation, persistence, death transfer, preset-cloning rejection and the shared kill-reward cooldown.
