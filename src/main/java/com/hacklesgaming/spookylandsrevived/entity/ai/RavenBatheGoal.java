package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.EnumSet;

import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Now and then a raven goes for a bath, either in nearby shallow water (1-2 blocks deep,
 * open air above) or in a full water cauldron, whichever is closer. It settles in,
 * splashes about with the bathe animation, then flies off.
 */
public class RavenBatheGoal extends Goal {
    private static final int SEARCH_RADIUS = 10;
    private static final int SEARCH_DOWN = 8;
    private static final int SEARCH_UP = 4;
    private static final int SEARCH_SAMPLES = 60;
    /** Cauldrons are single blocks, so they get a full (smaller) scan instead of random samples. */
    private static final int CAULDRON_RADIUS = 8;
    private static final int CAULDRON_VERTICAL = 4;
    private static final int MAX_TRAVEL_TICKS = 240;
    /** Roughly how often (in ticks) it considers a bath. */
    private static final int CHANCE_INTERVAL = 600;
    /** Raven's feet sit this far up a full cauldron: body above the rim, feet in the water. */
    private static final double CAULDRON_SEAT = 0.8;

    private final RavenEntity raven;

    private @Nullable BlockPos bath;
    private boolean cauldron;
    private Vec3 surface = Vec3.ZERO;
    private boolean bathing;
    private int travelTicks;
    private int bathTicks;
    private int cooldown;

    public RavenBatheGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        // Don't go for a bath in the first moments after spawning.
        this.cooldown = 200;
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (raven.isInWater() || raven.hasCarriedItem() || raven.wantsFlockTakeoff()
                || raven.getRandom().nextInt(reducedTickDelay(CHANCE_INTERVAL)) != 0) {
            return false;
        }

        BlockPos water = findShallowWater();
        BlockPos tub = findFullCauldron();
        BlockPos origin = raven.blockPosition();
        if (tub != null && (water == null || tub.distSqr(origin) <= water.distSqr(origin))) {
            bath = tub;
            cauldron = true;
        } else {
            bath = water;
            cauldron = false;
        }
        return bath != null;
    }

    @Override
    public void start() {
        bathing = false;
        travelTicks = 0;
        double y = cauldron ? bath.getY() + CAULDRON_SEAT : bath.getY() + 0.9;
        surface = new Vec3(bath.getX() + 0.5, y, bath.getZ() + 0.5);
        if (!raven.getNavigation().moveTo(surface.x, bath.getY() + 1.5, surface.z, 1.0)) {
            bath = null;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (bath == null || !isStillBath(bath)) {
            return false;
        }
        if (bathing) {
            return bathTicks > 0 && (cauldron || raven.isInWater());
        }
        return travelTicks < MAX_TRAVEL_TICKS && !raven.wantsFlockTakeoff();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (bath == null) {
            return;
        }

        if (bathing) {
            bathTicks--;
            raven.getNavigation().stop();
            if (cauldron) {
                // Hold it in the middle of the cauldron (gravity is off while bathing there).
                raven.setPos(surface.x, surface.y, surface.z);
                raven.setDeltaMovement(Vec3.ZERO);
            } else {
                // Stay put on the pond surface.
                Vec3 m = raven.getDeltaMovement();
                raven.setDeltaMovement(m.x * 0.5, m.y, m.z * 0.5);
            }
            splash();
            return;
        }

        travelTicks++;
        double dx = raven.getX() - surface.x;
        double dz = raven.getZ() - surface.z;
        double horizontalSq = dx * dx + dz * dz;

        if (cauldron ? tickCauldronApproach(horizontalSq) : tickWaterApproach(horizontalSq)) {
            startBathing();
            return;
        }

        if (horizontalSq >= 9.0 && raven.getNavigation().isDone()) {
            if (!raven.getNavigation().moveTo(surface.x, bath.getY() + 1.5, surface.z, 1.0)) {
                travelTicks = MAX_TRAVEL_TICKS;
            }
        }
    }

    /** Pond: drop straight onto the water. Returns true once it's in. */
    private boolean tickWaterApproach(double horizontalSq) {
        if (raven.isInWater() && horizontalSq < 2.25) {
            return true;
        }
        if (horizontalSq < 9.0) {
            raven.getNavigation().stop();
            raven.getMoveControl().setWantedPosition(surface.x, surface.y - 0.3, surface.z, 0.8);
        }
        return false;
    }

    /** Cauldron: line up above the centre first, then sink in so it clears the rim. */
    private boolean tickCauldronApproach(double horizontalSq) {
        double dy = raven.getY() - surface.y;
        if (horizontalSq < 0.04 && Math.abs(dy) < 0.3) {
            return true;
        }
        if (horizontalSq < 9.0) {
            raven.getNavigation().stop();
            double targetY = horizontalSq > 0.02 ? bath.getY() + 1.5 : surface.y;
            raven.getMoveControl().setWantedPosition(surface.x, targetY, surface.z, 0.6);
        }
        return false;
    }

    private void startBathing() {
        bathing = true;
        bathTicks = 100 + raven.getRandom().nextInt(100); // 5-10 seconds
        raven.getNavigation().stop();
        raven.setBathing(true, cauldron);
        raven.playSound(SoundEvents.GENERIC_SPLASH, 0.4F, 1.4F);
    }

    @Override
    public void stop() {
        boolean hadBath = bathing;
        bathing = false;
        bath = null;
        raven.setBathing(false, false);
        raven.getNavigation().stop();
        if (hadBath) {
            raven.requestFlight(); // shake off and fly away
        }
        cooldown = reducedTickDelay(1200 + raven.getRandom().nextInt(1200));
    }

    private void splash() {
        if (!(raven.level() instanceof ServerLevel level)) {
            return;
        }
        RandomSource random = raven.getRandom();
        if (raven.tickCount % 3 == 0) {
            double y = cauldron ? bath.getY() + 0.95 : raven.getY() + 0.25;
            double spread = cauldron ? 0.15 : 0.25;
            level.sendParticles(ParticleTypes.SPLASH, raven.getX(), y, raven.getZ(),
                    5, spread, 0.05, spread, 0.1);
        }
        if (random.nextInt(25) == 0) {
            raven.playSound(SoundEvents.GENERIC_SPLASH, 0.25F, 1.3F + random.nextFloat() * 0.4F);
        }
    }

    private boolean isStillBath(BlockPos pos) {
        Level level = raven.level();
        return cauldron ? isFullCauldron(level.getBlockState(pos)) : level.getFluidState(pos).is(FluidTags.WATER);
    }

    private static boolean isFullCauldron(BlockState state) {
        return state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL;
    }

    /** Closest full water cauldron with room above it. */
    private @Nullable BlockPos findFullCauldron() {
        Level level = raven.level();
        BlockPos origin = raven.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-CAULDRON_RADIUS, -CAULDRON_VERTICAL, -CAULDRON_RADIUS),
                origin.offset(CAULDRON_RADIUS, CAULDRON_VERTICAL, CAULDRON_RADIUS))) {
            double dist = pos.distSqr(origin);
            if (dist >= bestDist || !isFullCauldron(level.getBlockState(pos))) {
                continue;
            }
            if (!level.isEmptyBlock(pos.above()) || !level.isEmptyBlock(pos.above(2))) {
                continue;
            }
            best = pos.immutable();
            bestDist = dist;
        }
        return best;
    }

    /** Random-sample the area for the closest water that's 1-2 blocks deep with air above. */
    private @Nullable BlockPos findShallowWater() {
        Level level = raven.level();
        RandomSource random = raven.getRandom();
        BlockPos origin = raven.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i < SEARCH_SAMPLES; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS,
                    random.nextInt(SEARCH_DOWN + SEARCH_UP + 1) - SEARCH_DOWN,
                    random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS);

            double dist = pos.distSqr(origin);
            if (dist >= bestDist) {
                continue;
            }
            if (!level.getFluidState(pos).is(FluidTags.WATER)) {
                continue;
            }
            // Top of the water, with room above to land.
            if (!level.isEmptyBlock(pos.above()) || !level.isEmptyBlock(pos.above(2))) {
                continue;
            }
            // Shallow: no water two blocks down.
            if (level.getFluidState(pos.below(2)).is(FluidTags.WATER)) {
                continue;
            }
            best = pos.immutable();
            bestDist = dist;
        }
        return best;
    }
}
