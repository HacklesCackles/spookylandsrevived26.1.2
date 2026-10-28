package com.hacklesgaming.spookylandsrevived.entity.ai;

import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Random wandering for the raven.
 * On the ground it often just hops/walks a few blocks; otherwise it flies off to a random
 * spot. Never picks a spot above lava or fire, and if it ends up hovering over one it
 * leaves straight away.
 */
public class RavenWanderGoal extends WaterAvoidingRandomFlyingGoal {
    /** Chance that a ground wander is a short walk instead of a flight. */
    private static final float WALK_CHANCE = 0.5F;
    /** Chance a flight heads toward a nearby gallows instead of somewhere random. */
    private static final float GALLOWS_DRIFT_CHANCE = 0.3F;

    private final RavenEntity raven;

    public RavenWanderGoal(RavenEntity raven, double speedModifier) {
        super(raven, speedModifier);
        this.raven = raven;
    }

    @Override
    public boolean canUse() {
        if (!raven.onGround() && raven.getNavigation().isDone() && raven.isHazardBelow(raven.blockPosition())) {
            this.trigger(); // skip the random wait: get away from the lava now
        }
        return super.canUse();
    }

    @Override
    protected @Nullable Vec3 getPosition() {
        // Wander mode: drifted out of range, so head straight back to the home spot.
        BlockPos home = raven.getWanderHome();
        if (home != null && !raven.isWithinWanderRange(raven.position())) {
            return Vec3.atBottomCenterOf(home).add(0.0, 1.0, 0.0);
        }

        // A flock takeoff always means flying, never a walk.
        boolean flockFlight = raven.consumeFlockFlight();

        if (!flockFlight && raven.onGround() && !raven.isInWater() && raven.getRandom().nextFloat() < WALK_CHANCE) {
            Vec3 walk = LandRandomPos.getPos(raven, 4, 2);
            if (isSafe(walk)) {
                return walk;
            }
        }

        // Now and then, drift toward a nearby gallows (at most one hop's worth) and circle over it.
        BoundingBox gallows = raven.getNearbyGallows();
        if (gallows != null && raven.getRandom().nextFloat() < GALLOWS_DRIFT_CHANCE) {
            Vec3 overGallows = Vec3.atCenterOf(gallows.getCenter()).add(
                    raven.getRandom().nextInt(9) - 4, gallows.getYSpan() / 2.0 + 2 + raven.getRandom().nextInt(4),
                    raven.getRandom().nextInt(9) - 4);
            Vec3 offset = overGallows.subtract(raven.position());
            Vec3 target = offset.length() > 12.0 ? raven.position().add(offset.normalize().scale(12.0)) : overGallows;
            if (isSafe(target)) {
                return target;
            }
        }

        // Try a few random flight spots. Alone: take the first safe one.
        // With a flock nearby: take the one closest to the flock, so the group drifts together.
        Vec3 flockCenter = raven.getFlockCenter(RavenEntity.FLOCK_RANGE);
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 5; attempt++) {
            Vec3 fly = super.getPosition();
            if (!isSafe(fly)) {
                continue;
            }
            if (flockCenter == null) {
                return fly;
            }
            double dist = fly.distanceToSqr(flockCenter);
            if (dist < bestDist) {
                best = fly;
                bestDist = dist;
            }
        }
        if (best != null) {
            return best;
        }

        // Nothing safe found nearby. If we're over a hazard, at least climb and move on.
        if (raven.isHazardBelow(raven.blockPosition())) {
            Vec3 look = raven.getViewVector(0.0F);
            return raven.position().add(look.x * 8.0, 4.0, look.z * 8.0);
        }
        return null;
    }

    private boolean isSafe(@Nullable Vec3 pos) {
        return pos != null && !raven.isHazardBelow(BlockPos.containing(pos))
                && !raven.isNearScarecrow(BlockPos.containing(pos))
                && raven.isWithinWanderRange(pos);
    }
}
