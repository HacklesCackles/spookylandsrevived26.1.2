package com.hacklesgaming.spookylandsrevived.registry;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;
import com.hacklesgaming.spookylandsrevived.world.RavenSpawnsBiomeModifier;
import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Biome modifier types (the "type" in data/.../neoforge/biome_modifier/*.json). */
public class ModBiomeModifiers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, SpookyLandsRevived.MOD_ID);

    /** "type": "spookylandsrevived:raven_spawns" */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<RavenSpawnsBiomeModifier>> RAVEN_SPAWNS =
            BIOME_MODIFIER_SERIALIZERS.register("raven_spawns", () -> RavenSpawnsBiomeModifier.CODEC);
}
