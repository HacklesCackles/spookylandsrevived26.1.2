package com.hacklesgaming.spookylandsrevived;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@EventBusSubscriber(modid = SpookyLandsRevived.MOD_ID)
public class StructureTuner {

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Registry<StructureSet> sets = event.getServer().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);

        Config.STRUCTURES.forEach((name, tuning) -> {
            Identifier id = Identifier.parse(SpookyLandsRevived.MOD_ID + ":" + name);
            StructureSet set = sets.getOptional(id).orElse(null);
            if (set == null) {
                SpookyLandsRevived.LOGGER.warn("StructureTuner: no structure set named {}", id);
                return;
            }

            StructurePlacement placement = set.placement();

            double chance = tuning.chance.get();
            if (chance >= 0) {
                placement.frequency = (float) chance;
            }

            if (placement instanceof RandomSpreadStructurePlacement spread) {
                int spacing = tuning.spacing.get() > 0 ? tuning.spacing.get() : spread.spacing();
                int separation = tuning.separation.get() > 0 ? tuning.separation.get() : spread.separation();
                if (separation >= spacing) {
                    separation = spacing - 1;
                    SpookyLandsRevived.LOGGER.warn("StructureTuner: {} separation must be below spacing, using {}", id, separation);
                }
                spread.spacing = spacing;
                spread.separation = separation;
            }
        });
    }
}