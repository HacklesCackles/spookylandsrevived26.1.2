package com.hacklesgaming.spookylandsrevived.registry;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;
import com.hacklesgaming.spookylandsrevived.block.entity.RavenNestBlockEntity;
import com.hacklesgaming.spookylandsrevived.block.entity.ScarecrowBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SpookyLandsRevived.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RavenNestBlockEntity>> RAVEN_NEST =
            BLOCK_ENTITY_TYPES.register("raven_nest",
                    () -> new BlockEntityType<>(RavenNestBlockEntity::new, ModBlocks.RAVEN_NEST.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScarecrowBlockEntity>> SCARECROW =
            BLOCK_ENTITY_TYPES.register("scarecrow",
                    () -> new BlockEntityType<>(ScarecrowBlockEntity::new, ModBlocks.SCARECROW.get()));
}
