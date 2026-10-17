# FlexiiQOL – Arbeitsregeln

Öffentlicher Hytale-Server-Mod (CurseForge, MIT): Zoom auf R, Fullbright je Spieler, später
Einstellungsseite und Progression.

## Regeln

- **Commits auf Englisch, ohne Claude-Zeilen** (kein `Co-Authored-By`, keine Session-Zeile).
- **Öffentliches Repo:** kein dekompilierter Hytale-Code, keine kopierten Ausschnitte, keine kopierten
  Vanilla-Assets. Lesen ja, übernehmen nein. Keine privaten Infrastruktur-Details (AWS, SSM) hier.
- **Produktentscheidungen** (Zoomstärke, Klickverhalten, Gleiten, Helligkeit, Texte) trifft Felix:
  fragen statt Defaults setzen.
- `zoom/ZoomSpec`, `light/LightTree` und `QolConfig` bleiben frei von Hytale-Klassen (testbar ohne
  Server). Hytale-Spezifisches gehört in die übrigen Klassen.
- Hytale hat keine deutsche Sprache (Client und Server: en-US, es-ES, fr-FR, pt-BR, ru-RU, uk-UA,
  zh-CN). Texte also auf Englisch in `Server/Languages/en-US/flexiiqol.lang`.

## Hytale 0.7.1: geprüfte API-Fakten

Quelle: dekompilierter Server unter `~/Hytale-Projects/decompiled/0.7.1/` (`dev/decompile.sh`), dazu die
Fakten aus `../hytale-gifchat/CLAUDE.md` (World-Thread, Client-Log, Asset-Pack-Pfade).

- **Zoom:** eingebauter Client-Zoom `Effects.Zoom` (`ZoomConfig`) in einem Interaktionsschritt. Ein
  `Charging`-Schritt mit `AllowIndefiniteHold` und ohne `Next` hält ihn, solange die Taste unten ist.
- **Taste:** Die Komponente `Interactions` am Spieler legt eine Eingabe (`InteractionType`) auf eine
  RootInteraction und gewinnt gegen das gehaltene Item. Sie wird mit dem Spieler gespeichert; fehlt die
  Root später, verwirft das Spiel den Eintrag (`DropUnresolvedInteractions`). R = `Ability3`.
- **R in Vanilla:** leere Hand `UseBlock` → `UseEntity`; mit Item nur eine passende Rune aus
  Fähigkeiten-Slot 3. „Rotate a Block“ ist eine eigene Client-Belegung.
- **`UseEntity`** führt die `Interactions`-Belegung des Ziels im Kontext des Drückenden aus;
  `UseEntityEvent.Pre` ist abbrechbar.
- **Klicks beim Zoomen:** Standardmäßig sperrt jede laufende Eingabe-Kette neue Klicks
  (`InteractionTypeUtils.DEFAULT_INTERACTION_BLOCKED_BY`); `CancelOnOtherClick` lässt einen Klick den
  Zoom beenden.
- **Assets zur Laufzeit:** `RootInteraction.getAssetStore().loadAssetsFromPaths(packKey, paths)` lädt
  eine geschriebene JSON-Datei; der Server schickt sie sofort an alle Clients. `packKey` ist
  `Group:Name` des Plugins (`de.hymods:FlexiiQOL`). `plugin reload` lässt geladene Assets stehen.
- **Server-Kamera:** `SetServerCamera(Custom, true, ServerCameraSettings)`, zurück mit
  `SetServerCamera(Custom, false, null)`. `rotationLerpSpeed < 1` lässt die Kamera nachgleiten.
- **Licht:** `SetChunk.localLight` = 1 Byte „hat Licht“ + Baum (Maskenbyte, 8 Oktanten, Wert 16 Bit LE:
  Rot 0–3, Grün 4–7, Blau 8–11, Himmel 12–15). Der Server teilt ein `CachedPacket` je Abschnitt
  zwischen allen Spielern: nur kopieren. Ausgehende Filter laufen synchron in
  `PacketHandler.writePacket`; die eigene Kopie läuft wieder durch den Filter.
- **Abschnitte neu schicken:** `ChunkTracker.forEachLoadedSection` hält das Lock während des Callbacks,
  das Lock ist nicht reentrant: erst sammeln, dann `removeForReload(x, y, z)`.

## Entwicklung

`dev/server.sh start|stop|restart|reload|cmd|logs|status` – lokaler Server in `run/` (git-ignoriert) auf
`127.0.0.1:5521`, Debugger 5006, damit der GifChat-Testserver (5520/5005) parallel laufen kann. Konsole
über eine FIFO. `reload` (= `plugin reload`) lädt nur Code; Änderungen am Asset-Pack brauchen `restart`.
Felix ist auf dem Testserver Operator (`run/permissions.json`).

Bildschirmfotos macht Felix selbst (⇧⌘3, `~/Ablage/Screenshots`). Client-Fehler stehen in
`~/Library/Application Support/Hytale/UserData/Logs/*_client.log` („Disconnecting with error …“).
