package com.hacklesgaming.spookylandsrevived.item;

import java.util.function.Consumer;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * The scarecrow as an item: shows the same animated GeckoLib model as the placed block
 * (in the inventory, in your hand, on the ground and in item frames), gently swaying.
 *
 * It reuses the block's assets:
 *   geckolib/models/block/scarecrow.geo.json
 *   geckolib/animations/block/scarecrow.animation.json
 *   textures/block/scarecrow.png
 * How big it looks and which way it faces in each view is set in models/item/scarecrow.json ("display").
 */
public class ScarecrowItem extends BlockItem implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.scarecrow.idle");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public ScarecrowItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private @Nullable GeoItemRenderer<ScarecrowItem> renderer;

            @Override
            public @Nullable GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GeoItemRenderer<ScarecrowItem>(new DefaultedBlockGeoModel<>(
                            Identifier.fromNamespaceAndPath(SpookyLandsRevived.MOD_ID, "scarecrow")))
                            .useAlternateGuiLighting();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ScarecrowItem>("main", 5, test -> test.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
