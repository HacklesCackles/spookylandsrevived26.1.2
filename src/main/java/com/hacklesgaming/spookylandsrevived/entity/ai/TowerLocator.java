package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.Map;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.Nullable;

/**
 * Finds a tower dungeon (spookylandsrevived:tower_dungeon) near a position, using the
 * structure references saved in each chunk. Only looks at chunks that are already loaded,
 * so it never loads or generates terrain.
 */
public final class TowerLocator {
    private static final Identifier TOWER = Identifier.fromNamespaceAndPath(SpookyLandsRevived.MOD_ID, "tower_dungeon");

    private TowerLocator() {
    }

    /** Bounding box of the whole tower closest to pos, within chunkRadius chunks, or null. */
    public static @Nullable BoundingBox findNearest(ServerLevel level, BlockPos pos, int chunkRadius) {
        int centreX = SectionPos.blockToSectionCoord(pos.getX());
        int centreZ = SectionPos.blockToSectionCoord(pos.getZ());
        BoundingBox best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int cx = centreX + dx;
                int cz = centreZ + dz;
                if (!level.hasChunk(cx, cz)) {
                    continue; // not loaded: skip rather than load it
                }
                BlockPos chunkPos = new BlockPos(SectionPos.sectionToBlockCoord(cx), pos.getY(), SectionPos.sectionToBlockCoord(cz));
                Map<Structure, LongSet> references = level.structureManager().getAllStructuresAt(chunkPos);
                for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
                    if (!isTower(level, entry.getKey())) {
                        continue;
                    }
                    BoundingBox[] found = {null};
                    level.structureManager().fillStartsForStructure(entry.getKey(), entry.getValue(),
                            start -> found[0] = start.getBoundingBox());
                    if (found[0] != null) {
                        double dist = found[0].getCenter().distSqr(pos);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = found[0];
                        }
                    }
                }
            }
        }
        return best;
    }

    /**
     * The actual top of the tower: the highest blocks inside its structure box (the box itself
     * can be much bigger than the tower you see). x/z is the middle of those top blocks,
     * topY the height of the first air above them, halfSpan how far they spread from the middle.
     */
    public record TowerTop(double x, double z, int topY, double halfSpan) {}

    public static @Nullable TowerTop findTop(ServerLevel level, BoundingBox box) {
        int bestY = Integer.MIN_VALUE;
        int minX = 0, maxX = 0, minZ = 0, maxZ = 0;
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (y > bestY) {
                    bestY = y;
                    minX = maxX = x;
                    minZ = maxZ = z;
                } else if (y == bestY) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minZ = Math.min(minZ, z);
                    maxZ = Math.max(maxZ, z);
                }
            }
        }
        if (bestY == Integer.MIN_VALUE) {
            return null;
        }
        double halfSpan = Math.max(maxX - minX + 1, maxZ - minZ + 1) / 2.0;
        return new TowerTop((minX + maxX + 1) / 2.0, (minZ + maxZ + 1) / 2.0, bestY, halfSpan);
    }

    private static boolean isTower(ServerLevel level, Structure structure) {
        Identifier id = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure);
        return TOWER.equals(id);
    }
}
