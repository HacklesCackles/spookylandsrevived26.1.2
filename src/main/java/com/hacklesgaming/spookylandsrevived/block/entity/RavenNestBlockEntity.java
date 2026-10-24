package com.hacklesgaming.spookylandsrevived.block.entity;

import com.hacklesgaming.spookylandsrevived.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Stores the loot ravens bring back. 9 slots, filled in order. */
public class RavenNestBlockEntity extends BlockEntity {
    public static final int SIZE = 9;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public RavenNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RAVEN_NEST.get(), pos, state);
    }

    /** Put a stack in the nest. Returns whatever didn't fit (empty if it all went in). */
    public ItemStack addItem(ItemStack stack) {
        ItemStack remaining = stack.copy();
        // Top up matching stacks first, then use empty slots.
        for (int i = 0; i < SIZE && !remaining.isEmpty(); i++) {
            ItemStack slot = items.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remaining)) {
                int move = Math.min(remaining.getCount(), slot.getMaxStackSize() - slot.getCount());
                if (move > 0) {
                    slot.grow(move);
                    remaining.shrink(move);
                }
            }
        }
        for (int i = 0; i < SIZE && !remaining.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, remaining.copy());
                remaining = ItemStack.EMPTY;
            }
        }
        if (remaining.getCount() != stack.getCount()) {
            setChanged();
        }
        return remaining;
    }

    /** True if a stack like this could go in (some room left). */
    public boolean canAccept(ItemStack stack) {
        for (ItemStack slot : items) {
            if (slot.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /** Take out the most recently filled stack, or EMPTY if the nest is empty. */
    public ItemStack takeLast() {
        for (int i = SIZE - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) {
                ItemStack taken = items.get(i);
                items.set(i, ItemStack.EMPTY);
                setChanged();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // Nest broken: spill the loot.
        if (this.level != null) {
            Containers.dropContents(this.level, pos, items);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
    }
}
