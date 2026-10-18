# FlexiisGoggles

Quality of life for Hytale servers (0.7):

- **Zoom:** hold **R** to zoom in, like OptiFine. FlexiisGoggles uses Hytale's own client-side zoom (the one
  the spyglass uses): the view narrows smoothly, the item in your hand goes away, the mouse slows down,
  and the camera glides after it. Letting go zooms back out.
  - R is Hytale's "Use Ability 3"; bind that action to any key under Settings → Controls.
  - R keeps its usual job while you hold a block (rotating it), while the rune in ability slot 3 fits
    your weapon (casting it), and in game modes or with plugins that use R themselves.
- **Fullbright:** caves and nights light up for you alone. Torches, lava and the sun still light up
  more, so the world keeps its depth. Nobody else sees it, and mob spawning and plant growth keep
  using the real light.
- **Settings page:** `/goggles` lets every player pick their zoom strength (2x to 12x), how much the camera
  glides, and fullbright with its brightness (1 to 6). The choices are saved per player.

FlexiisGoggles is a server plugin. Players need nothing but the normal Hytale client.

**Status:** in development, nothing released yet.

## Install

Put `FlexiisGoggles-<version>.jar` into the server's `mods/` folder and restart.

## Settings

`mods/de.hymods_FlexiisGoggles/config.json`, created on the first start, holds what players get until they
pick their own on `/goggles`. `/goggles reload` applies changes without a restart.

```json
{
  "zoom": {
    "enabled": true,
    "magnification": 4,
    "sensitivity": 0.35,
    "inLerp": 0.15,
    "outLerp": 0.25,
    "glide": 0.5
  },
  "fullbright": {
    "enabled": true,
    "level": 3
  }
}
```

| Setting | Meaning |
|---|---|
| `zoom.enabled`, `fullbright.enabled` | Switch a feature off for everyone; it also leaves the settings page |
| `zoom.magnification` | Default zoom strength, 2 to 12 |
| `zoom.sensitivity` | Mouse sensitivity at 4x, as a share of the normal one; other strengths scale it |
| `zoom.inLerp`, `zoom.outLerp` | How fast the view zooms in and out (0.01 slow to 1 instant) |
| `zoom.glide` | Default glide while zoomed, 0 (off) to 1.8 |
| `fullbright.level` | Default fullbright brightness, 1 to 6 |

Each player's own choices are in `players.json` next to it. Switching fullbright on or changing its
brightness makes that player's client rebuild the world it has loaded, which can show as a short
flicker.

## Commands

```
/goggles                   settings page (every player)
/fullbright            switch fullbright on or off for yourself (every player)
/goggles status            settings and state (operators and the console)
/goggles reload            read config.json again (operators and the console)
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
```

- The local server listens on `127.0.0.1:5521`; add it to the client's server list.
  `BIND=0.0.0.0 dev/server.sh restart` opens it to the local network.
- It runs in `authenticated` mode, because the release client refuses the insecure development flow.
  Once per `run/` folder: `dev/server.sh cmd auth persistence Encrypted`, then
  `dev/server.sh cmd auth login device` and confirm the code from the log in the browser.
- Debugger: IntelliJ configuration "Debug dev server" (port 5006).
- `dev/decompile.sh` decompiles the installed server into `~/Hytale-Projects/decompiled/<version>/` for
  reading the API. Decompiled code never goes into this repository.

## License

MIT, see [LICENSE](LICENSE).
