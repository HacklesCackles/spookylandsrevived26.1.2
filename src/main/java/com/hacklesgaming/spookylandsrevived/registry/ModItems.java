package com.hacklesgaming.spookylandsrevived.registry;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;
import com.hacklesgaming.spookylandsrevived.item.ScarecrowItem;

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
    /** Uses the animated GeckoLib scarecrow model as its item look (see ScarecrowItem). */
    public static final DeferredItem<ScarecrowItem> SCARECROW = ITEMS.registerItem("scarecrow",
            props -> new ScarecrowItem(ModBlocks.SCARECROW.get(), props.useBlockDescriptionPrefix()));
}
