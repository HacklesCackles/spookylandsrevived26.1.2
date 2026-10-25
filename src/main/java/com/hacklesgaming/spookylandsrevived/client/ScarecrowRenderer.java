package com.hacklesgaming.spookylandsrevived.client;

import com.geckolib.renderer.GeoBlockRenderer;
import com.hacklesgaming.spookylandsrevived.block.entity.ScarecrowBlockEntity;
import com.hacklesgaming.spookylandsrevived.registry.ModBlockEntities;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the animated GeckoLib scarecrow from the bottom half's block entity.
 * Uses GeckoLib's default asset paths for "scarecrow" (see ScarecrowBlockEntity) and turns
 * the model to match the block's facing automatically.
 */
public class ScarecrowRenderer extends GeoBlockRenderer<ScarecrowBlockEntity, BlockEntityRenderState> {
    public ScarecrowRenderer(BlockEntityRendererProvider.Context context) {
        super(context, ModBlockEntities.SCARECROW.get());
    }

    /**
     * The model is two blocks tall (plus arms reaching ~1.4 blocks out), but lives in the bottom block.
     * Without this the top half would vanish whenever the bottom block is off screen.
     */
    @Override
    public AABB getRenderBoundingBox(ScarecrowBlockEntity blockEntity) {
        // Arms reach about 1.4 blocks out each side; head tops out just over 2 blocks.
        return new AABB(blockEntity.getBlockPos()).inflate(1.0, 0.0, 1.0).expandTowards(0.0, 1.75, 0.0);
    }
}
