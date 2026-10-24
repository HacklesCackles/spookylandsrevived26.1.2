package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.EnumSet;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;
import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/**
 * Tamed ravens bring things to their owner, a bit like cats:
 * - Anything a tamed raven picks up (a valuable lying around, loot stolen from another
 *   player) gets taken to the owner instead of the nest.
 * - Every so often it "finds" a little gift on its own (loot table
 *   spookylandsrevived:gameplay/raven_gift) and brings that.
 * It flies to the owner and drops the item at their feet.
 */
public class RavenGiftGoal extends Goal {
    public static final ResourceKey<LootTable> GIFT_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(SpookyLandsRevived.MOD_ID, "gameplay/raven_gift"));

    /** Owner must be within this range for a delivery. */
    private static final double DELIVERY_RANGE = 48.0;
    private static final double DROP_DISTANCE = 2.0;
    private static final int MAX_TICKS = 600;
    /**
     * Time between self-found gifts, counted in goal checks (about every 2 ticks),
     * so 3000-6000 here is roughly 5-10 minutes.
     */
    private static final int GIFT_MIN_TICKS = 3000;
    private static final int GIFT_RANDOM_TICKS = 3000;

    private final RavenEntity raven;
    private @Nullable Player owner;
    private int ticks;
    private int giftCooldown;

    public RavenGiftGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.giftCooldown = GIFT_MIN_TICKS + raven.getRandom().nextInt(GIFT_RANDOM_TICKS);
    }

    @Override
    public boolean canUse() {
        if (!raven.isTame() || raven.isOrderedToSit() || raven.isBathing()) {
            return false;
        }
        Player player = raven.getOwner();
        if (player == null || player.isSpectator() || raven.distanceToSqr(player) > DELIVERY_RANGE * DELIVERY_RANGE) {
            return false;
        }

        if (!raven.hasCarriedItem()) {
            // Count down to finding a gift of its own.
            if (--giftCooldown > 0) {
                return false;
            }
            giftCooldown = GIFT_MIN_TICKS + raven.getRandom().nextInt(GIFT_RANDOM_TICKS);
            if (!findGift()) {
                return false;
            }
        }
        owner = player;
        return true;
    }

    @Override
    public void start() {
        ticks = 0;
        flyToOwner();
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && owner.isAlive() && owner.level() == raven.level()
                && raven.hasCarriedItem() && !raven.isOrderedToSit() && ticks < MAX_TICKS;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        ticks++;
        raven.getLookControl().setLookAt(owner, 10.0F, raven.getMaxHeadXRot());
        if (raven.distanceToSqr(owner) < DROP_DISTANCE * DROP_DISTANCE) {
            deliver();
            return;
        }
        if (raven.distanceToSqr(owner) < 16.0) {
            raven.getNavigation().stop();
            raven.getMoveControl().setWantedPosition(owner.getX(), owner.getY() + 1.0, owner.getZ(), 1.0);
        } else if (ticks % 10 == 0 || raven.getNavigation().isDone()) {
            flyToOwner();
        }
    }

    @Override
    public void stop() {
        owner = null;
        raven.getNavigation().stop();
    }

    private void flyToOwner() {
        if (owner != null) {
            raven.getNavigation().moveTo(owner.getX(), owner.getY() + 1.5, owner.getZ(), 1.2);
        }
    }

    private void deliver() {
        if (!(raven.level() instanceof ServerLevel level) || owner == null) {
            return;
        }
        ItemStack gift = raven.getCarriedItem();
        raven.setCarriedItem(ItemStack.EMPTY);
        ItemEntity item = new ItemEntity(level, owner.getX(), owner.getY() + 0.3, owner.getZ(), gift);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        raven.playSound(SoundEvents.ITEM_PICKUP, 0.5F, 1.0F);
        level.broadcastEntityEvent(raven, RavenEntity.EVENT_GIFT_PARTICLES);
        owner = null;
    }

    /** Roll the gift loot table and hold the first thing it gives. */
    private boolean findGift() {
        if (!(raven.level() instanceof ServerLevel level)) {
            return false;
        }
        ItemStack[] found = {ItemStack.EMPTY};
        raven.dropFromGiftLootTable(level, GIFT_LOOT, (l, stack) -> {
            if (found[0].isEmpty() && !stack.isEmpty()) {
                found[0] = stack;
            }
        });
        if (found[0].isEmpty()) {
            return false;
        }
        raven.setCarriedItem(found[0]);
        return true;
    }
}
