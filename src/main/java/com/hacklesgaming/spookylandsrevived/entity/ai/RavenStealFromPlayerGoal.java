package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Once in a while a raven swoops at a nearby player who has valuables (raven_valuables tag)
 * in their inventory, snatches one item and flies off with it. Hitting the raven makes it drop it.
 * Ignores players in creative or spectator mode.
 */
public class RavenStealFromPlayerGoal extends Goal {
    private static final double TARGET_RANGE = 12.0;
    private static final double GIVE_UP_RANGE = 24.0;
    private static final double SNATCH_DISTANCE = 1.6;
    private static final int MAX_CHASE_TICKS = 300;
    private static final int FLEE_TICKS = 100;
    /** Roughly how often (in ticks) a raven thinks about stealing: about once a minute. */
    private static final int CHANCE_INTERVAL = 1200;

    private final RavenEntity raven;
    private @Nullable Player target;
    private boolean fleeing;
    private int ticks;
    private int cooldown;

    public RavenStealFromPlayerGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.cooldown = 600; // not straight after spawning
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (raven.isTame()) {
            return false; // tamed ravens don't steal from anyone
        }
        if (raven.hasCarriedItem() || raven.isInWater() || raven.isBathing()
                || raven.getRandom().nextInt(reducedTickDelay(CHANCE_INTERVAL)) != 0) {
            return false;
        }
        target = raven.level().getNearestPlayer(raven.getX(), raven.getY(), raven.getZ(), TARGET_RANGE,
                entity -> entity instanceof Player player && isValidTarget(player));
        return target != null;
    }

    @Override
    public void start() {
        fleeing = false;
        ticks = 0;
        chase();
    }

    @Override
    public boolean canContinueToUse() {
        if (fleeing) {
            return ticks < FLEE_TICKS;
        }
        return target != null && ticks < MAX_CHASE_TICKS && !raven.hasCarriedItem()
                && isValidTarget(target) && raven.distanceToSqr(target) < GIVE_UP_RANGE * GIVE_UP_RANGE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        if (fleeing || target == null) {
            return;
        }

        raven.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (raven.distanceToSqr(target.getX(), target.getY() + 1.0, target.getZ()) < SNATCH_DISTANCE * SNATCH_DISTANCE) {
            snatch(target);
            return;
        }
        // Players move, so keep re-aiming. Close in: fly straight at them.
        if (raven.distanceToSqr(target) < 16.0) {
            raven.getNavigation().stop();
            raven.getMoveControl().setWantedPosition(target.getX(), target.getY() + 1.0, target.getZ(), 1.4);
        } else if (ticks % 10 == 0 || raven.getNavigation().isDone()) {
            chase();
        }
    }

    @Override
    public void stop() {
        raven.getNavigation().stop();
        target = null;
        fleeing = false;
        cooldown = reducedTickDelay(2400 + raven.getRandom().nextInt(2400)); // 2-4 minutes
    }

    private void chase() {
        if (target != null) {
            raven.getNavigation().moveTo(target.getX(), target.getY() + 1.0, target.getZ(), 1.3);
        }
    }

    private void snatch(Player player) {
        Inventory inventory = player.getInventory();
        List<Integer> slots = valuableSlots(inventory);
        if (slots.isEmpty()) {
            ticks = MAX_CHASE_TICKS; // they put it away, give up
            return;
        }
        int slot = slots.get(raven.getRandom().nextInt(slots.size()));
        ItemStack taken = inventory.removeItem(slot, 1);
        if (taken.isEmpty()) {
            return;
        }
        raven.setCarriedItem(taken);
        raven.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.6F);
        raven.playSound(SoundEvents.PARROT_FLY, 0.6F, 0.8F);

        // Fly off up and away from the player.
        fleeing = true;
        ticks = 0;
        Vec3 away = raven.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 dest = raven.position().add(away.normalize().scale(12.0)).add(0.0, 5.0, 0.0);
        raven.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.4);
    }

    private boolean isValidTarget(Player player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator()
                && !raven.isScarecrowProtected(player.blockPosition())
                && !valuableSlots(player.getInventory()).isEmpty();
    }

    /** Main inventory and hotbar slots (not armor/offhand) holding a valuable. */
    private static List<Integer> valuableSlots(Inventory inventory) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            if (RavenEntity.isValuable(inventory.getItem(i))) {
                slots.add(i);
            }
        }
        return slots;
    }
}
