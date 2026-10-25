package com.hacklesgaming.spookylandsrevived.compat.jei;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;
import com.hacklesgaming.spookylandsrevived.registry.ModItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * JEI support. JEI finds this class by its @JeiPlugin annotation, and only when JEI is
 * installed, so nothing here runs (or needs JEI) otherwise.
 *
 * Crafting recipes from data/spookylandsrevived/recipe (like the scarecrow) show up in JEI on
 * their own. This adds "information" pages for things JEI can't explain by itself.
 * Texts are in the lang file under jei.spookylandsrevived.*.
 */
@JeiPlugin
public class SpookyJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(SpookyLandsRevived.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(ModItems.SCARECROW.get(),
                Component.translatable("jei.spookylandsrevived.scarecrow"));
        registration.addIngredientInfo(ModItems.RAVEN_NEST.get(),
                Component.translatable("jei.spookylandsrevived.raven_nest"));
        registration.addIngredientInfo(ModItems.RAVEN_SPAWN_EGG.get(),
                Component.translatable("jei.spookylandsrevived.raven"));
    }
}
