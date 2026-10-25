package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.EnumSet;

import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Keeps ravens loosely together: a raven that has drifted too far from the other ravens
 * around it flies back toward the middle of the group (with some random spread so they
 * don't all stack on one spot).
 */
public class RavenFlockGoal extends Goal {
    /** Closer than this to the flock's middle counts as "with the flock". */
    private static final double REGROUP_DISTANCE = 8.0;
    /** Stop once this close to the chosen spot. */
    private static final double ARRIVE_DISTANCE = 3.0;
    private static final int MAX_TICKS = 200;

    private final RavenEntity raven;
    private @Nullable Vec3 target;
    private int ticks;
    private int checkCooldown;

    public RavenFlockGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Only look every couple of seconds; scanning for entities every tick is wasteful.
        if (checkCooldown > 0) {
            checkCooldown--;
            return false;
        }
        RandomSource random = raven.getRandom();
        checkCooldown = reducedTickDelay(40 + random.nextInt(40));

        if (raven.isInWater() || raven.isTame()) { // tamed ravens stick with their owner instead
            return false;
        }
        Vec3 center = raven.getFlockCenter(RavenEntity.FLOCK_RANGE);
        if (center == null || raven.position().distanceToSqr(center) < REGROUP_DISTANCE * REGROUP_DISTANCE) {
            return false;
        }

        Vec3 spot = center.add(random.nextInt(7) - 3, 1 + random.nextInt(3), random.nextInt(7) - 3);
        if (raven.isHazardBelow(BlockPos.containing(spot))) {
            return false;
        }
        target = spot;
        return true;
    }

    @Override
    public void start() {
        ticks = 0;
        if (target == null || !raven.getNavigation().moveTo(target.x, target.y, target.z, 1.1)) {
            target = null;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return target != null
                && ticks < MAX_TICKS
                && !raven.getNavigation().isDone()
                && raven.position().distanceToSqr(target) > ARRIVE_DISTANCE * ARRIVE_DISTANCE;
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public void stop() {
        raven.getNavigation().stop();
        target = null;
    }
}
