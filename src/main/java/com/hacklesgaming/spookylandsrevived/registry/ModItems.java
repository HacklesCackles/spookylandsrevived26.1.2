package com.hacklesgaming.spookylandsrevived.registry;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SpookyLandsRevived.MOD_ID);

    public static final DeferredItem<Item> RAVEN_SPAWN_EGG = ITEMS.registerItem("raven_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.RAVEN.get())));

    public static final DeferredItem<BlockItem> RAVEN_NEST = ITEMS.registerSimpleBlockItem(ModBlocks.RAVEN_NEST);
    public static final DeferredItem<BlockItem> SCARECROW = ITEMS.registerSimpleBlockItem(ModBlocks.SCARECROW);
}
