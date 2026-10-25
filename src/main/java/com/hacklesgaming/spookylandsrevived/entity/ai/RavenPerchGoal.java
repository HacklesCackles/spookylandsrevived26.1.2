package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.EnumSet;

import com.hacklesgaming.spookylandsrevived.block.entity.ScarecrowBlockEntity;
import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;
import com.hacklesgaming.spookylandsrevived.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Every so often the raven picks a raised block to sit on, flies to it, lands on top and
 * sits for a while.
 * - If a village gallows is nearby it usually heads there and sits on the top beam.
 * - Otherwise it picks a nearby block from the raven_perches tag (fences, walls, leaves,
 *   logs...), higher ones first.
 * - Bold ravens (see RavenEntity#isBold) often go and sit on a scarecrow's head instead.
 */
public class RavenPerchGoal extends Goal {
    private static final int SEARCH_RADIUS = 10;
    private static final int SEARCH_DOWN = 6;
    private static final int SEARCH_UP = 8;
    private static final int SEARCH_SAMPLES = 60;
    private static final int MAX_TRAVEL_TICKS = 240;
    private static final int MAX_GALLOWS_TRAVEL_TICKS = 700;
    /** Roughly how often (in ticks) it tries to go perch. */
    private static final int CHANCE_INTERVAL = 100;
    /** Chance a perch trip goes to the gallows when one is in range. */
    private static final float GALLOWS_CHANCE = 0.6F;
    /** Chance a bold raven's perch trip goes to a nearby scarecrow. */
    private static final float SCARECROW_CHANCE = 0.5F;
    private static final double SCARECROW_RANGE = 24.0;
    /** Pathfinding only reaches ~16 blocks, so longer trips go in hops of this length. */
    private static final double HOP_LENGTH = 12.0;

    private final RavenEntity raven;

    private @Nullable BlockPos perch;
    private boolean gallowsTrip;
    /** Bold ravens only: sitting on top of a scarecrow. */
    private boolean scarecrowTrip;
    private Vec3 seat = Vec3.ZERO;
    private boolean sitting;
    private int travelTicks;
    private int sitTicks;
    private int cooldown;

    public RavenPerchGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (raven.isInWater() || raven.wantsFlockTakeoff()
                || raven.getRandom().nextInt(reducedTickDelay(CHANCE_INTERVAL)) != 0) {
            return false;
        }

        gallowsTrip = false;
        scarecrowTrip = false;
        perch = null;
        // Bold ravens like sitting on scarecrows: head for one now and then.
        if (raven.isBold() && raven.getRandom().nextFloat() < SCARECROW_CHANCE) {
            BlockPos scarecrow = ScarecrowBlockEntity.findNearest(raven.level(), raven.blockPosition(), SCARECROW_RANGE);
            if (scarecrow != null && raven.level().getBlockState(scarecrow.above()).is(ModBlocks.SCARECROW.get())) {
                perch = scarecrow.above(); // the top half (head and shoulders)
                scarecrowTrip = true;
            }
        }
        if (perch == null && raven.getRandom().nextFloat() < GALLOWS_CHANCE && raven.level() instanceof ServerLevel level) {
            BoundingBox gallows = raven.getNearbyGallows();
            if (gallows != null) {
                perch = GallowsLocator.findPerchSpot(level, gallows, raven.getRandom());
                gallowsTrip = perch != null;
            }
        }
        if (perch == null) {
            perch = findPerch();
        }
        return perch != null;
    }

    @Override
    public void start() {
        sitting = false;
        travelTicks = 0;
        Level level = raven.level();
        BlockState state = level.getBlockState(perch);
        double top = state.getCollisionShape(level, perch).max(Direction.Axis.Y);
        seat = new Vec3(perch.getX() + 0.5, perch.getY() + top, perch.getZ() + 0.5);

        if (!moveTowardSeat()) {
            perch = null; // no path, try again later
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (perch == null || !isStillPerch()) {
            return false; // perch broken
        }
        if (raven.wantsFlockTakeoff()) {
            return false; // the flock is leaving, go with it
        }
        if (sitting) {
            return sitTicks > 0;
        }
        return travelTicks < (gallowsTrip ? MAX_GALLOWS_TRAVEL_TICKS : MAX_TRAVEL_TICKS);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (perch == null) {
            return;
        }

        if (sitting) {
            raven.getNavigation().stop();
            sitTicks--;
            // Knocked off or pushed away: give up.
            if (raven.onGround() && !isStandingOnPerch()) {
                sitTicks = 0;
            }
            return;
        }

        travelTicks++;
        double dx = raven.getX() - seat.x;
        double dz = raven.getZ() - seat.z;
        double horizontalSq = dx * dx + dz * dz;
        double dy = raven.getY() - seat.y;

        if (raven.onGround() && horizontalSq < 0.5 && isStandingOnPerch()) {
            // Landed on it.
            sitting = true;
            // Gallows visits last a bit longer: 30-60 seconds. Normal perches 15-45.
            sitTicks = gallowsTrip || scarecrowTrip ? 600 + raven.getRandom().nextInt(600) : 300 + raven.getRandom().nextInt(600);
            raven.setPerchOverride(true);
            raven.getNavigation().stop();
            return;
        }

        if (horizontalSq < 9.0 && Math.abs(dy) < 3.0) {
            // Final approach: fly straight at the spot just above the block, then
            // stop steering once centred so gravity sets it down on top.
            raven.getNavigation().stop();
            if (horizontalSq > 0.04 || dy < 0.0) {
                raven.getMoveControl().setWantedPosition(seat.x, seat.y + 0.25, seat.z, 0.8);
            }
            raven.getLookControl().setLookAt(seat.x, seat.y, seat.z);
        } else if (raven.getNavigation().isDone()) {
            // Path ended (end of a hop, or interrupted): head on.
            if (!moveTowardSeat()) {
                travelTicks = Integer.MAX_VALUE / 2;
            }
        }
    }

    @Override
    public void stop() {
        raven.getNavigation().stop();
        raven.setPerchOverride(false);
        perch = null;
        sitting = false;
        gallowsTrip = false;
        scarecrowTrip = false;
        cooldown = reducedTickDelay(200 + raven.getRandom().nextInt(400));
    }

    /** Path to the seat, or to a point one hop along the way if it's too far for one path. */
    private boolean moveTowardSeat() {
        Vec3 here = raven.position();
        Vec3 target = seat.add(0.0, 0.5, 0.0);
        Vec3 offset = target.subtract(here);
        double dist = offset.length();
        if (dist > HOP_LENGTH) {
            Vec3 hop = here.add(offset.scale(HOP_LENGTH / dist));
            // Keep some height on the way so it flies over things rather than through them.
            target = new Vec3(hop.x, Math.max(hop.y, Math.min(here.y, target.y) + 3.0), hop.z);
        }
        return raven.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
    }

    private boolean isStillPerch() {
        BlockState state = raven.level().getBlockState(perch);
        if (scarecrowTrip) {
            return state.is(ModBlocks.SCARECROW.get());
        }
        // Gallows can be any wood type depending on the village style, so only require "still solid".
        return gallowsTrip ? !state.getCollisionShape(raven.level(), perch).isEmpty() : state.is(RavenEntity.PERCHES);
    }

    private boolean isStandingOnPerch() {
        boolean specificSpot = gallowsTrip || scarecrowTrip;
        return raven.onGround() && (raven.getOnPos().equals(perch) || (!specificSpot && raven.isOnPerch()));
    }

    /** Random-sample the area and keep the highest valid perch. */
    private @Nullable BlockPos findPerch() {
        Level level = raven.level();
        RandomSource random = raven.getRandom();
        BlockPos origin = raven.blockPosition();
        BlockPos best = null;

        for (int i = 0; i < SEARCH_SAMPLES; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS,
                    random.nextInt(SEARCH_DOWN + SEARCH_UP + 1) - SEARCH_DOWN,
                    random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS);

            if (best != null && pos.getY() <= best.getY()) {
                continue;
            }
            if (!level.getBlockState(pos).is(RavenEntity.PERCHES) || raven.isNearScarecrow(pos)) {
                continue;
            }
            // Needs two clear blocks on top to land in.
            if (!level.isEmptyBlock(pos.above()) || !level.isEmptyBlock(pos.above(2))) {
                continue;
            }
            best = pos.immutable();
        }
        return best;
    }
}
