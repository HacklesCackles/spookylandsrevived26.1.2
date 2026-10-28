package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.EnumSet;

import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A tamed, following raven lands on its owner's shoulder once the owner has stood
 * still for a few seconds (same idea as parrots). Getting off is handled in RavenEntity.
 */
public class RavenShoulderGoal extends Goal {
    /** Owner has to stand still this long (in goal checks, ~2 ticks each: ~3 seconds). */
    private static final int STILL_CHECKS_NEEDED = 30;
    private static final double START_RANGE = 8.0;
    private static final double LAND_DISTANCE = 1.2;
    private static final int MAX_TICKS = 160;

    private final RavenEntity raven;
    private @Nullable Player owner;
    private @Nullable Vec3 lastOwnerPos;
    private int stillChecks;
    private int ticks;

    public RavenShoulderGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!raven.isTame() || raven.isOrderedToSit() || raven.isWandering() || raven.isOnShoulder() || raven.hasCarriedItem()
                || raven.isBathing() || !raven.canLandOnShoulderAgain()) {
            stillChecks = 0;
            return false;
        }
        Player player = raven.getOwner();
        if (player == null || !RavenEntity.canCarryRaven(player) || raven.distanceToSqr(player) > START_RANGE * START_RANGE) {
            stillChecks = 0;
            lastOwnerPos = null;
            return false;
        }
        Vec3 pos = player.position();
        if (lastOwnerPos != null && pos.distanceToSqr(lastOwnerPos) < 0.01) {
            stillChecks++;
        } else {
            stillChecks = 0;
        }
        lastOwnerPos = pos;
        if (stillChecks < STILL_CHECKS_NEEDED) {
            return false;
        }
        owner = player;
        return true;
    }

    @Override
    public void start() {
        ticks = 0;
        stillChecks = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && ticks < MAX_TICKS && !raven.isOnShoulder() && !raven.isOrderedToSit()
                && !raven.isWandering() && RavenEntity.canCarryRaven(owner) && owner.level() == raven.level();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        ticks++;
        Vec3 shoulder = raven.shoulderPositionOf(owner);
        raven.getLookControl().setLookAt(owner, 10.0F, raven.getMaxHeadXRot());
        if (raven.position().distanceToSqr(shoulder) < LAND_DISTANCE * LAND_DISTANCE) {
            raven.landOnShoulder(owner);
            return;
        }
        if (raven.distanceToSqr(owner) < 9.0) {
            raven.getNavigation().stop();
            raven.getMoveControl().setWantedPosition(shoulder.x, shoulder.y + 0.2, shoulder.z, 0.9);
        } else if (raven.getNavigation().isDone() || ticks % 10 == 0) {
            raven.getNavigation().moveTo(shoulder.x, shoulder.y + 0.5, shoulder.z, 1.0);
        }
    }

    @Override
    public void stop() {
        owner = null;
        lastOwnerPos = null;
        raven.getNavigation().stop();
    }
}
