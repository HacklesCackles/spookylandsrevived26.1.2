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

    static final ModConfigSpec SPEC = BUILDER.build();
}