package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Ravens notice valuables (raven_valuables tag) lying on the ground nearby, swoop down,
 * take one and fly off with it.
 */
public class RavenPickUpValuableGoal extends Goal {
    private static final double SEARCH_RANGE = 10.0;
    private static final double GRAB_DISTANCE = 1.2;
    private static final int MAX_TICKS = 240;
    /** How often (in ticks) it scans for dropped items. */
    private static final int SCAN_INTERVAL = 40;

    private final RavenEntity raven;
    private @Nullable ItemEntity target;
    private int ticks;
    private int scanCooldown;

    public RavenPickUpValuableGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (scanCooldown > 0) {
            scanCooldown--;
            return false;
        }
        scanCooldown = reducedTickDelay(SCAN_INTERVAL);
        if (raven.hasCarriedItem() || raven.isBathing()) {
            return false;
        }
        List<ItemEntity> items = raven.level().getEntitiesOfClass(ItemEntity.class,
                raven.getBoundingBox().inflate(SEARCH_RANGE), this::isGrabbable);
        target = items.stream().min(Comparator.comparingDouble(raven::distanceToSqr)).orElse(null);
        return target != null;
    }

    @Override
    public void start() {
        ticks = 0;
        raven.getNavigation().moveTo(target.getX(), target.getY() + 0.5, target.getZ(), 1.2);
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && ticks < MAX_TICKS && !raven.hasCarriedItem() && isGrabbable(target);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        ticks++;
        if (raven.distanceToSqr(target) < GRAB_DISTANCE * GRAB_DISTANCE) {
            ItemStack stack = target.getItem();
            ItemStack taken = stack.split(1);
            if (stack.isEmpty()) {
                target.discard();
            } else {
                target.setItem(stack);
            }
            raven.setCarriedItem(taken);
            raven.playSound(SoundEvents.ITEM_PICKUP, 0.5F, 1.6F);
            raven.requestFlight();
            return;
        }
        if (raven.distanceToSqr(target) < 9.0) {
            raven.getNavigation().stop();
            raven.getMoveControl().setWantedPosition(target.getX(), target.getY() + 0.2, target.getZ(), 1.0);
        } else if (raven.getNavigation().isDone()) {
            raven.getNavigation().moveTo(target.getX(), target.getY() + 0.5, target.getZ(), 1.2);
        }
    }

    @Override
    public void stop() {
        raven.getNavigation().stop();
        target = null;
    }

    private boolean isGrabbable(ItemEntity item) {
        return item.isAlive() && !item.hasPickUpDelay() && RavenEntity.isValuable(item.getItem())
                && !raven.isScarecrowProtected(item.blockPosition());
    }
}
