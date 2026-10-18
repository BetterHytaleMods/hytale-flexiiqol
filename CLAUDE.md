# FlexiisGoggles – Arbeitsregeln

Öffentlicher Hytale-Server-Mod (CurseForge, MIT), bis 18.10.2026 „FlexiiQOL“: Zoom und Night Vision
über craftbare Brillen, eine eigene Werkbank (Optician's Bench) und Quarz als neues Erz.

## Regeln

- **Commits auf Englisch, ohne Claude-Zeilen** (kein `Co-Authored-By`, keine Session-Zeile).
- **Öffentliches Repo:** kein dekompilierter Hytale-Code und keine Ausschnitte daraus. **Spiel-Assets
  (Modelle, Texturen) dürfen genutzt werden**: Laut Felix (18.10.) hat Hypixel (Simon) Mod-Erstellern
  das ausdrücklich erlaubt; die Brillenhelme bauen auf den Vanilla-Helmen auf. Keine privaten
  Infrastruktur-Details (AWS, SSM) hier.
- **Produktentscheidungen** (Stärken, Materialien, Namen, Optik, Texte) trifft Felix: fragen statt
  Defaults setzen.
- `zoom/ZoomSpec`, `zoom/ZoomSteps`, `zoom/ZoomRules`, `goggles/Goggles`, `goggles/Effective`,
  `settings/PlayerSettings` und `GogglesConfig` bleiben frei von Hytale-Klassen (testbar ohne Server).
- Hytale hat keine deutsche Sprache. Texte auf Englisch in
  `Server/Languages/en-US/flexiisgoggles.lang` (Schlüssel `flexiisgoggles.<key>`).
- **Grafik nur über `dev/art/Art.java`** (`java dev/art/Art.java` im Repo): Modelle aus Boxen, Texturen
  aus Paletten, Icons daraus gerendert. Die Brillenhelme liest `dev/art/Helms.java` aus dem Vanilla-Helm
  der Stufe in `Assets.zip` (Pfad per `HYTALE_ASSETS` änderbar) und setzt die Brille ein;
  `dev/art/Blocky.java` rendert beliebige Blockymodelle. Danach Server neu starten.

## Stand (18.10.2026)

- Zoom, Night Vision und Einstellungsseite sind im Spiel mit Felix getestet, noch ohne Brillen.
- Die Brillen-Progression ist gebaut: Brillen, Brillenhelme, Werkbank mit 4 Stufen, Quarz, Glas,
  Linsen, Rezepte und Welt-Generierung. Serverseitig ist alles geprüft (alle Assets laden, Quarz entsteht
  in neuen Chunks), **im Spiel noch nicht**. Felix hatte keine Zeit.
- Offen im Spiel:
  - Helme am Kopf (die Brillen sitzen laut Felix ordentlich, Versatz (0, 20, 2) bestätigt).
  - Wie wirken Optik und Icons?
  - Klappen Werkbank-Stufen, Rezepte und Kategorien?
  - Geht das Regler-Maximum (`#Zoom.Max`)?
  - Wirkt `CosmeticsToHide`?
- Noch kein Release, nichts auf AWS.

**Felix' Entscheidungen (18.10.):**
- Komplett umbenannt (Repo, Plugin, `/goggles`). Ohne Brille kein Zoom und kein Night Vision; Schalter
  `requireGoggles`.
- Brillen: Zoom I–IV (1,5/3/5/8×), Night Vision I–IV (1/2/3/5), UltraGoggles (16×, Start 12×, Night
  Vision 6).
  Config kann je Brille nur senken.
- Spielerseite: Regler bis zum Maximum der getragenen Brille, Gleiten frei (0–1,8), Night Vision an/aus.
  Night Vision ist beim Aufsetzen an.
- Brillenhelme: je Brille einer (Scout, Nightwatch, Overseer), Rezept an der Rüstungswerkbank mit
  Vanilla-Helm der passenden Stufe (Kupfer, Eisen, Thorium, Adamantit); Rüstungswerte per `Parent`.
- Zoom fest auf R („Use Ability 3“), keine Tastenwahl im Mod.
- **Quarz nur durch Abbauen des neuen Erzes**, kein Weg über Quarzit. Quarz sieht aus wie Kristall,
  das Erz kristallig (Felix nach dem ersten Blick).
- **Lumenite** (Felix, 18.10.): seltenes Optik-Material, sieht für alle aus wie Stein. Sichtbar nur mit
  Night Vision IV oder Ultra (auch als Helm) bei aktivierter Night Vision. Die UltraGoggles brauchen es
  (3 Stück) und die 5. Bankstufe.
- Werte, Materialien und Namen sind Vorschläge aus dem Plan; Felix prüft sie im Spiel.

## Aufbau

| Teil | Aufgabe |
|---|---|
| `goggles/Goggles` | Alle Brillen mit Höchstwerten, Item-IDs der Brillen und Helme |
| `goggles/WornGoggles` | Jeden Tick: welche Brille (oder welchen Helm) jeder Spieler im Kopf-Slot trägt |
| `goggles/Effective` | Fähigkeit (Brille oder Server) und Spielerwahl → Zoom, Gleiten, Night-Vision-Stufe |
| `GogglesConfig` | `config.json`: `requireGoggles`, Zoom, Night Vision, Grenzen je Brille |
| `settings/PlayerSettings` | Eigene Wahl je Spieler in `players.json` |
| `ui/SettingsPage` + `Common/UI/Custom/FlexiisGoggles/Settings.ui` | Seite `/goggles`, Regler enden beim Brillenmaximum |
| `zoom/*` | R auf die Zoom-Root der Stufe (`Root_FlexiisGoggles_Zoom_<n>x`, Stufen in `ZoomSteps`), Gleiten, Schutz gegen „R auf anderen Spieler“ |
| `nightvision/NightVisionSystem`, `ClientBlocks` | Je Spieler Luft-Licht und Lumenite-Optik in einem `UpdateBlockTypes`, Änderung nach 300 ms Ruhe |
| `command/GogglesCommand`, `NightVisionCommand` | `/goggles` (+ `status`, `reload`), `/nightvision` (`/nv`) |
| `Server/Item/Items/FlexiisGoggles/` | Brillen (über `Flexii_Goggles_Template`), Helme (`Parent` = Vanilla-Helm), Quarz, Glas, Linsen, Erz, Werkbank |
| `Server/WorldGen/Modifier/Flexii_Quartz.json`, `Flexii_Lumenite.json` | Quarz- und Lumenite-Erz in den Höhlen der Zonen 1–3 |
| `dev/art/Art.java` | Alle Modelle, Texturen und Icons |

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
- **Werkbank** = Item mit `BlockType.Bench {Type: Crafting, Id, Categories[], TierLevels[]}` und
  `BlockEntity.Components.BenchBlock`. `TierLevels[n]` = Kosten von Stufe n auf n+1; 4 Einträge, der
  letzte ohne Kosten, ergeben 4 Stufen. Optik je Stufe über `State.Definitions.TierN`. Oberfläche
  generisch, nur Kategorie-Icons nötig. Ohne `HitboxType` gilt ein voller Block.
- **Rezepte** stehen im Item (`Recipe`); `BenchRequirement` mit `Id`, `Categories` (Pflicht, sonst
  unsichtbar) und `RequiredTierLevel`. Material per `ItemId` oder `ResourceTypeId` (`Sands`,
  `Crystal_Shards`, `Wood_Trunk`, `Rock`). Werkbänke selbst: `Workbench`, Kategorie
  `Workbench_Crafting`. Rüstungswerkbank: `Armor_Bench`, Kategorie `Armor_Head`, Stufen 1–3.
- **Kopf-Items:** nur die Slots Kopf, Brust, Hände und Beine; Brille und Helm teilen sich den Kopf
  (Slot 0 in `InventoryComponent.Armor`). Sichtbares hängt an der Item-ID. `Parent` vererbt fast alles
  außer `Quality` und `Recipe`. Items gibt man sich mit `/give <item>`, von der Konsole
  `give <spieler> <item>`.
- **Modelle am Kopf:** Teile hängen relativ zu Kopfknochen (am Hals) plus Versatz des Wurzelknotens
  `Head`. Kopfmitte = (0, 20, 2) (Spielerfigur: Kopf-Box 30×28×28 mit diesem Versatz; im Spiel bestätigt). Vanilla nutzt
  (0, 0, 0), (0, 15, 3) oder (0, 20, 2); die Position des Wurzelknotens zählt nicht. Item- und
  Blockmodelle: 32 Einheiten je Block, ein Barren ist 26 lang.
- **Welt-Generierung:** `Server/WorldGen/Modifier/<Id>.json` mit `Target {Root: Default, Rules: [Glob
  auf Zones.<Zone>.Cave.Caves]}` und `Operations [{Type: Add, Content: {Type: CaveType, PointGrid,
  HeightRange, BlockMask, Generator: {Type: OreCluster}}}]` greift (Test 18.10.). Enums in Codecs in
  Pascal-Schreibweise (`CaveType`, nicht `CAVE_TYPE`). `{Type: Log, ContentType: CaveType}` loggt die
  getroffenen Dateien. `PointGrid.Scale` = Abstand in Blöcken; Scale 3 ergab etwa 60 Quarzblöcke je
  Chunk, Kupfer etwa 370. Lumenite (Scale 14, y 20–75, kleine Cluster): in 2 von 12 Chunks, 7 und 25
  Blöcke. Nur neu erzeugte Chunks; von der Konsole erzeugt `chunk load <x> <z>` einen.
- **Verstecktes Erz:** Der Blocktyp im Spiel sieht aus wie Stein (gleiche drei Texturen, Sounds,
  Partikel wie `Rock_Stone`). Die leuchtende Optik steckt in einem nie platzierten Vorlage-Blocktyp
  (`Flexii_Ore_Lumenite_Revealed`), damit Client und Server Modell und Textur kennen. Für die
  Enthüllung kopiert `ClientBlocks` dessen Optik-Felder (`drawType`, `model`, `modelTexture`,
  `cubeTextures`, `light` …) in eine Kopie des Erz-Pakets und schickt sie nur an den einen Spieler.
- `HytaleServer.SCHEDULED_EXECUTOR` ist ein einzelner Thread für den ganzen Server: nur kurze Aufgaben.
- Die Warnung „Skipping pack at de.hymods_FlexiisGoggles: missing or invalid manifest.json“ beim Start ist
  harmlos: Der Server sucht in `mods/` nach Asset-Packs und findet dort unseren Datenordner.

## Entwicklung

`dev/server.sh start|stop|restart|reload|cmd|logs|status` – lokaler Server in `run/` (git-ignoriert) auf
`127.0.0.1:5521` (`BIND=0.0.0.0 dev/server.sh restart` öffnet ihn fürs Heimnetz), Debugger 5006, damit der GifChat-Testserver (5520/5005) parallel laufen kann. Konsole
über eine FIFO. `reload` (= `plugin reload`) lädt nur Code; Änderungen am Asset-Pack (auch an
`flexiisgoggles.lang`) brauchen `restart`. Felix ist auf dem Testserver Operator (`run/permissions.json`).

**Vor jedem `restart` prüfen, ob Felix verbunden ist** (Log: letztes „joined world“ ohne
„left“/„Disconnecting“). Er testet oft aus dem Heimnetz; ein Neustart wirft ihn raus. Reine
Code-Änderungen über `reload`, das trennt niemanden.

Bildschirmfotos macht Felix selbst (⇧⌘3, `~/Ablage/Screenshots`). Client-Fehler stehen in
`~/Library/Application Support/Hytale/UserData/Logs/*_client.log` („Disconnecting with error …“).
