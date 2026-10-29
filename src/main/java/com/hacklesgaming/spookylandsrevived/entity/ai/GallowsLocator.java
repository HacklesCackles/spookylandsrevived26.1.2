package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hacklesgaming.spookylandsrevived.GallowsInjector;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.jspecify.annotations.Nullable;

/**
 * Finds the village gallows piece (spookylandsrevived:village/plains/village_gallows) near a position,
 * using the structure data saved with the world, so it works for every village style
 * (the biome processors swap the wood, but it's still the same piece).
 */
public final class GallowsLocator {
    private static final String GALLOWS_TEMPLATE = "spookylandsrevived:village/plains/village_gallows";

    private GallowsLocator() {
    }

    /** Bounding box of the closest gallows within range, or null if none. */
    public static @Nullable BoundingBox findNearest(ServerLevel level, BlockPos pos, double range) {
        // Village structures leave references in every chunk they cover, so the
        // raven's own chunk is enough to find a village it's in or right next to.
        Map<Structure, LongSet> references = level.structureManager().getAllStructuresAt(pos);
        if (references.isEmpty()) {
            return null;
        }

        BoundingBox[] best = {null};
        double[] bestDist = {range * range};
        references.forEach((structure, refs) ->
                level.structureManager().fillStartsForStructure(structure, refs, start -> {
                    for (StructurePiece piece : start.getPieces()) {
                        if (!isGallows(piece)) {
                            continue;
                        }
                        BoundingBox box = piece.getBoundingBox();
                        double dist = box.getCenter().distSqr(pos);
                        if (dist < bestDist[0]) {
                            bestDist[0] = dist;
                            best[0] = box;
                        }
                    }
                }));
        return best[0];
    }

    private static boolean isGallows(StructurePiece piece) {
        if (!(piece instanceof PoolElementStructurePiece poolPiece)) {
            return false;
        }
        StructurePoolElement element = poolPiece.getElement();
        if (GallowsInjector.isGallows(element)) {
            return true; // placed this session
        }
        // Loaded from a save: the element is a new object, so match on the template name.
        return element instanceof SinglePoolElement && element.toString().contains(GALLOWS_TEMPLATE);
    }

    /**
     * A spot to sit on the gallows: one of the highest blocks in the piece with two clear
     * blocks above it (the top beam). Returns the block to stand on.
     */
    public static @Nullable BlockPos findPerchSpot(ServerLevel level, BoundingBox box, RandomSource random) {
        List<BlockPos> top = new ArrayList<>();
        int topY = Integer.MIN_VALUE;
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.maxY(); y >= box.minY(); y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                        continue;
                    }
                    // Highest solid block in this column.
                    if (y >= topY && level.isEmptyBlock(pos.above()) && level.isEmptyBlock(pos.above(2))) {
                        if (y > topY) {
                            top.clear();
                            topY = y;
                        }
                        top.add(pos);
                    }
                    break;
                }
            }
        }
        return top.isEmpty() ? null : top.get(random.nextInt(top.size()));
    }
}
