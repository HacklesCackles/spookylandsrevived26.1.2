package com.hacklesgaming.spookylandsrevived;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = "spookylandsrevived")
public class GallowsInjector {

    private static final String MODID = "spookylandsrevived";
    private static final String STRUCTURE = MODID + ":village/plains/gallows";

    // village style -> processor list name
    private static final Map<String, String> STYLES = Map.of(
            "plains", "minecraft:mossify_10_percent",
            "desert", MODID + ":gallows_desert",
            "savanna", MODID + ":gallows_savanna",
            "snowy", MODID + ":gallows_snowy",
            "taiga", MODID + ":gallows_taiga");

    // The gallows pool elements we injected (compared by identity). Read by JigsawPlacerMixin
    // from worldgen threads, so it is built fully and then swapped in.
    private static volatile Set<StructurePoolElement> gallowsElements = Set.of();

    /** True if this pool element is one of our injected gallows. */
    public static boolean isGallows(StructurePoolElement element) {
        return gallowsElements.contains(element);
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Set<StructurePoolElement> injected = Collections.newSetFromMap(new IdentityHashMap<>());
        gallowsElements = injected;

        int weight = Config.GALLOWS_WEIGHT.get();   // 0 = disabled
        if (weight <= 0) return;

        var access = event.getServer().registryAccess();
        Registry<StructureTemplatePool> pools = access.lookupOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processors = access.lookupOrThrow(Registries.PROCESSOR_LIST);

        STYLES.forEach((style, processorName) -> {
            Identifier poolId = Identifier.parse("minecraft:village/" + style + "/houses");
            StructureTemplatePool pool = pools.getOptional(poolId).orElse(null);
            if (pool == null) return;

            Holder<StructureProcessorList> proc = processors.getOrThrow(
                    ResourceKey.create(Registries.PROCESSOR_LIST, Identifier.parse(processorName)));

            StructurePoolElement element = SinglePoolElement
                    .legacy(STRUCTURE, proc)
                    .apply(StructureTemplatePool.Projection.RIGID);

            injected.add(element);

            // weighted lookup list used when picking
            for (int i = 0; i < weight; i++) pool.templates.add(element);
            // raw list is immutable, so copy, add and replace
            List<Pair<StructurePoolElement, Integer>> raw = new ArrayList<>(pool.rawTemplates);
            raw.add(Pair.of(element, weight));
            pool.rawTemplates = raw;
        });
    }
}