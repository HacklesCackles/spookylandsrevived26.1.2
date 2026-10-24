package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.EnumSet;

import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A tamed raven told to sit stays put: it drops to whatever is below and doesn't move
 * until its owner tells it to follow again.
 */
public class RavenSitGoal extends Goal {
    private final RavenEntity raven;

    public RavenSitGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return raven.isTame() && raven.isOrderedToSit() && !raven.isInWater();
    }

    @Override
    public void start() {
        raven.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        raven.getNavigation().stop();
    }
}
