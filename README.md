# FlexiiQOL

Quality of life for Hytale servers (0.7):

- **Zoom:** hold **R** to zoom in, like OptiFine. FlexiiQOL uses Hytale's own client-side zoom (the one
  the spyglass uses): the view narrows smoothly, the mouse slows down, and letting go zooms back out.
  While zoomed, the camera glides after the mouse.
  - R is Hytale's "Use Ability 3"; bind that action to any key under Settings → Controls.
  - R keeps its usual job while you hold a block (rotating it), while the rune in ability slot 3 fits
    your weapon (casting it), and in game modes or with plugins that use R themselves.
- **Fullbright:** `/fullbright` lights up caves for you alone and stays on until you switch it off, also
  after a relog. Nobody else sees it, and mob spawning and plant growth keep using the real light.

FlexiiQOL is a server plugin. Players need nothing but the normal Hytale client.

**Status:** in development, nothing released yet. The zoom and fullbright values are provisional until
they have been tried in game.

## Install

Put `FlexiiQOL-<version>.jar` into the server's `mods/` folder and restart.

## Settings

`mods/de.hymods_FlexiiQOL/config.json`, created on the first start. `/qol reload` applies changes
without a restart.

```json
{
  "zoom": {
    "enabled": true,
    "magnification": 4.0,
    "sensitivity": 0.35,
    "inLerp": 0.15,
    "outLerp": 0.25,
    "glide": 0.7
  },
  "fullbright": {
    "enabled": true,
    "level": 15
  }
}
```

| Setting | Meaning |
|---|---|
| `zoom.magnification` | How many times narrower the view gets (1.25 to 16), relative to each player's own field of view |
| `zoom.sensitivity` | Mouse sensitivity while zoomed, as a share of the normal one (0.05 to 1) |
| `zoom.inLerp`, `zoom.outLerp` | How fast the view zooms in and out (0.01 slow to 1 instant) |
| `zoom.glide` | How much the camera trails the mouse while zoomed (0 off, up to 0.95) |
| `fullbright.level` | The light level fullbright raises every block to (1 to 15); the sky light stays real |

Players with fullbright on get their own copy of every chunk section instead of the shared one, which
costs the server some extra work per player.

## Commands

```
/fullbright            switch fullbright on or off for yourself (every player)
/qol status            settings and state (operators and the console)
/qol reload            read config.json again
/qol spike ...         feasibility test tools, see SpikeCommand
```

## Development

Needs Java 25 and an installed Hytale launcher (the server jar and `Assets.zip` come from there). Maven
comes through the wrapper.

```bash
dev/server.sh start            # build, install the plugin, start a local server (run/)
dev/server.sh reload           # rebuild and reload only the plugin
dev/server.sh cmd <command>    # console command, e.g. dev/server.sh cmd qol status
dev/server.sh logs | status | stop | restart
./mvnw verify                  # tests
```

- The local server listens on `127.0.0.1:5521`; add it to the client's server list.
- It runs in `authenticated` mode, because the release client refuses the insecure development flow.
  Once per `run/` folder: `dev/server.sh cmd auth persistence Encrypted`, then
  `dev/server.sh cmd auth login device` and confirm the code from the log in the browser.
- Debugger: IntelliJ configuration "Debug dev server" (port 5006).
- `dev/decompile.sh` decompiles the installed server into `~/Hytale-Projects/decompiled/<version>/` for
  reading the API. Decompiled code never goes into this repository.

## License

MIT, see [LICENSE](LICENSE).
