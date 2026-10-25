package com.hacklesgaming.spookylandsrevived.world;

import com.hacklesgaming.spookylandsrevived.Config;
import com.hacklesgaming.spookylandsrevived.registry.ModBiomeModifiers;
import com.hacklesgaming.spookylandsrevived.registry.ModEntities;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Adds raven spawns to the given biomes, using the weight and group size from the config
 * (ravens section) instead of fixed numbers in the json. Used by
 * data/spookylandsrevived/neoforge/biome_modifier/raven_spawns.json.
 * Biome modifiers run once when a world loads, so config changes need a world restart.
 */
public record RavenSpawnsBiomeModifier(HolderSet<Biome> biomes) implements BiomeModifier {
    public static final MapCodec<RavenSpawnsBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(builder -> builder
            .group(Biome.LIST_CODEC.fieldOf("biomes").forGetter(RavenSpawnsBiomeModifier::biomes))
            .apply(builder, RavenSpawnsBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !biomes.contains(biome)) {
            return;
        }
        int weight = Config.RAVEN_SPAWN_WEIGHT.get();
        if (weight <= 0) {
            return; // natural spawning switched off
        }
        int min = Config.RAVEN_MIN_GROUP.get();
        int max = Math.max(min, Config.RAVEN_MAX_GROUP.get());
        builder.getMobSpawnSettings().addSpawn(MobCategory.AMBIENT, weight,
                new MobSpawnSettings.SpawnerData(ModEntities.RAVEN.get(), min, max));
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return ModBiomeModifiers.RAVEN_SPAWNS.get();
    }
}
