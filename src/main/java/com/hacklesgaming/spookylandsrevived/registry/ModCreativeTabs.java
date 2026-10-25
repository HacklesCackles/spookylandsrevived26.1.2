package com.hacklesgaming.spookylandsrevived.registry;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The "Spooky Lands Revived" creative tab. It lists every item registered in ModItems,
 * so new items show up here automatically.
 */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SpookyLandsRevived.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.spookylandsrevived"))
                    .icon(() -> new ItemStack(ModItems.RAVEN_SPAWN_EGG.get()))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .displayItems((parameters, output) ->
                            ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());
}
