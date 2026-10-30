# SpookyLandsRevived

A NeoForge mod for Minecraft 26.1.2 (NeoForge 26.1.2.124) that adds spooky structures, ravens and a few
eerie touches to the world. Requires GeckoLib. JEI is optional (adds info pages).

## Structures

| Structure | ID | Notes |
|---|---|---|
| Abandoned house | `abandoned_house` | |
| Abandoned cemetery | `abandoned_cemetery` | |
| Tower dungeon | `tower_dungeon` | Ravens like to gather around it |
| Spider den | `spider_den` | Buried; only the entrance shows |
| Climber's Fall | `climbers_fall` | Ice spikes and other frozen biomes; a frozen adventurer, a hidden powder snow drop and a stray spawner in an ice cave below |
| Village gallows | `village/plains/village_gallows` | Added to plains, desert, savanna, snowy and taiga villages, with a palette matching each style |

All structures are data driven (jigsaw structures, template pools, structure sets and biome tags
under `src/main/resources/data/spookylandsrevived`). The village gallows is added to the vanilla
village house pools when a server starts (`GallowsInjector`).

Climber's Fall has 4 surface layouts and 2 cave layouts that connect at random, so the drop is never
in quite the same spot. Its biome tag includes Biomes O' Plenty's tundra, auroral garden and snowy
fir clearing when that mod is installed.

## Creatures and blocks

- **Ravens**: flock, perch, soar, bathe, steal valuables and ripe crops, and stash loot in raven nests
  on treetops. Give one valuables to tame it.
- **Scarecrow**: keeps ravens away from the area around it (bold ravens excepted).
- **Pet wander mode**: sneak-click a tamed pet (vanilla or modded) to have it wander near a spot
  instead of following you.
- **Cobwebs**: generate on some trees and next to natural pumpkins in new chunks.

## Villager trades

- **Leatherworker (novice)**: always buys a saddle for 1 emerald, on top of its 2 random novice trades.
- **Librarian (master)**: sells any of the 16 dyed candles (3 emeralds each), each color equally likely. Still 2 trades,
  picked from the 14 extra colors added to the vanilla red and yellow (`data/minecraft/tags/villager_trade/librarian/level_5.json`
  and `data/minecraft/trade_set/librarian/level_5.json`).

Guaranteed trades are data files in `data/spookylandsrevived/villager_trade`, listed in the tag
`data/spookylandsrevived/tags/villager_trade/guaranteed/<profession>/level_<n>.json` (1 novice to 5 master).
They are added when a villager reaches that level (`VillagerTradesMixin`), so villagers that already
passed it keep their old trades.

## Configuration

A config file (`spookylandsrevived-common.toml`) is created in the `config` folder. It can also be edited in game from the
Mods screen. Sections, in order:

1. **`pet_modes`**: on/off, vanilla and modded pets, excluded pets, wander radius, sneak requirement.
2. **`ravens`**: spawn rate, flock size, stealing, crop raiding, riding animals, bold chance, soaring.
3. **`tree_cobwebs`** and **`pumpkin_cobwebs`**: on/off and chance.
4. **`structures`**:
   - `gallowsWeight`: spawn weight of the gallows in villages. `0` disables it. Vanilla houses are about 2-4.
   - Per structure (`abandoned_house`, `tower_dungeon`, `spider_den`, `abandoned_cemetery`, `climbers_fall`):
     - `chance`: chance (0.0 to 1.0) that a grid spot actually gets the structure. `-1` uses the data file value.
     - `spacing`: average grid size in chunks. Bigger means rarer. `0` uses the data file value.
     - `separation`: minimum gap in chunks inside a grid cell. Must be smaller than `spacing`. `0` uses the data file value.

Structure and cobweb changes apply to chunks generated after a restart. Structures that were already
generated in an existing world are not changed.

## Building

```
gradlew build
```

The jar is written to `build/libs`. Use `gradlew runClient` to test in a dev client. After renaming or
removing data files, run `gradlew clean build` so old copies don't linger in `build/resources`.

## License

MIT. `TEMPLATE_LICENSE.txt` is the license of the NeoForged MDK template this project started from.
