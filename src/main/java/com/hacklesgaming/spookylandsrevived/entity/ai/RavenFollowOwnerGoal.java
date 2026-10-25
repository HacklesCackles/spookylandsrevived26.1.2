package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.EnumSet;

import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * A tamed raven that isn't sitting keeps near its owner: flies to them when they get more
 * than a few blocks away, and teleports next to them if they get too far (like wolves).
 */
public class RavenFollowOwnerGoal extends Goal {
    private static final double START_DISTANCE = 7.0;
    private static final double STOP_DISTANCE = 3.0;
    private static final double TELEPORT_DISTANCE = 24.0;

    private final RavenEntity raven;
    private @Nullable Player owner;
    private int repathCooldown;

    public RavenFollowOwnerGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!raven.isTame() || raven.isOrderedToSit()) {
            return false;
        }
        Player player = raven.getOwner();
        if (player == null || player.isSpectator()
                || raven.distanceToSqr(player) < START_DISTANCE * START_DISTANCE) {
            return false;
        }
        owner = player;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && owner.isAlive() && !raven.isOrderedToSit()
                && owner.level() == raven.level()
                && raven.distanceToSqr(owner) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        repathCooldown = 0;
    }

    @Override
    public void stop() {
        owner = null;
        raven.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        raven.getLookControl().setLookAt(owner, 10.0F, raven.getMaxHeadXRot());
        if (--repathCooldown > 0) {
            return;
        }
        repathCooldown = adjustedTickDelay(10);

        if (raven.distanceToSqr(owner) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            teleportNearOwner();
        } else {
            // Aim a little above the owner's head so it flies rather than walks.
            raven.getNavigation().moveTo(owner.getX(), owner.getY() + 2.0, owner.getZ(), 1.2);
        }
    }

    private void teleportNearOwner() {
        BlockPos base = owner.blockPosition();
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos pos = base.offset(
                    raven.getRandom().nextInt(7) - 3,
                    raven.getRandom().nextInt(3),
                    raven.getRandom().nextInt(7) - 3);
            if (Math.abs(pos.getX() - base.getX()) < 2 && Math.abs(pos.getZ() - base.getZ()) < 2) {
                continue; // not right on top of the owner
            }
            if (raven.level().isEmptyBlock(pos) && raven.level().isEmptyBlock(pos.above())
                    && !raven.isHazardBelow(pos)) {
                raven.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, raven.getYRot(), raven.getXRot());
                raven.getNavigation().stop();
                return;
            }
        }
    }
}
