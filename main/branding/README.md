# Llyeta artwork

Created with the built-in imagegen tool on 28 October 2026. The loading artwork is an original scene inspired by the supplied RuneScape courtyard and dark fantasy references, rather than a composite. It contains no text, UI, counters, buttons, watermarks, lava or molten ground.

Source assets: `login-background.png` (1672x941), `app-icon.png` (1254x1254, transparent), and `launcher-wordmark.png` (2172x724, transparent). All three outputs were inspected for composition, spelling and transparency. The wordmark reads **LLYETA**.

Run `powershell -ExecutionPolicy Bypass -File scripts/update-branding.ps1` from the workspace root to regenerate runtime assets. The scene is proportionally scaled and cropped to the native 765x503 login canvas, then split at x=383 into adjoining 383x503 and 382x503 PNGs. The app icon fits a transparent 191x225 canvas; the launcher wordmark fits a transparent 469x167 canvas. Original generated images stay at full resolution here. The background deliberately contains no baked-in title or login controls.

## Background prompt

Use case: stylized-concept. Create a finished original landscape loading-screen background for a RuneScape private server called Llyeta. Reference 1 supplies setting guidance: an Old School RuneScape wilderness hub, low stone enclosure, towering dark gray armored warrior statue, purple magical portal at the right, weathered notice board at the left, stony path and muted grassy hills. Reference 2 supplies dark dramatic cinematic lighting and red-orange accents only. Reimagine this as one coherent original scene, not a collage or screenshot merge. A wide atmospheric hand-painted game illustration retaining angular low-poly OSRS stonework and medieval equipment silhouettes, viewed from a lower three-quarter camera along the path toward the central armored statue and purple portal. Weathered dark rocks and dry earth in the foreground, low stone walls, distant shadowy wilderness crags, subtle red-orange sky light and sparse drifting sparks, with purple portal glow. Keep the central lower third calm and dark for the actual client login/loading panel. Wide 16:9 composition at high resolution. No text, lettering, logos, watermark, borders, buttons, game UI, counters or overlays. No lava, molten ground or glowing cracks; solid stone, dirt and sparse muted grass only.

## Icon prompt

Use case: logo-brand. Transparent application icon for a medieval fantasy RuneScape-style desktop game named Llyeta. A single bold capital L in beveled weathered silver metal, inset into a dark charcoal shield with subtle crimson edging and a small amethyst accent. Clean iconic silhouette, restrained fantasy launcher aesthetic, centered frontal view, legible at small 32px size. No words or other letters, no scenery or watermark. Genuine transparent background, generous transparent margin, square canvas. The only letter is L.

## Wordmark prompt

Use case: logo-brand. Transparent launcher wordmark for an old-school medieval fantasy game. Exact title LLYETA, spelled L-L-Y-E-T-A, in silver beveled fantasy lettering, subtle dark red and black outline, tiny muted violet accents, balanced horizontal composition, restrained weathered metal finish and readable silhouette. Dark gothic RuneScape wilderness aesthetic. Whole word with generous transparent margins on a wide canvas about 3:1. Genuine transparent background. No plaque, scenery, other text, watermark or game UI. Must read LLYETA, not LLETYA.
