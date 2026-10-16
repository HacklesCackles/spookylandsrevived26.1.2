# SpookyLandsRevived

A NeoForge mod for Minecraft 26.1.2 that adds spooky structures to the world:

- **Abandoned house** (`abandonedhouse_001`)
- **Tower dungeon** (`tower_dungeon001`)
- **Spider den** (`spider_den001`)
- **Abandoned cemetery** (`abandoned_cemetery001`)
- **Village gallows**: a small gallows platform that can appear in plains, desert, savanna, snowy and taiga villages, with a block palette matching each village style

All structures are data driven (jigsaw structures, template pools, structure sets and biome tags
under `src/main/resources/data/spookylandsrevived`). The village gallows is added to the vanilla
village house pools when a server starts (`GallowsInjector`).

## Configuration

A common config file (`spookylandsrevived-common.toml`) is created in the `config` folder. It can also be
edited in game from the Mods screen. Changes take effect after restarting the world or server.

- **`gallowsWeight`**: spawn weight of the gallows in villages. `0` disables it. Vanilla houses are about 2-4.
- **Per structure** (`abandonedhouse_001`, `tower_dungeon001`, `spider_den001`, `abandoned_cemetery001`):
  - `chance`: chance (0.0 to 1.0) that a grid spot actually gets the structure. `-1` uses the data file value.
  - `spacing`: average grid size in chunks. Bigger means rarer. `0` uses the data file value.
  - `separation`: minimum gap in chunks inside a grid cell. Must be smaller than `spacing`. `0` uses the data file value.

Structures that were already generated in an existing world are not changed.

## Building

```
gradlew build
```

The jar is written to `build/libs`. Use `gradlew runClient` to test in a dev client.

## License

MIT. `TEMPLATE_LICENSE.txt` is the license of the NeoForged MDK template this project started from.
