package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.EnumSet;
import java.util.List;

import com.hacklesgaming.spookylandsrevived.Config;
import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * Now and then a raven lands on a fully grown crop (wheat, carrots, potatoes, beetroot,
 * or any modded CropBlock), pulls it up and flies off with the produce.
 * The crop is left replanted at stage 0, like it was harvested.
 * Respects the mobGriefing game rule.
 */
public class RavenRaidCropGoal extends Goal {
    private static final int SEARCH_RADIUS = 12;
    private static final int SEARCH_VERTICAL = 4;
    private static final int SEARCH_SAMPLES = 120;
    private static final int MAX_TICKS = 300;
    /** Roughly how often (in ticks) it looks for crops: about every 30 seconds. */
    private static final int CHANCE_INTERVAL = 600;

    private final RavenEntity raven;
    private @Nullable BlockPos crop;
    private int ticks;
    private int cooldown;

    public RavenRaidCropGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        this.cooldown = 400;
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!(raven.level() instanceof ServerLevel level) || !EventHooks.canEntityGrief(level, raven)) {
            return false;
        }
        if (raven.isTame() || !Config.RAVENS_RAID_CROPS.get()) {
            return false; // tamed ravens leave crops alone; config can switch raids off
        }
        if (raven.hasCarriedItem() || raven.isInWater() || raven.isBathing() || raven.wantsFlockTakeoff()
                || raven.getRandom().nextInt(reducedTickDelay(CHANCE_INTERVAL)) != 0) {
            return false;
        }
        crop = findRipeCrop();
        return crop != null;
    }

    @Override
    public void start() {
        ticks = 0;
        raven.getNavigation().moveTo(crop.getX() + 0.5, crop.getY() + 1.0, crop.getZ() + 0.5, 1.0);
    }

    @Override
    public boolean canContinueToUse() {
        return crop != null && ticks < MAX_TICKS && !raven.hasCarriedItem() && isRipe(raven.level().getBlockState(crop));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (crop == null) {
            return;
        }
        ticks++;
        double dx = raven.getX() - (crop.getX() + 0.5);
        double dz = raven.getZ() - (crop.getZ() + 0.5);
        double horizontalSq = dx * dx + dz * dz;
        double dy = raven.getY() - crop.getY();

        if (horizontalSq < 0.5 && dy < 1.0 && dy > -0.5) {
            pullUp();
            return;
        }
        if (horizontalSq < 9.0) {
            raven.getNavigation().stop();
            raven.getMoveControl().setWantedPosition(crop.getX() + 0.5, crop.getY() + 0.1, crop.getZ() + 0.5, 0.8);
        } else if (raven.getNavigation().isDone()) {
            raven.getNavigation().moveTo(crop.getX() + 0.5, crop.getY() + 1.0, crop.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        raven.getNavigation().stop();
        crop = null;
        cooldown = reducedTickDelay(1200 + raven.getRandom().nextInt(1200));
    }

    private void pullUp() {
        if (!(raven.level() instanceof ServerLevel level) || crop == null) {
            return;
        }
        BlockState state = level.getBlockState(crop);
        if (!(state.getBlock() instanceof CropBlock cropBlock) || !cropBlock.isMaxAge(state)) {
            return;
        }
        // The first drop is the produce (wheat, carrot, potato, beetroot...).
        List<ItemStack> drops = Block.getDrops(state, level, crop, null);
        ItemStack produce = drops.stream().filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY);

        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, crop, Block.getId(state));
        level.setBlock(crop, cropBlock.getStateForAge(0), Block.UPDATE_ALL);
        if (!produce.isEmpty()) {
            raven.setCarriedItem(produce.copyWithCount(1));
        }
        raven.requestFlight();
        crop = null;
    }

    private static boolean isRipe(BlockState state) {
        return state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state);
    }

    /** Random-sample the area for the closest fully grown crop with open air above. */
    private @Nullable BlockPos findRipeCrop() {
        RandomSource random = raven.getRandom();
        BlockPos origin = raven.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < SEARCH_SAMPLES; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS,
                    random.nextInt(SEARCH_VERTICAL * 2 + 1) - SEARCH_VERTICAL,
                    random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS);
            double dist = pos.distSqr(origin);
            if (dist >= bestDist || !isRipe(raven.level().getBlockState(pos))) {
                continue;
            }
            if (!raven.level().isEmptyBlock(pos.above()) || raven.isScarecrowProtected(pos)) {
                continue;
            }
            best = pos.immutable();
            bestDist = dist;
        }
        return best;
    }
}
