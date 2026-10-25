package com.hacklesgaming.spookylandsrevived.block.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.hacklesgaming.spookylandsrevived.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Lives in the scarecrow's bottom half.
 * - Keeps a per-world list of loaded scarecrows so ravens can check "is there a scarecrow
 *   near here?" without scanning blocks.
 * - Is the GeckoLib animatable for the scarecrow model (the block itself renders nothing;
 *   the 2-block-tall model is drawn by GeoBlockRenderer from this block entity).
 *
 * Assets (GeckoLib defaults for block entity "scarecrow"):
 *   assets/spookylandsrevived/geckolib/models/block/scarecrow.geo.json
 *   assets/spookylandsrevived/geckolib/animations/block/scarecrow.animation.json
 *   assets/spookylandsrevived/textures/block/scarecrow.png
 * Animation names must match: "animation.scarecrow.idle" (loop) and
 * "animation.scarecrow.spook" (plays once when it scares a raven off).
 */
public class ScarecrowBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final Map<Level, Set<BlockPos>> LOADED = Collections.synchronizedMap(new WeakHashMap<>());

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.scarecrow.idle");
    private static final RawAnimation SPOOK = RawAnimation.begin().thenPlay("animation.scarecrow.spook");
    private static final String CONTROLLER = "main";
    /** Don't replay the spook animation more often than this (ticks), even if several ravens flee. */
    private static final int SPOOK_COOLDOWN_TICKS = 60;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private long lastSpookTime = Long.MIN_VALUE / 2;

    public ScarecrowBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCARECROW.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            LOADED.computeIfAbsent(level, l -> Collections.synchronizedSet(new HashSet<>())).add(worldPosition.immutable());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            Set<BlockPos> set = LOADED.get(level);
            if (set != null) {
                set.remove(worldPosition);
            }
        }
    }

    /** Server side: play the spook animation for everyone nearby (rate limited). */
    public void playSpook() {
        if (level == null || level.isClientSide()) {
            return;
        }
        long now = level.getGameTime();
        if (now - lastSpookTime < SPOOK_COOLDOWN_TICKS) {
            return;
        }
        lastSpookTime = now;
        triggerAnim(CONTROLLER, "spook");
    }

    /** Closest loaded scarecrow within range of pos (by straight-line distance), or null. */
    public static @Nullable BlockPos findNearest(Level level, BlockPos pos, double range) {
        Set<BlockPos> set = LOADED.get(level);
        if (set == null || set.isEmpty()) {
            return null;
        }
        BlockPos best = null;
        double bestDist = range * range;
        synchronized (set) {
            for (BlockPos scarecrow : set) {
                double dist = scarecrow.distSqr(pos);
                if (dist <= bestDist) {
                    bestDist = dist;
                    best = scarecrow;
                }
            }
        }
        return best;
    }

    public static boolean isNear(Level level, BlockPos pos, double range) {
        return findNearest(level, pos, range) != null;
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ScarecrowBlockEntity>(CONTROLLER, 5, test -> test.setAndContinue(IDLE))
                .triggerableAnim("spook", SPOOK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
