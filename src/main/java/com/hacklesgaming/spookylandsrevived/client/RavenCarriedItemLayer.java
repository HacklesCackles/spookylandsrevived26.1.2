package com.hacklesgaming.spookylandsrevived.client;

import java.util.List;

import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import com.geckolib.util.RenderUtil;
import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Draws the raven's carried item (stolen valuable or crop) at the carried_item bone in its beak.
 */
public class RavenCarriedItemLayer<R extends GeoRenderState> extends BlockAndItemGeoLayer<RavenEntity, Void, R> {
    private static final String BONE = "carried_item";

    public RavenCarriedItemLayer(EntityRendererProvider.Context context, GeoRenderer<RavenEntity, Void, R> renderer) {
        super(context, renderer);
    }

    @Override
    protected List<RenderData> getRelevantBones(RavenEntity raven, @Nullable Void relatedObject, R renderState, float partialTick) {
        ItemStack stack = raven.getCarriedItem();
        if (stack.isEmpty()) {
            return List.of();
        }
        // GROUND display context = the small size items have when lying on the floor.
        return List.of(RenderData.item(BONE, ItemDisplayContext.GROUND,
                RenderUtil.createRenderStateForItem(stack, this.itemModelResolver, ItemDisplayContext.GROUND, raven)));
    }

    @Override
    public void addRenderData(RavenEntity raven, @Nullable Void relatedObject, R renderState, float partialTick) {
        List<RenderData> contents = getRelevantBones(raven, relatedObject, renderState, partialTick);
        if (!contents.isEmpty()) {
            renderState.addGeckolibData(CONTENTS, contents);
        }
    }
}
