package com.hacklesgaming.spookylandsrevived;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * SpookyLandsRevived adds spooky structures (abandoned house, tower dungeon, spider den).
 * All content is data driven: see src/main/resources/data/spookylandsrevived.
 */
@Mod(SpookyLandsRevived.MOD_ID)
public class SpookyLandsRevived {
    public static final String MOD_ID = "spookylandsrevived";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpookyLandsRevived(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}