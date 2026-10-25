package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.hacklesgaming.spookylandsrevived.Config;
import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Now and then a wild raven flies over to a nearby animal or villager (entity tag
 * spookylandsrevived:raven_rideable), lands on its back / head and rides along for a while.
 *
 * It hops off when the ride is over, when it or its mount gets hurt, when the mount goes into
 * water, lies down in a bed or gets picked up by something else, or when a player walks right
 * up to it (sneak to get close without scaring it off).
 *
 * The raven is added to the minecraft:non_controlling_rider tag so it never steers its mount.
 */
public class RavenRideMobGoal extends Goal {
    private static final double SEARCH_RANGE = 12.0;
    private static final double MOUNT_DISTANCE = 1.0;
    private static final int MAX_TRAVEL_TICKS = 300;
    /** Roughly how often (in ticks) it thinks about hitching a ride: about every 1.5 minutes. */
    private static final int CHANCE_INTERVAL = 1800;
    /** A player (not sneaking) this close scares it off its mount. */
    private static final double PLAYER_SCARE_RANGE = 2.5;

    private final RavenEntity raven;
    private @Nullable Mob mount;
    private int travelTicks;
    private int rideTicks;
    private int cooldown;

    public RavenRideMobGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        this.cooldown = 1200; // not straight after spawning
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!Config.RAVENS_RIDE_MOBS.get() || raven.isTame() || raven.isPassenger() || raven.hasCarriedItem()
                || raven.isInWater() || raven.isBathing() || raven.wantsFlockTakeoff()
                || raven.getRandom().nextInt(reducedTickDelay(CHANCE_INTERVAL)) != 0) {
            return false;
        }
        List<Mob> candidates = raven.level().getEntitiesOfClass(Mob.class,
                raven.getBoundingBox().inflate(SEARCH_RANGE), this::isRideable);
        mount = candidates.stream().min(Comparator.comparingDouble(raven::distanceToSqr)).orElse(null);
        return mount != null;
    }

    @Override
    public void start() {
        travelTicks = 0;
        rideTicks = 0;
        flyToMount();
    }

    @Override
    public boolean canContinueToUse() {
        if (mount == null || !mount.isAlive()) {
            return false;
        }
        if (raven.isPassenger()) {
            return raven.getVehicle() == mount && rideTicks > 0 && !shouldHopOff(mount);
        }
        return travelTicks < MAX_TRAVEL_TICKS && isRideable(mount);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (mount == null) {
            return;
        }
        if (raven.isPassenger()) {
            rideTicks--;
            return;
        }

        travelTicks++;
        Vec3 seat = seatOf(mount);
        raven.getLookControl().setLookAt(mount, 30.0F, 30.0F);
        if (raven.position().distanceToSqr(seat) < MOUNT_DISTANCE * MOUNT_DISTANCE) {
            raven.getNavigation().stop();
            raven.setRidingByGoal(true); // before startRiding, so RavenEntity doesn't kick it off
            if (raven.startRiding(mount)) {
                // Ride for 20-60 seconds.
                rideTicks = reducedTickDelay(400 + raven.getRandom().nextInt(800));
            } else {
                raven.setRidingByGoal(false);
                travelTicks = MAX_TRAVEL_TICKS; // someone else got there first
            }
            return;
        }
        // The mount wanders about, so keep re-aiming. Close in: fly straight at its back.
        if (raven.position().distanceToSqr(seat) < 9.0) {
            raven.getNavigation().stop();
            raven.getMoveControl().setWantedPosition(seat.x, seat.y + 0.2, seat.z, 0.9);
        } else if (raven.getNavigation().isDone() || travelTicks % 10 == 0) {
            flyToMount();
        }
    }

    @Override
    public void stop() {
        boolean wasRiding = raven.isPassenger();
        if (wasRiding) {
            raven.stopRiding();
            // Little hop up so it doesn't drop straight onto the mount's head.
            raven.setDeltaMovement(raven.getDeltaMovement().add(0.0, 0.3, 0.0));
        }
        raven.setRidingByGoal(false);
        raven.getNavigation().stop();
        mount = null;
        // Rode for a while: wait longer before the next ride. Never got there: try again sooner.
        cooldown = reducedTickDelay(wasRiding ? 2400 + raven.getRandom().nextInt(2400) : 600);
    }

    private void flyToMount() {
        if (mount != null) {
            Vec3 seat = seatOf(mount);
            raven.getNavigation().moveTo(seat.x, seat.y + 0.5, seat.z, 1.0);
        }
    }

    /** Where the raven's feet end up when riding (top of the back, or top of a villager's head). */
    private static Vec3 seatOf(Mob mob) {
        return mob.position().add(0.0, mob.getBbHeight(), 0.0);
    }

    private boolean isRideable(Mob mob) {
        return mob.isAlive() && mob != raven
                && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(mob.getType()).is(RavenEntity.RIDEABLE)
                && !mob.isBaby() && !mob.isVehicle() && !mob.isPassenger()
                && !mob.isInWater() && !mob.isSleeping()
                && !raven.isHazardBelow(mob.blockPosition())
                && !playerTooClose(mob);
    }

    private boolean shouldHopOff(Mob mob) {
        return mob.isInWater() || mob.isSleeping() || mob.hurtTime > 0 || mob.isPassenger()
                || mob.getPassengers().size() > 1 || playerTooClose(mob);
    }

    /** A player right next to the mount (unless they're sneaking or in creative/spectator). */
    private static boolean playerTooClose(Entity mob) {
        Player player = mob.level().getNearestPlayer(mob, PLAYER_SCARE_RANGE);
        return player != null && player.isAlive() && !player.isShiftKeyDown()
                && !player.isCreative() && !player.isSpectator();
    }
}
