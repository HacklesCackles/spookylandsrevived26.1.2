package com.hacklesgaming.spookylandsrevived.client;

import com.hacklesgaming.spookylandsrevived.block.entity.RavenNestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Shows the item on top of the nest's pile (the one you'd get back by right-clicking it),
 * lying flat in the middle of the nest. Draws nothing when the nest is empty.
 */
public class RavenNestRenderer implements BlockEntityRenderer<RavenNestBlockEntity, RavenNestRenderer.State> {
    /** Height of the item above the bottom of the block, in blocks. Tweak to suit the nest model. */
    private static final float ITEM_HEIGHT = 3.5F / 16.0F;
    /** Size of the item. 0.375 is the size campfires use for food. */
    private static final float ITEM_SCALE = 0.375F;

    private final ItemModelResolver itemModelResolver;

    public RavenNestRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RavenNestBlockEntity nest, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(nest, state, partialTick, cameraPos, crumblingOverlay);
        ItemStack top = nest.getTopItem();
        int seed = (int) nest.getBlockPos().asLong();
        // Each nest gets its own fixed random turn, so they don't all look the same.
        state.yaw = Math.floorMod(seed * 31, 360);
        state.item = new ItemStackRenderState();
        if (!top.isEmpty()) {
            itemModelResolver.updateForTopItem(state.item, top, ItemDisplayContext.FIXED, nest.getLevel(), null, seed);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item == null || state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, ITEM_HEIGHT, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F)); // lay it flat
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        @Nullable ItemStackRenderState item;
        float yaw;
    }
}
