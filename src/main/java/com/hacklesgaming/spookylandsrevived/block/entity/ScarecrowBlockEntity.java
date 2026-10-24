package com.hacklesgaming.spookylandsrevived.block.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.hacklesgaming.spookylandsrevived.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Marks a scarecrow's position. Keeps a per-world list of loaded scarecrows so ravens can
 * check "is there a scarecrow near here?" without scanning blocks.
 */
public class ScarecrowBlockEntity extends BlockEntity {
    private static final Map<Level, Set<BlockPos>> LOADED = Collections.synchronizedMap(new WeakHashMap<>());

    public ScarecrowBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCARECROW.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            LOADED.computeIfAbsent(level, l -> Collections.synchronizedSet(new HashSet<>())).add(worldPosition.immutable());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            Set<BlockPos> set = LOADED.get(level);
            if (set != null) {
                set.remove(worldPosition);
            }
        }
    }

    /** Closest loaded scarecrow within range of pos (by straight-line distance), or null. */
    public static @Nullable BlockPos findNearest(Level level, BlockPos pos, double range) {
        Set<BlockPos> set = LOADED.get(level);
        if (set == null || set.isEmpty()) {
            return null;
        }
        BlockPos best = null;
        double bestDist = range * range;
        synchronized (set) {
            for (BlockPos scarecrow : set) {
                double dist = scarecrow.distSqr(pos);
                if (dist <= bestDist) {
                    bestDist = dist;
                    best = scarecrow;
                }
            }
        }
        return best;
    }

    public static boolean isNear(Level level, BlockPos pos, double range) {
        return findNearest(level, pos, range) != null;
    }
}
