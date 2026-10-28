package com.hacklesgaming.spookylandsrevived;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Worldgen features: cobwebs on trees and next to natural pumpkins (see TreeCobwebFeature). */
public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, SpookyLandsRevived.MOD_ID);

    public static final DeferredHolder<Feature<?>, TreeCobwebFeature> TREE_COBWEBS =
            FEATURES.register("tree_cobwebs", () -> new TreeCobwebFeature(NoneFeatureConfiguration.CODEC));
}
