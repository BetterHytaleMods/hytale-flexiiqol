# FlexiisGoggles – Arbeitsregeln

Öffentlicher Hytale-Server-Mod (CurseForge, MIT): Zoom auf R, Fullbright je Spieler, später
Einstellungsseite und Progression.

## Regeln

- **Commits auf Englisch, ohne Claude-Zeilen** (kein `Co-Authored-By`, keine Session-Zeile).
- **Öffentliches Repo:** kein dekompilierter Hytale-Code, keine kopierten Ausschnitte, keine kopierten
  Vanilla-Assets. Lesen ja, übernehmen nein. Keine privaten Infrastruktur-Details (AWS, SSM) hier.
- **Produktentscheidungen** (Zoomstärke, Klickverhalten, Gleiten, Helligkeit, Texte) trifft Felix:
  fragen statt Defaults setzen.
- `zoom/ZoomSpec`, `zoom/ZoomRules`, `settings/PlayerSettings` und `GogglesConfig`
  bleiben frei von Hytale-Klassen (testbar ohne Server). Hytale-Spezifisches gehört in die übrigen
  Klassen.
- Hytale hat keine deutsche Sprache (Client und Server: en-US, es-ES, fr-FR, pt-BR, ru-RU, uk-UA,
  zh-CN). Texte also auf Englisch in `Server/Languages/en-US/flexiisgoggles.lang`.

## Stand (18.10.2026)

Im Spiel mit Felix getestet (Client und Testserver 0.7.2), alles läuft. Noch kein Release, nichts auf
AWS. **Nächstes Vorhaben:** Brillen-Progression („FlexiisGoggles“), wird gerade geplant.

**Felix' Entscheidungen (18.10.):**
- Zoom nur auf R („Use Ability 3“), keine Tastenwahl im Mod; eine andere Taste legt jeder in Hytales
  Steuerung um. Ausnahmen wie gehabt (Block in der Hand, passende Rune, fremde Belegung).
- Beim Zoomen Ego-Perspektive, Item in der Hand ausgeblendet, keine Vignette, Klick beendet den Zoom.
- Einstellungsseite `/goggles` für jeden Spieler: Zoomstärke 2–12× (ab Werk 4×, Schritte 1), Gleiten aus
  bis 1,8 (ab Werk 0,5), Fullbright an/aus (ab Werk aus) mit Helligkeit 1–6 (ab Werk 3).
- Fullbright über das Licht der Luft (siehe API-Fakten), Radius 0. Die Lichtdaten der Chunks zu
  ändern wirkt im Client nicht.

## Aufbau

| Klasse | Aufgabe |
|---|---|
| `settings/PlayerSettings` | Eigene Wahl je Spieler in `players.json`; nicht Gewähltes folgt der Config |
| `ui/SettingsPage` + `Common/UI/Custom/FlexiisGoggles/Settings.ui` | Seite `/goggles`: Schieberegler mit Wertanzeige, Checkbox |
| `zoom/ZoomBindingSystem` | Jeden Tick je Spieler: R auf die Zoom-Root der gewählten Stufe legen oder freigeben, nach `ZoomRules` |
| `zoom/ZoomRules` | R bleibt beim Spiel bei Block in der Hand, passender Rune in Slot 3, fremder Belegung oder `OverrideAll` (Zuschauer) |
| `zoom/UseEntityGuard` | Bricht `UseEntityEvent.Pre` ab, wenn das Ziel unseren Zoom auf der Taste hat |
| `zoom/ZoomAssets`, `ZoomSpec` | Eine Root je Stufe (`Root_FlexiisGoggles_Zoom_<n>x`) schreiben und zur Laufzeit laden (`start()`, `/goggles reload`) |
| `zoom/Glide`, `GlideSystem` | Server-Kamera mit kleinem `rotationLerpSpeed`, solange eine Zoom-Kette läuft |
| `fullbright/FullbrightSystem`, `AirLight` | Jeden Tick je Spieler: hat sein Client das gewünschte Licht der Luft? Änderung nach 300 ms Ruhe schicken |
| `command/GogglesCommand` | `/goggles` öffnet die Seite (für alle); `/goggles status|reload` brauchen die Berechtigung |
| `command/FullbrightCommand` | `/fullbright` schaltet um (für alle) |

## Hytale 0.7.1/0.7.2: geprüfte API-Fakten

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
  `Group:Name` des Plugins (`de.hymods:FlexiisGoggles`). Geht schon in `start()`. `plugin reload` lässt
  geladene Assets stehen.
- **Server-Kamera:** `SetServerCamera(Custom, true, ServerCameraSettings)`, zurück mit
  `SetServerCamera(Custom, false, null)`. `rotationLerpSpeed < 1` lässt die Kamera nachgleiten.
- **Licht rechnet der Client selbst (Test 18.10., Client 0.7.2):** Lokales und globales Licht in
  `SetChunk` umzuschreiben änderte nichts Sichtbares (neu geschickte Abschnitte; einen frischen
  Login damit haben wir nicht getestet). Der Client flutet das Licht aus den
  `Light`-Werten der Blocktypen (wie der Server: `Color` #rgb = Stufe 0–15 je Kanal, −1 je Block).
  Im Shader: Blocklicht × 2 (`StaticLightMultiplier`), max. mit Sonnenlicht; `CubeShadingMode:
  Fullbright` rendert einen Würfel ohne Licht voll hell.
- **Fullbright = Licht der Luft:** `UpdateBlockTypes` (`AddOrUpdate`, nur `Empty`, id 0, mit
  `light = ColorLight(0, L, L, L)`, `updateMapGeometry = true`) nur an den einen Spieler. Der Client
  baut dann die geladene Welt neu (Client-Log „All rendered chunks were discarded“, ~0,5–0,8 s).
  **Radius muss 0 sein:** Blocklichter mit Radius bekommen zusätzlich je ein Punktlicht, mit jeder
  Luftzelle summiert sich das zu grellem Licht (auch mit `#111`). `BlockType.toPacket()` ist gecacht
  und geteilt: nur Kopien (`new protocol.BlockType(other)`) ändern.
- Item in der Hand beim Zoomen ausblenden: `Effects.HideFirstPersonHeldItem` (so auch das Fernrohr).
- **Seiten-UI (Test 18.10.):** Vanilla-Muster aus `EntitySpawnPage` laufen: `Slider` mit
  `Min`/`Max`/`Step`, Wert als Codec.FLOAT über `ValueChanged` und `MouseButtonReleased`,
  Label daneben per `set("#X.TextSpans", Message)`; `$C.@CheckBoxWithLabel #Id` mit
  `"#Id #CheckBox.Value"`; `$C.@BackButton {}`. Übersetzungen im Markup als `%flexiisgoggles.<key>`.
  Ein `AbstractPlayerCommand` kann Unterbefehle haben; mit `requireNoPermission()` gilt das nur für ihn
  selbst, Unterbefehle brauchen weiter ihre Berechtigung (auch von der Konsole aufrufbar).
- `HytaleServer.SCHEDULED_EXECUTOR` ist ein einzelner Thread für den ganzen Server: nur kurze Aufgaben.
- Die Warnung „Skipping pack at de.hymods_FlexiisGoggles: missing or invalid manifest.json“ beim Start ist
  harmlos: Der Server sucht in `mods/` nach Asset-Packs und findet dort unseren Datenordner.

## Entwicklung

`dev/server.sh start|stop|restart|reload|cmd|logs|status` – lokaler Server in `run/` (git-ignoriert) auf
`127.0.0.1:5521` (`BIND=0.0.0.0 dev/server.sh restart` öffnet ihn fürs Heimnetz), Debugger 5006, damit der GifChat-Testserver (5520/5005) parallel laufen kann. Konsole
über eine FIFO. `reload` (= `plugin reload`) lädt nur Code; Änderungen am Asset-Pack (auch an
`flexiisgoggles.lang`) brauchen `restart`. Felix ist auf dem Testserver Operator (`run/permissions.json`).

Bildschirmfotos macht Felix selbst (⇧⌘3, `~/Ablage/Screenshots`). Client-Fehler stehen in
`~/Library/Application Support/Hytale/UserData/Logs/*_client.log` („Disconnecting with error …“).
