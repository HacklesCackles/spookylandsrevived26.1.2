package com.hacklesgaming.spookylandsrevived.pet;

import java.util.EnumSet;
import java.util.Optional;

import com.hacklesgaming.spookylandsrevived.Config;
import com.hacklesgaming.spookylandsrevived.registry.ModAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * Wander mode: the pet roams within a few blocks of its home spot and ignores its owner.
 * Runs at a high priority and takes the MOVE flag, so the pet's own follow, teleport and
 * stroll goals can't run while it's active. Switches itself off as soon as the pet is
 * ordered to sit (so vanilla's sit toggle always wins), or when it has a target (combat).
 */
public class WanderNearHomeGoal extends Goal {
    private final TamableAnimal pet;
    private final double speed;
    private int cooldown;

    public WanderNearHomeGoal(TamableAnimal pet, double speed) {
        this.pet = pet;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        PetModeData data = pet.getData(ModAttachments.PET_MODE);
        if (!data.wandering()) {
            return false;
        }
        if (pet.isOrderedToSit()) {
            PetModes.clearWander(pet); // sitting wins: standing up again goes back to following
            return false;
        }
        return data.home().isPresent() && pet.isTame() && !pet.isPassenger() && pet.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        cooldown = 20;
    }

    @Override
    public void stop() {
        pet.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (--cooldown > 0) {
            return;
        }
        cooldown = 60 + pet.getRandom().nextInt(100);

        Optional<BlockPos> home = PetModes.getHome(pet);
        if (home.isEmpty()) {
            return;
        }
        int radius = Config.PET_WANDER_RADIUS.get();
        Vec3 center = Vec3.atBottomCenterOf(home.get());
        double radiusSqr = (double) radius * radius;

        // Drifted out of range (after a fight, say): walk straight back.
        if (pet.position().distanceToSqr(center) > radiusSqr) {
            pet.getNavigation().moveTo(center.x, center.y, center.z, speed);
            return;
        }
        if (!pet.getNavigation().isDone()) {
            return;
        }

        Vec3 target = pickTarget(center, radius, radiusSqr);
        if (target != null) {
            pet.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }

    private Vec3 pickTarget(Vec3 center, int radius, double radiusSqr) {
        RandomSource random = pet.getRandom();
        if (pet.getMoveControl() instanceof FlyingMoveControl) {
            // Flying pets: any spot in the circle around home, a little above it.
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            return new Vec3(center.x + Math.cos(angle) * dist, center.y + 1 + random.nextInt(3),
                    center.z + Math.sin(angle) * dist);
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            Vec3 pos = LandRandomPos.getPos(pet, 8, 4);
            if (pos != null && pos.distanceToSqr(center) <= radiusSqr) {
                return pos;
            }
        }
        return null;
    }
}
