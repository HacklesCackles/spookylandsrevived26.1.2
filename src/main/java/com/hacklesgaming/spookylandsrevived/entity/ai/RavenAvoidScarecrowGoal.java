package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.EnumSet;

import com.hacklesgaming.spookylandsrevived.block.entity.ScarecrowBlockEntity;
import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A raven that finds itself close to a scarecrow gets spooked and flies off away from it.
 */
public class RavenAvoidScarecrowGoal extends Goal {
    private static final int CHECK_INTERVAL = 20;
    private static final int MAX_TICKS = 120;

    private final RavenEntity raven;
    private @Nullable Vec3 escape;
    private @Nullable BlockPos scarecrowPos;
    private int ticks;
    private int checkCooldown;

    public RavenAvoidScarecrowGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (checkCooldown > 0) {
            checkCooldown--;
            return false;
        }
        checkCooldown = reducedTickDelay(CHECK_INTERVAL);
        if (raven.isBathing() || raven.isBold()) {
            return false; // bold ravens aren't scared of scarecrows
        }
        BlockPos scarecrow = ScarecrowBlockEntity.findNearest(raven.level(), raven.blockPosition(), RavenEntity.SCARE_RADIUS);
        if (scarecrow == null) {
            return false;
        }
        scarecrowPos = scarecrow;
        Vec3 away = raven.position().subtract(Vec3.atCenterOf(scarecrow)).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(raven.getRandom().nextDouble() - 0.5, 0.0, raven.getRandom().nextDouble() - 0.5);
        }
        escape = raven.position().add(away.normalize().scale(12.0)).add(0.0, 3.0, 0.0);
        return !raven.isHazardBelow(BlockPos.containing(escape));
    }

    @Override
    public void start() {
        ticks = 0;
        raven.getNavigation().moveTo(escape.x, escape.y, escape.z, 1.4);
        // The scarecrow plays its "spook" animation (if its model has one).
        if (scarecrowPos != null && raven.level().getBlockEntity(scarecrowPos) instanceof ScarecrowBlockEntity scarecrow) {
            scarecrow.playSpook();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return escape != null && ticks < MAX_TICKS && !raven.getNavigation().isDone();
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public void stop() {
        escape = null;
        scarecrowPos = null;
    }
}
