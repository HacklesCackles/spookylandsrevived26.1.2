package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.EnumSet;
import java.util.List;

import com.hacklesgaming.spookylandsrevived.block.entity.RavenNestBlockEntity;
import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;
import com.hacklesgaming.spookylandsrevived.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * A raven carrying loot takes it home and drops it in its nest.
 * - Uses its own nest if it has one with room.
 * - Otherwise adopts a nearby nest with room (so a flock ends up sharing).
 * - Otherwise builds a new nest on a treetop or high perch nearby, and tells its
 *   flockmates without a nest that this is now their home too.
 * If it can't build (mobGriefing off, nowhere to build) and has held the item a long
 * time, it loses interest and drops it.
 */
public class RavenStashGoal extends Goal {
    /** Hold the loot this long first so it flies off with it before heading home. */
    private static final int MIN_CARRY_TICKS = 100;
    /** Give up and drop the item after carrying it this long with no nest. */
    private static final int GIVE_UP_CARRY_TICKS = 6000;
    private static final double NEST_SEARCH_RANGE = 32.0;
    private static final double SHARE_RANGE = 16.0;
    private static final int BUILD_RADIUS = 16;
    private static final int BUILD_DOWN = 4;
    private static final int BUILD_UP = 12;
    private static final int BUILD_SAMPLES = 120;
    /** Don't build a new nest closer than this to an existing one. */
    private static final double MIN_NEST_SPACING = 6.0;
    private static final int MAX_TRAVEL_TICKS = 900;
    private static final double HOP_LENGTH = 12.0;

    private final RavenEntity raven;
    /** Nest to drop into, or (when building) where the new nest will be placed. */
    private @Nullable BlockPos target;
    private boolean building;
    private int ticks;
    private int retryCooldown;

    public RavenStashGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (retryCooldown > 0) {
            retryCooldown--;
            return false;
        }
        if (!raven.hasCarriedItem() || raven.getCarriedTicks() < MIN_CARRY_TICKS || raven.isBathing()
                || !(raven.level() instanceof ServerLevel level)) {
            return false;
        }
        retryCooldown = reducedTickDelay(100);

        ItemStack loot = raven.getCarriedItem();
        building = false;
        target = null;

        // 1. Own nest.
        BlockPos home = raven.getHomeNest();
        if (home != null) {
            RavenNestBlockEntity nest = nestAt(level, home);
            if (nest != null && nest.canAccept(loot)) {
                target = home;
                return true;
            }
            if (nest == null) {
                raven.setHomeNest(null); // destroyed
            }
        }

        // 2. Someone else's nest nearby.
        BlockPos nearby = findNearbyNest(level, loot);
        if (nearby != null) {
            raven.setHomeNest(nearby);
            target = nearby;
            return true;
        }

        // 3. Build one.
        if (EventHooks.canEntityGrief(level, raven)) {
            BlockPos spot = findBuildSpot(level);
            if (spot != null) {
                target = spot;
                building = true;
                return true;
            }
        }

        // 4. Nowhere to put it.
        if (raven.getCarriedTicks() > GIVE_UP_CARRY_TICKS) {
            raven.dropCarriedItem(level);
        }
        return false;
    }

    @Override
    public void start() {
        ticks = 0;
        moveTowardTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && raven.hasCarriedItem() && ticks < MAX_TRAVEL_TICKS && !raven.isBathing();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null || !(raven.level() instanceof ServerLevel level)) {
            return;
        }
        ticks++;
        double dx = raven.getX() - (target.getX() + 0.5);
        double dz = raven.getZ() - (target.getZ() + 0.5);
        double horizontalSq = dx * dx + dz * dz;
        double dy = raven.getY() - target.getY();

        if (horizontalSq < 1.0 && dy > -0.5 && dy < 1.5) {
            arrive(level);
            return;
        }
        if (horizontalSq < 9.0 && Math.abs(dy) < 4.0) {
            raven.getNavigation().stop();
            raven.getMoveControl().setWantedPosition(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 0.8);
        } else if (raven.getNavigation().isDone()) {
            if (!moveTowardTarget()) {
                ticks = MAX_TRAVEL_TICKS;
            }
        }
    }

    @Override
    public void stop() {
        raven.getNavigation().stop();
        target = null;
        building = false;
    }

    private void arrive(ServerLevel level) {
        BlockPos pos = target;
        target = null;
        if (pos == null) {
            return;
        }

        if (building) {
            if (!canBuildAt(level, pos)) {
                return; // something changed: try again next time
            }
            level.setBlock(pos, ModBlocks.RAVEN_NEST.get().defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
            raven.setHomeNest(pos);
            shareNestWithFlock(level, pos);
        }

        RavenNestBlockEntity nest = nestAt(level, pos);
        if (nest == null) {
            raven.setHomeNest(null);
            return;
        }
        ItemStack leftover = nest.addItem(raven.getCarriedItem());
        raven.setCarriedItem(leftover);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.4F, 0.8F);
        raven.requestFlight();
    }

    private void shareNestWithFlock(ServerLevel level, BlockPos pos) {
        List<RavenEntity> mates = level.getEntitiesOfClass(RavenEntity.class,
                raven.getBoundingBox().inflate(SHARE_RANGE), other -> other != raven && other.getHomeNest() == null);
        for (RavenEntity mate : mates) {
            mate.setHomeNest(pos);
        }
    }

    private boolean moveTowardTarget() {
        if (target == null) {
            return false;
        }
        Vec3 here = raven.position();
        Vec3 dest = Vec3.atBottomCenterOf(target).add(0.0, 1.0, 0.0);
        Vec3 offset = dest.subtract(here);
        double dist = offset.length();
        if (dist > HOP_LENGTH) {
            Vec3 hop = here.add(offset.scale(HOP_LENGTH / dist));
            dest = new Vec3(hop.x, Math.max(hop.y, Math.min(here.y, dest.y) + 3.0), hop.z);
        }
        return raven.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0);
    }

    private static @Nullable RavenNestBlockEntity nestAt(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof RavenNestBlockEntity nest ? nest : null;
    }

    /** Closest loaded nest within range that has room for this item. */
    private @Nullable BlockPos findNearbyNest(ServerLevel level, ItemStack loot) {
        BlockPos best = null;
        double bestDist = NEST_SEARCH_RANGE * NEST_SEARCH_RANGE;
        // Nests are block entities, so check the block entity lists of nearby loaded chunks
        // instead of scanning every block.
        int chunkRadius = (int) Math.ceil(NEST_SEARCH_RANGE / 16.0);
        int cx = raven.blockPosition().getX() >> 4;
        int cz = raven.blockPosition().getZ() >> 4;
        for (int x = cx - chunkRadius; x <= cx + chunkRadius; x++) {
            for (int z = cz - chunkRadius; z <= cz + chunkRadius; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof RavenNestBlockEntity nest && nest.canAccept(loot)) {
                        double dist = be.getBlockPos().distSqr(raven.blockPosition());
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = be.getBlockPos();
                        }
                    }
                }
            }
        }
        return best;
    }

    private boolean isNearAnyNest(ServerLevel level, BlockPos spot) {
        int cx = spot.getX() >> 4;
        int cz = spot.getZ() >> 4;
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof RavenNestBlockEntity
                            && be.getBlockPos().distSqr(spot) < MIN_NEST_SPACING * MIN_NEST_SPACING) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Highest good spot nearby on top of leaves, logs or a perch block. Returns where the nest goes. */
    private @Nullable BlockPos findBuildSpot(ServerLevel level) {
        RandomSource random = raven.getRandom();
        BlockPos origin = raven.blockPosition();
        BlockPos best = null;
        for (int i = 0; i < BUILD_SAMPLES; i++) {
            BlockPos support = origin.offset(
                    random.nextInt(BUILD_RADIUS * 2 + 1) - BUILD_RADIUS,
                    random.nextInt(BUILD_DOWN + BUILD_UP + 1) - BUILD_DOWN,
                    random.nextInt(BUILD_RADIUS * 2 + 1) - BUILD_RADIUS);
            BlockPos nestPos = support.above();
            if (best != null && nestPos.getY() <= best.getY()) {
                continue;
            }
            if (!isGoodSupport(level.getBlockState(support)) || !canBuildAt(level, nestPos)) {
                continue;
            }
            if (raven.isHazardBelow(nestPos) || isNearAnyNest(level, nestPos) || raven.isScarecrowProtected(nestPos)) {
                continue;
            }
            best = nestPos.immutable();
        }
        return best;
    }

    private static boolean isGoodSupport(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(RavenEntity.PERCHES);
    }

    private static boolean canBuildAt(ServerLevel level, BlockPos nestPos) {
        BlockState nestState = ModBlocks.RAVEN_NEST.get().defaultBlockState();
        return level.isEmptyBlock(nestPos) && level.isEmptyBlock(nestPos.above())
                && nestState.canSurvive(level, nestPos)
                && isGoodSupport(level.getBlockState(nestPos.below()));
    }
}
