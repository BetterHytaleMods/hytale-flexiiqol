# FlexiiQOL – Arbeitsregeln

Öffentlicher Hytale-Server-Mod (CurseForge, MIT): Zoom auf R, Fullbright je Spieler, später
Einstellungsseite und Progression.

## Regeln

- **Commits auf Englisch, ohne Claude-Zeilen** (kein `Co-Authored-By`, keine Session-Zeile).
- **Öffentliches Repo:** kein dekompilierter Hytale-Code, keine kopierten Ausschnitte, keine kopierten
  Vanilla-Assets. Lesen ja, übernehmen nein. Keine privaten Infrastruktur-Details (AWS, SSM) hier.
- **Produktentscheidungen** (Zoomstärke, Klickverhalten, Gleiten, Helligkeit, Texte) trifft Felix:
  fragen statt Defaults setzen.
- `zoom/ZoomSpec`, `zoom/ZoomRules`, `light/LightTree`, `fullbright/FullbrightPlayers` und `QolConfig`
  bleiben frei von Hytale-Klassen (testbar ohne Server). Hytale-Spezifisches gehört in die übrigen
  Klassen.
- Hytale hat keine deutsche Sprache (Client und Server: en-US, es-ES, fr-FR, pt-BR, ru-RU, uk-UA,
  zh-CN). Texte also auf Englisch in `Server/Languages/en-US/flexiiqol.lang`.

## Stand (17.10.2026)

Zoom und Fullbright sind gebaut und laufen serverseitig. **Im Spiel ist noch nichts getestet**: Felix
hatte keine Zeit; der Machbarkeitstest (Plan Schritt 1a) läuft jetzt gleich mit den fertigen Klassen.
Bis dahin kein Release und nichts auf den AWS-Server.

**Vorläufige Werte, Felix entscheidet im Test:** Zoom 4×, Empfindlichkeit 0,35, `inLerp` 0,15,
`outLerp` 0,25, Ego-Perspektive, keine Vignette, Klick beendet den Zoom, Gleiten 0,7 (nur beim Zoomen),
Fullbright-Stufe 15, die drei Texte in `flexiiqol.lang`.

**Offene Fragen an den Test:**
- Nimmt der Client die zur Laufzeit geladene Root an (`Root_FlexiiQOL_Zoom`)? Sonst
  `/qol spike zoom base` (Root aus dem Jar) zum Vergleich.
- Liegen „Use Ability 3“ und „Rotate a Block“ beide auf R? Falls nicht, kann die Block-Regel weg.
- Verträgt sich die Gleit-Kamera (`SetServerCamera`) mit dem Client-Zoom?
- Zeigt der Client das umgeschriebene Licht? Wird ein Abschnitt nach dem Abbauen dunkel, und reicht
  das Nachschicken (`EditResend`), oder braucht es die Stirnlampe? Ändern Flüssigkeiten das Licht im
  Client (bisher nur Blöcke beobachtet)?

## Aufbau

| Klasse | Aufgabe |
|---|---|
| `zoom/ZoomBindingSystem` | Jeden Tick je Spieler: R auf `Root_FlexiiQOL_Zoom` legen oder freigeben, nach `ZoomRules` |
| `zoom/ZoomRules` | R bleibt beim Spiel bei Block in der Hand, passender Rune in Slot 3, fremder Belegung oder `OverrideAll` (Zuschauer) |
| `zoom/UseEntityGuard` | Bricht `UseEntityEvent.Pre` ab, wenn das Ziel unseren Zoom auf der Taste hat |
| `zoom/ZoomAssets`, `ZoomSpec` | Root aus der Config schreiben und zur Laufzeit laden (`start()`, `/qol reload`) |
| `zoom/Glide`, `GlideSystem` | Server-Kamera mit `rotationLerpSpeed = 1 - glide`, solange eine Zoom-Kette läuft |
| `fullbright/Fullbright` | Fassade: Spielerliste, Filter, Nachschicken |
| `fullbright/FullbrightFilter` | Ausgehende `SetChunk` kopieren und aufhellen; `ServerSetBlock(s)` an `EditResend` melden |
| `fullbright/EditResend` | Nach Blockänderungen: 250 ms Ruhe, warten bis der Server neu belichtet hat (max. 3 s), Abschnitt neu schicken |
| `fullbright/FullbrightPlayers` | `fullbright.json` im Datenordner |
| `command/FullbrightCommand` | `/fullbright` für alle (`requireNoPermission()`) |
| `spike/*` | `/qol spike …` und Paket-Mitschnitt für den Test; fliegt danach raus |

## Hytale 0.7.1: geprüfte API-Fakten

Quelle: dekompilierter Server unter `~/Hytale-Projects/decompiled/0.7.1/` (`dev/decompile.sh`), dazu die
Fakten aus `../hytale-gifchat/CLAUDE.md` (World-Thread, Client-Log, Asset-Pack-Pfade).

- **Zoom:** eingebauter Client-Zoom `Effects.Zoom` (`ZoomConfig`) in einem Interaktionsschritt. Ein
  `Charging`-Schritt mit `AllowIndefiniteHold` und ohne `Next` hält ihn, solange die Taste unten ist.
- **Taste:** Die Komponente `Interactions` am Spieler legt eine Eingabe (`InteractionType`) auf eine
  RootInteraction und gewinnt gegen alles, auch gegen `OverrideAll` (`InteractionContext.getRootInteractionId`).
  Sie wird mit dem Spieler gespeichert und über `InteractionsUpdate` an die Clients geschickt (auch das
  Entfernen). Fehlt die Root später, verwirft das Spiel den Eintrag (`DropUnresolvedInteractions`).
  R = `Ability3`.
- **Wer `Interactions` am Spieler noch nutzt:** der Kreativ-Radierer (`Primary`) und Spielmodi mit
  `InteractionOverrides`/`OverrideAllInteractions` (Zuschauer setzt `OverrideAll`). Beide entfernen nur
  ihre eigenen Einträge und die Komponente erst, wenn sie leer ist; wir halten es genauso.
- **R in Vanilla:** Mit Item in der Hand fragt das Spiel nur Fähigkeiten-Slot 3: passt die Rune
  (`CoreItemAbility.canUseWith`, braucht eine Waffe), wird gezaubert, sonst passiert nichts. Eigene
  `Ability3`-Einträge von Items werden nie benutzt. Leere Hand: `UseBlock` → `UseEntity`; kein
  Vanilla-Block hat eine `Ability3`-Interaktion. Mit Baumodus-Werkzeug (`isUsingToolsItem`) gilt das
  Werkzeug. „Rotate a Block“ ist eine eigene Client-Belegung.
- **`UseEntity`** führt die `Interactions`-Belegung des Ziels im Kontext des Drückenden aus;
  `UseEntityEvent.Pre` ist abbrechbar und wird am Drückenden ausgelöst.
- **Klicks beim Zoomen:** Standardmäßig sperrt jede laufende Eingabe-Kette neue Klicks
  (`InteractionTypeUtils.DEFAULT_INTERACTION_BLOCKED_BY`); `CancelOnOtherClick` lässt einen Klick den
  Zoom beenden.
- **Assets zur Laufzeit:** `RootInteraction.getAssetStore().loadAssetsFromPaths(packKey, paths)` lädt
  eine geschriebene JSON-Datei; der Server schickt sie sofort an alle Clients. `packKey` ist
  `Group:Name` des Plugins (`de.hymods:FlexiiQOL`). Geht schon in `start()`. `plugin reload` lässt
  geladene Assets stehen.
- **Server-Kamera:** `SetServerCamera(Custom, true, ServerCameraSettings)`, zurück mit
  `SetServerCamera(Custom, false, null)`. `rotationLerpSpeed < 1` lässt die Kamera nachgleiten.
- **Licht:** `SetChunk.localLight` = 1 Byte „hat Licht“ + Baum (Maskenbyte, 8 Oktanten, Wert 16 Bit LE:
  Rot 0–3, Grün 4–7, Blau 8–11, Himmel 12–15). Ab Werk schickt der Server lokales Licht, globales nicht
  (`BlockChunk.SEND_LOCAL_LIGHTING_DATA`/`SEND_GLOBAL_LIGHTING_DATA`). Der Server teilt ein
  `CachedPacket` je Abschnitt zwischen allen Spielern: nur kopieren. Ausgehende Filter laufen synchron
  in `PacketHandler.writePacket`; die eigene Kopie läuft wieder durch den Filter.
- **Blockänderungen:** `ServerSetBlock` trägt Blockkoordinaten, `ServerSetBlocks` Abschnittskoordinaten
  (Abschnitt = 32³, `ChunkUtil.chunkCoordinate`); ab 1024 Änderungen geht der ganze Abschnitt als
  `SetChunk` raus. Der Server belichtet danach im Licht-Thread neu (`World.getChunkLighting()`,
  `isQueued(org.joml.Vector3i(x, y, z))`) und schickt kein Licht hinterher.
- **Abschnitte neu schicken:** `ChunkTracker.removeForReload(x, y, z)` in Abschnittskoordinaten.
  `forEachLoadedSection` hält das Lock während des Callbacks, das Lock ist nicht reentrant: erst sammeln,
  dann neu schicken.
- `HytaleServer.SCHEDULED_EXECUTOR` ist ein einzelner Thread für den ganzen Server: nur kurze Aufgaben.
- Die Warnung „Skipping pack at de.hymods_FlexiiQOL: missing or invalid manifest.json“ beim Start ist
  harmlos: Der Server sucht in `mods/` nach Asset-Packs und findet dort unseren Datenordner.

## Entwicklung

`dev/server.sh start|stop|restart|reload|cmd|logs|status` – lokaler Server in `run/` (git-ignoriert) auf
`127.0.0.1:5521`, Debugger 5006, damit der GifChat-Testserver (5520/5005) parallel laufen kann. Konsole
über eine FIFO. `reload` (= `plugin reload`) lädt nur Code; Änderungen am Asset-Pack (auch an
`flexiiqol.lang`) brauchen `restart`. Felix ist auf dem Testserver Operator (`run/permissions.json`).

Bildschirmfotos macht Felix selbst (⇧⌘3, `~/Ablage/Screenshots`). Client-Fehler stehen in
`~/Library/Application Support/Hytale/UserData/Logs/*_client.log` („Disconnecting with error …“).
