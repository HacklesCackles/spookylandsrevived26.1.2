package com.hacklesgaming.spookylandsrevived;

import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;
import com.hacklesgaming.spookylandsrevived.registry.ModEntities;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = SpookyLandsRevived.MOD_ID)
public class ModEvents {
    @SubscribeEvent
    static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.RAVEN.get(), RavenEntity.createAttributes().build());
    }

    @SubscribeEvent
    static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // MOTION_BLOCKING counts leaves, so ravens can spawn on top of trees.
        // Which blocks they spawn on: the spookylandsrevived:raven_spawnable_on block tag.
        event.register(ModEntities.RAVEN.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING, RavenSpawner::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // Creative tab contents live in registry/ModCreativeTabs (the mod's own tab).
}
