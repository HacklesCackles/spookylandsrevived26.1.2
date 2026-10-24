package com.hacklesgaming.spookylandsrevived.registry;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;
import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SpookyLandsRevived.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<RavenEntity>> RAVEN =
            ENTITY_TYPES.register("raven", id -> EntityType.Builder
                    .of(RavenEntity::new, MobCategory.AMBIENT)
                    .sized(0.3f, 0.5f)
                    .eyeHeight(0.45f)
                    .clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
}
