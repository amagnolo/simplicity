# Agent Working Notes

This file provides guidance to coding agents when working with code in this repository.

## Project

SimpliCity — a fork of MicropolisJ (Will Wright's SimCity / Micropolis port to Java/Swing) by Jason Long, originally licensed GPLv3 with the EA "no SimCity trademark" addendum (see `README` and `COPYING`). The codebase was recently retargeted: build moved from Ant to Maven (`597b08e`), Java source/target raised to **25** (`843bea3`), and game defaults were tweaked (autoBudget on, disasters off — `721c6ab`). The legacy `INSTALL` / `HACKING` files still describe the old Ant/Java-7 workflow and are out of date — trust `pom.xml`.

## Build & run

Maven project (`pom.xml`), packaging `jar`, main class `micropolisj.Main`.

```bash
mvn package                                  # produces target/SimpliCity-<version>.jar
java -jar target/SimpliCity-*.jar            # run the game
mvn exec:java -Dexec.mainClass=micropolisj.Main   # run without packaging (if exec plugin present locally)
```

There is **no test suite** — `src/test` does not exist and `pom.xml` declares no test dependencies. Don't fabricate `mvn test` workflows; verify changes by running the game.

JDK 25 is required. The `simplicity.iml` file is an IntelliJ module descriptor; the project also opens cleanly as a Maven project in IntelliJ/JetBrains IDEs.

## Auxiliary entry points

These are utility `main()` classes invoked via `java -cp` against the built jar (or `target/classes`):

- `micropolisj.util.TranslationTool` — Swing tool for editing `src/main/resources/i18n/*.properties` locale bundles. See `HACKING` for the localization workflow (still accurate apart from the jar name).
- `micropolisj.build_tool.MakeTiles` — regenerates the composite tile sheet from individual tile PNGs and `graphics/tiles.rc`. Honors `-Dtile_size=`, `-Dskip_tiles=`, `-Dtile_count=` system properties. Resolves per-size source art (`name_NxN.png` / `name.svg`) before falling back to scaling the 16px `name.png`.
- `micropolisj.build_tool.UpscaleArt` — Scale2x/EPX pixel-art upscaler (factor 2 or 4).
- `micropolisj.build_tool.HalveArt` — box-filter downscale by 2 (derives 8px sheets from cleaned 16px art).
- `micropolisj.build_tool.DeDither` — blends the dense 80s two-color dithering on the building/zone sheets into smooth shades (sparse detail like window grids stays crisp).
- `micropolisj.build_tool.GenerateTerrainArt` — draws the modern full-color terrain/forest/park/rubble/flood/radiation/fire art procedurally (see `docs/graphics-roadmap.adoc`).
- `micropolisj.build_tool.GenerateOverlayArt` — draws the modern road/rail/wire/bridge/traffic art procedurally.
- `micropolisj.build_tool.RearrangeTiles` — companion tool for tile reorganization.

Don't run these by hand for routine regeneration — `graphics/build-tiles.sh` orchestrates the whole sheet pipeline (all tile sizes, sprites, tiles.rc sync).

## Architecture

Three layers, kept strictly separated; the engine has no Swing dependency.

### `micropolisj.engine` — simulation core (no UI)

`Micropolis` is the simulation god-object: it owns the tile map (`char[][] map`), per-quadrant overlays at half/quarter resolution (`pollutionMem`, `crimeMem`, `popDensity`, `trfDensity`, `terrainMem`, `landValueMem`), the budget (`CityBudget`), evaluation state (`CityEval`), and active sprites. The front-end drives time by calling `animate()` periodically.

Key concepts:
- **Tiles** are 16-bit `char` values. Static metadata for each tile id lives in `TileSpec` (loaded from `src/main/resources/tiles.rc` at startup) — burnability, conductivity, zone status, animation chain, power/shutdown variants. `TileConstants` exposes the integer IDs and helper predicates.
- **Sprites** (`Sprite` and subclasses: `AirplaneSprite`, `HelicopterSprite`, `ShipSprite`, `TrainSprite`, `MonsterSprite`, `TornadoSprite`, `ExplosionSprite`) are mobile entities overlaid on the tile grid.
- **Tools** (`MicropolisTool`, `ToolStroke`, `BuildingTool`, `RoadLikeTool`, `Bulldozer`) encapsulate user build actions; they produce `ToolResult`/`ToolEffect` so previews and actual mutations share logic.
- **Listeners** (`Micropolis.Listener`, `MapListener`, `EarthquakeListener`) are how the GUI subscribes to engine events — never the other way around.
- Save format is custom binary (see `FILE_FORMAT.txt`); XML import/export goes through `XML_Helper`.

### `micropolisj.gui` — Swing front-end

`MainWindow extends JFrame implements Micropolis.Listener, EarthquakeListener` is the application shell. `MicropolisDrawingArea` renders the tile map; `OverlayMapView` is the minimap with data overlays; `BudgetDialog`, `EvaluationPane`, `GraphsPane`, `MessagesPane`, `NotificationPane`, `DemandIndicator`, `NewCityDialog` are the dialog/panel components. `TileImages` loads the rendered tile sheet produced by `MakeTiles`.

### `micropolisj.graphics`, `micropolisj.util`, `micropolisj.build_tool`

`graphics` holds `TileImage` and `Animation` rendering primitives shared by the GUI and the build-tool. `util` contains the translation tool. `build_tool` contains the offline tile-sheet generators.

## Resources

- `src/main/resources/i18n/` — five `.properties` bundles per locale: `CityMessages`, `CityStrings`, `GuiStrings`, `StatusMessages`, plus `MapGenerator` etc. Locales currently shipped: default (en), `de`, `fr`, `it`, `sv`. Italian was added in `ae11534`.
- `src/main/resources/tiles.rc` — declarative tile definitions consumed by `TileSpec` at runtime; kept in sync with the `MakeTiles` recipe `graphics/tiles.rc` by `graphics/build-tiles.sh`.
- `src/main/resources/{8x8,16x16,32x32,64x64}/` — pre-rendered tile sheets at four zoom levels (zoom range 8–64; `MicropolisDrawingArea` is HiDPI-aware and picks the sheet covering `zoom × device scale`). `sm/` holds the 3px minimap tiles. `classic/{N}x{N}/` holds the original 80s-style art, selectable live via Options → Graphics (`TileSkin` preference).
- `src/main/resources/obj*_{32x32,64x64}.png` — derived sprite size variants (regenerated by the script; don't edit by hand).
- `src/main/resources/sounds/` — game audio.
- `graphics/` (repo root, outside `src/`) — source artwork and the `build-tiles.sh` regeneration script. Derived `graphics/*_NxN.png` upscales are gitignored; committed `_NxN` files are hand-made art the script won't overwrite. See `docs/graphics-roadmap.adoc` for the full pipeline and the art-modernization TODO list.

## Conventions worth knowing

- Engine files preserve the original GPLv3 / EA copyright headers — keep them when editing existing files.
- The simulation runs on the Swing EDT (`Main.main` schedules everything via `SwingUtilities.invokeLater`); engine mutations from background threads are not safe.
- Half-resolution and quarter-resolution overlay arrays in `Micropolis` are indexed `[y/2][x/2]` and `[y/4][x/4]` respectively — read the field-level Javadoc in `Micropolis.java` before touching them.
