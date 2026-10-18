# Changelog

## 0.1.1 – 2026-10-18

- About twice as much quartz: new chunks get about 140 blocks of quartz ore instead of 60.
- Existing worlds get quartz too: a chunk generated before the mod gets quartz veins in its stone the
  first time it loads with the mod, once (marked in the chunk, so it never happens again).

## 0.1.0 – 2026-10-18

First release.

- Zoom Goggles I–IV (1.5x to 8x): hold R to zoom with Hytale's own client zoom, the held item hides,
  the mouse slows down and the camera glides.
- Night Vision Goggles I–IV (levels 1 to 5): the world lights up for the wearer alone; `/nightvision`
  switches it on and off.
- UltraGoggles: zoom up to 16x (starting at 12x) and night vision 6.
- Helms with goggles mounted, at the armor bench: Scout, Nightwatch and Overseer helms on the vanilla
  copper, iron, thorium, cobalt and adamantite helmets.
- Optician's Bench with five tiers, each with its own look; it gives off light from tier 3 and its
  grindstone turns while crafting.
- Quartz ore, glass and four lenses; lumenite, a rare ore only Night Vision Goggles IV and the
  UltraGoggles reveal.
- `/goggles` settings page: zoom strength and night vision brightness up to what the goggles allow,
  camera glide, night vision on or off.
- `config.json`: `requireGoggles` (or zoom and night vision for everyone), and per goggles limits that
  can only lower their strength.
