package com.hacklesgaming.spookylandsrevived;

import java.util.LinkedHashMap;
import java.util.Map;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue GALLOWS_WEIGHT = BUILDER
            .comment("Spawn weight of the gallows in villages. 0 disables it. Vanilla houses are about 2-4.")
            .defineInRange("gallowsWeight", 10, 0, 50);

    public static class StructureTuning {
        public final ModConfigSpec.DoubleValue chance;
        public final ModConfigSpec.IntValue spacing;
        public final ModConfigSpec.IntValue separation;

        StructureTuning(String name) {
            BUILDER.push(name);
            chance = BUILDER
                    .comment("Chance (0.0 to 1.0) that a spot in the grid actually gets this structure. -1 = use the value from the data file.")
                    .defineInRange("chance", -1.0, -1.0, 1.0);
            spacing = BUILDER
                    .comment("Average grid size in chunks. Bigger = rarer. 0 = use the value from the data file.")
                    .defineInRange("spacing", 0, 0, 200);
            separation = BUILDER
                    .comment("Minimum gap in chunks inside a grid cell. Must be smaller than spacing. 0 = use the value from the data file.")
                    .defineInRange("separation", 0, 0, 199);
            BUILDER.pop();
        }
    }

    // key = structure_set file name (no .json) in data/spookylandsrevived/worldgen/structure_set/
    public static final Map<String, StructureTuning> STRUCTURES = new LinkedHashMap<>();
    static {
        for (String name : new String[] { "abandonedhouse_001", "tower_dungeon001", "spider_den001", "abandoned_cemetery001" }) {
            STRUCTURES.put(name, new StructureTuning(name));
        }
    }

    // ---- ravens ----
    // All raven settings take effect right away (no restart needed).

    public static final ModConfigSpec.IntValue RAVEN_SPAWN_WEIGHT;
    public static final ModConfigSpec.IntValue RAVEN_MIN_GROUP;
    public static final ModConfigSpec.IntValue RAVEN_MAX_GROUP;
    public static final ModConfigSpec.BooleanValue RAVENS_STEAL_FROM_PLAYERS;
    public static final ModConfigSpec.BooleanValue RAVENS_RAID_CROPS;
    public static final ModConfigSpec.BooleanValue RAVENS_RIDE_MOBS;
    public static final ModConfigSpec.DoubleValue RAVEN_BOLD_CHANCE;
    public static final ModConfigSpec.BooleanValue RAVENS_SOAR;
    public static final ModConfigSpec.IntValue RAVEN_SOAR_HEIGHT;

    static {
        BUILDER.push("ravens");
        RAVEN_SPAWN_WEIGHT = BUILDER
                .comment("How often flocks of ravens spawn near players, in the biomes from the spookylandsrevived:spawns_ravens tag. Each player gets this % chance every 10 seconds (9 = about one flock every 2 minutes). Up to 10 wild ravens near a player. 0 = ravens never spawn naturally.")
                .defineInRange("spawnWeight", 9, 0, 100);
        RAVEN_MIN_GROUP = BUILDER
                .comment("Smallest flock that spawns together.")
                .defineInRange("minGroupSize", 2, 1, 16);
        RAVEN_MAX_GROUP = BUILDER
                .comment("Largest flock that spawns together (raised to minGroupSize if smaller).")
                .defineInRange("maxGroupSize", 4, 1, 16);
        RAVENS_STEAL_FROM_PLAYERS = BUILDER
                .comment("Wild ravens swoop at players and snatch valuables from their inventory.")
                .define("stealFromPlayers", true);
        RAVENS_RAID_CROPS = BUILDER
                .comment("Wild ravens pull up ripe crops. (Also needs the mobGriefing game rule on.)")
                .define("raidCrops", true);
        RAVENS_RIDE_MOBS = BUILDER
                .comment("Wild ravens now and then hitch a ride on animals and villagers (spookylandsrevived:raven_rideable entity tag).")
                .define("rideMobs", true);
        RAVEN_BOLD_CHANCE = BUILDER
                .comment("Chance (0.0 to 1.0) that a raven is bold: not scared of scarecrows, and likes to sit on them. Rolled once per raven. 0.01 = 1 in 100.")
                .defineInRange("boldChance", 0.05, 0.0, 1.0);
        RAVENS_SOAR = BUILDER
                .comment("Wild ravens now and then spiral up high, circle on a glide for a while (flockmates often join in), then spiral back down.")
                .define("soaring", true);
        RAVEN_SOAR_HEIGHT = BUILDER
                .comment("How high above the ground soaring ravens circle, in blocks (each raven varies a little).")
                .defineInRange("soarHeight", 20, 8, 48);
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}