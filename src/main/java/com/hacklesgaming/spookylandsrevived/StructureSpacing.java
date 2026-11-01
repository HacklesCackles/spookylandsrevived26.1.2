package com.hacklesgaming.spookylandsrevived;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/**
 * Keeps this mod's structures from generating right on top of each other.
 * Called (through StructureSpacingMixin) whenever the game decides a chunk is a spawn spot for a structure set.
 * If one of our structures listed earlier in Config.STRUCTURES could spawn within minStructureDistance chunks,
 * the later one is skipped. Villages and other mods' structures are never touched.
 */
public final class StructureSpacing {
    // set while we ask about the other structures, so their own checks don't chain into each other
    private static final ThreadLocal<Boolean> CHECKING = ThreadLocal.withInitial(() -> false);

    private StructureSpacing() {}

    public static boolean tooClose(Object placement, ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        if (CHECKING.get()) {
            return false;
        }

        int distance;
        try {
            distance = Config.MIN_STRUCTURE_DISTANCE.get();
        } catch (IllegalStateException e) {
            return false; // config not loaded yet
        }
        if (distance <= 0) {
            return false;
        }

        List<String> order = new ArrayList<>(Config.STRUCTURES.keySet());
        Map<String, Holder<StructureSet>> ours = new HashMap<>();
        int myIndex = -1;

        for (Holder<StructureSet> holder : state.possibleStructureSets()) {
            Optional<ResourceKey<StructureSet>> key = holder.unwrapKey();
            if (key.isEmpty()) {
                continue;
            }
            Identifier id = key.get().identifier();
            if (!id.getNamespace().equals(SpookyLandsRevived.MOD_ID)) {
                continue;
            }
            int index = order.indexOf(id.getPath());
            if (index < 0) {
                continue;
            }
            ours.put(id.getPath(), holder);
            if (holder.value().placement() == placement) {
                myIndex = index;
            }
        }

        // not one of ours, or first in line: nothing can push it out
        if (myIndex <= 0) {
            return false;
        }

        CHECKING.set(true);
        try {
            for (int i = 0; i < myIndex; i++) {
                Holder<StructureSet> other = ours.get(order.get(i));
                if (other != null && state.hasStructureChunkInRange(other, chunkX, chunkZ, distance)) {
                    return true;
                }
            }
        } finally {
            CHECKING.set(false);
        }
        return false;
    }
}
