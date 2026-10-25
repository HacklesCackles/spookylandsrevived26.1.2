package com.hacklesgaming.spookylandsrevived;

import com.geckolib.renderer.GeoEntityRenderer;
import com.hacklesgaming.spookylandsrevived.client.RavenCarriedItemLayer;
import com.hacklesgaming.spookylandsrevived.client.RavenNestRenderer;
import com.hacklesgaming.spookylandsrevived.client.ScarecrowRenderer;
import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;
import com.hacklesgaming.spookylandsrevived.registry.ModBlockEntities;
import com.hacklesgaming.spookylandsrevived.registry.ModEntities;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = SpookyLandsRevived.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SpookyLandsRevived.MOD_ID, value = Dist.CLIENT)
public class SpookyLandsRevivedClient {
    public SpookyLandsRevivedClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {

    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Default GeckoLib renderer: looks up geckolib/models/entity/raven.geo.json,
        // geckolib/animations/entity/raven.animation.json and textures/entity/raven.png
        // plus a layer that draws the carried item in its beak.
        event.registerEntityRenderer(ModEntities.RAVEN.get(), SpookyLandsRevivedClient::createRavenRenderer);

        // Nest: draws the top stored item lying in the nest.
        event.registerBlockEntityRenderer(ModBlockEntities.RAVEN_NEST.get(), RavenNestRenderer::new);
        // Scarecrow: the animated GeckoLib model (the block itself is invisible).
        registerScarecrowRenderer(event);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerScarecrowRenderer(EntityRenderersEvent.RegisterRenderers event) {
        BlockEntityRendererProvider provider = ScarecrowRenderer::new;
        event.registerBlockEntityRenderer((BlockEntityType) ModBlockEntities.SCARECROW.get(), provider);
    }

    // Raw types here so this compiles whether or not GeckoLib's interface injection
    // (EntityRenderState -> GeoRenderState) is visible to the compiler.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EntityRenderer<RavenEntity, ?> createRavenRenderer(EntityRendererProvider.Context context) {
        GeoEntityRenderer renderer = new GeoEntityRenderer<>(context, ModEntities.RAVEN.get());
        renderer.withRenderLayer(new RavenCarriedItemLayer(context, renderer));
        return renderer;
    }
}

