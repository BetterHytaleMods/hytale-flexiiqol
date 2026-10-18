# FlexiisGoggles

Craftable goggles for Hytale servers (0.7): zoom like OptiFine and night vision, earned through a new
crafting bench and its own ore.

- **Zoom Goggles I–IV:** hold **R** to zoom in. FlexiisGoggles uses Hytale's own client-side zoom (the
  one the spyglass uses): the view narrows smoothly, the item in your hand goes away, the mouse slows
  down, and the camera glides after it. Letting go zooms back out.
  - R is Hytale's "Use Ability 3"; bind that action to any key under Settings → Controls.
  - R keeps its usual job while you hold a block (rotating it), while the rune in ability slot 3 fits
    your weapon (casting it), and in game modes or with plugins that use R themselves.
- **Night Vision Goggles I–IV:** caves and nights light up for you alone. Torches, lava and the sun
  still light up more, so the world keeps its depth. Nobody else sees it, and mob spawning and plant
  growth keep using the real light.
- **UltraGoggles:** both at full strength. A late-game item that needs lumenite.
- **Helms:** mount goggles on a helmet at the armor bench to wear both: Scout Helms (zoom), Nightwatch
  Helms (night vision), the Overseer Helm (ultra).
- **Optician's Bench:** a cheap bench that upgrades through five tiers; each tier unlocks stronger
  goggles and visibly grows, from a trestle table with a grindstone to a gold-trimmed desk with a
  floating lumenite crystal. Glass and lenses are made there too.
- **Quartz:** a new crystal ore in the stone of zones 1–3 (in newly generated chunks).
- **Lumenite:** a rare crystal deep in the stone that looks like plain stone to everyone. Only Night
  Vision Goggles IV and UltraGoggles (also mounted on a helm) show it, while their night vision is on.
- **Settings page:** `/goggles` lets players pick their zoom strength and night vision brightness, up to
  what their goggles allow, and how much the camera glides. The choices are saved per player.

FlexiisGoggles is a server plugin. Players need nothing but the normal Hytale client.

**Status:** 0.1.0, the first release, for Hytale 0.7 servers (tested with 0.7.2). See the
[changelog](CHANGELOG.md).

## Getting started

1. Craft the **Optician's Bench** at the Workbench: 4 copper bars, 10 logs, 5 rock.
2. Mine **quartz ore** in the stone of zones 1–3; it goes into nearly everything. At the bench, sand and
   quartz make glass, glass and more quartz make lenses, lenses make goggles.
3. Upgrade the bench for stronger goggles: iron, quartz and light leather for tier 2, then thorium, gold
   and crystal shards, then adamantite, void crystals and voidhearts – and more quartz every time.
4. Night Vision Goggles IV show **lumenite**, which looks like plain stone to everyone else. Three of it
   go into the UltraGoggles at bench tier 5.
5. At the armor bench, goggles and a vanilla helmet of their tier make a helm.

## Goggles

| Goggles | Bench tier | Zoom | Night vision |
|---|---|---|---|
| Zoom Goggles I / II / III / IV | 1 / 2 / 3 / 4 | 1.5x / 3x / 5x / 8x | – |
| Night Vision Goggles I / II / III / IV | 1 / 2 / 3 / 4 | – | 1 / 2 / 3 / 5 |
| UltraGoggles | 5 | 16x (starts at 12x) | 6 |

Helms count like the goggles mounted on them. They need a vanilla helmet of the matching tier and keep
its armor values: copper for I, iron for II, thorium or cobalt for III, adamantite for IV and the
UltraGoggles (e.g. Cobalt Scout Helm).

## Install

Put `FlexiisGoggles-<version>.jar` into the server's `mods/` folder and restart.

- Quartz and lumenite ore only appear in chunks generated after the mod was installed; in an existing
  world, explore new land.
- Removing the mod later leaves its blocks (ores, benches) in the world as unknown blocks.

## Settings

`mods/de.hymods_FlexiisGoggles/config.json`, created on the first start. `/goggles reload` applies
changes without a restart.

| Setting | Meaning |
|---|---|
| `requireGoggles` | `true`: zoom and night vision only while wearing goggles. `false`: everyone gets both, up to 16x and brightness 6 |
| `zoom.enabled`, `nightVision.enabled` | Switch a feature off for everyone |
| `goggles.<name>.zoom`, `goggles.<name>.nightVision` | How strong each pair of goggles is on this server; can only be lowered |
| `zoom.magnification`, `nightVision.level` | What players get without goggles (`requireGoggles: false`) until they pick their own |
| `zoom.sensitivity` | Mouse sensitivity at 4x, as a share of the normal one; other strengths scale it |
| `zoom.inLerp`, `zoom.outLerp` | How fast the view zooms in and out (0.01 slow to 1 instant) |
| `zoom.glide` | Default glide while zoomed, 0 (off) to 1.8 |

Each player's own choices are in `players.json` next to it. Switching night vision on or changing its
brightness makes that player's client rebuild the world it has loaded, which can show as a short
flicker.

## Commands

```
/goggles               settings page (every player wearing goggles)
/nightvision, /nv      switch night vision on or off (every player wearing night vision goggles)
/goggles status        settings and state (operators and the console)
/goggles reload        read config.json again (operators and the console)
```

## Development

Needs Java 25 and an installed Hytale launcher (the server jar and `Assets.zip` come from there). Maven
comes through the wrapper.

```bash
dev/server.sh start            # build, install the plugin, start a local server (run/)
dev/server.sh reload           # rebuild and reload only the plugin (not for asset changes)
dev/server.sh cmd <command>    # console command, e.g. dev/server.sh cmd goggles status
dev/server.sh logs | status | stop | restart
./mvnw verify                  # tests
java dev/art/Art.java          # redraw models, textures and icons into src/main/resources/Common
```

- The local server listens on `127.0.0.1:5521`; add it to the client's server list.
  `BIND=0.0.0.0 dev/server.sh restart` opens it to the local network.
- It runs in `authenticated` mode, because the release client refuses the insecure development flow.
  Once per `run/` folder: `dev/server.sh cmd auth persistence Encrypted`, then
  `dev/server.sh cmd auth login device` and confirm the code from the log in the browser.
- Debugger: IntelliJ configuration "Debug dev server" (port 5006).
- `dev/decompile.sh` decompiles the installed server into `~/Hytale-Projects/decompiled/<version>/` for
  reading the API. Decompiled code never goes into this repository.
- All art is drawn by `dev/art/Art.java`. The helms start from the game's own tier helmets (read from
  `Assets.zip`, set `HYTALE_ASSETS` for another path), which Hypixel allows mod creators to use.

## License

MIT, see [LICENSE](LICENSE).
