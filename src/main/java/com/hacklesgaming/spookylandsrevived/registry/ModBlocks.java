package com.hacklesgaming.spookylandsrevived.registry;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;
import com.hacklesgaming.spookylandsrevived.block.RavenNestBlock;
import com.hacklesgaming.spookylandsrevived.block.ScarecrowBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SpookyLandsRevived.MOD_ID);

    public static final DeferredBlock<RavenNestBlock> RAVEN_NEST = BLOCKS.registerBlock("raven_nest",
            RavenNestBlock::new,
            props -> props
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.5F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .ignitedByLava());

    public static final DeferredBlock<ScarecrowBlock> SCARECROW = BLOCKS.registerBlock("scarecrow",
            ScarecrowBlock::new,
            props -> props
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.8F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY));
}
