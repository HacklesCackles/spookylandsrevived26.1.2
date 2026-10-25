package com.hacklesgaming.spookylandsrevived;

import org.slf4j.Logger;

import com.hacklesgaming.spookylandsrevived.registry.ModBiomeModifiers;
import com.hacklesgaming.spookylandsrevived.registry.ModBlockEntities;
import com.hacklesgaming.spookylandsrevived.registry.ModBlocks;
import com.hacklesgaming.spookylandsrevived.registry.ModCreativeTabs;
import com.hacklesgaming.spookylandsrevived.registry.ModEntities;
import com.hacklesgaming.spookylandsrevived.registry.ModItems;
import com.hacklesgaming.spookylandsrevived.registry.ModSounds;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * SpookyLandsRevived adds spooky structures (abandoned house, tower dungeon, spider den).
 * Structures are data driven: see src/main/resources/data/spookylandsrevived.
 * Creatures (raven) are registered in the registry package.
 */
@Mod(SpookyLandsRevived.MOD_ID)
public class SpookyLandsRevived {
    public static final String MOD_ID = "spookylandsrevived";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpookyLandsRevived(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModBiomeModifiers.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
    }
}