# FlexiiQOL

Quality of life for Hytale servers (0.7):

- **Zoom:** hold **R** to zoom in, like OptiFine. FlexiiQOL uses Hytale's own client-side zoom (the one
  the spyglass uses): the view narrows smoothly, the mouse slows down, and letting go zooms back out. R is
  Hytale's "Use Ability 3"; bind that action to any key under Settings → Controls.
- **Fullbright** (coming next): `/fullbright` lights up caves for you alone. Nobody else sees it, and
  mob spawning and plant growth keep using the real light.

FlexiiQOL is a server plugin. Players need nothing but the normal Hytale client.

**Status:** in development, nothing released yet.

## Install

Put `FlexiiQOL-<version>.jar` into the server's `mods/` folder and restart. Settings live in
`mods/de.hymods_FlexiiQOL/config.json`, which the plugin creates on first start.

## Commands

```
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
