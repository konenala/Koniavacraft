package com.github.nalamodikk.common.upgrade;

import com.github.nalamodikk.common.item.upgrade.UpgradeItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class UpgradeInventory implements Container {
    private final NonNullList<ItemStack> slots;
    private final int maxSlots;

    public UpgradeInventory(int size) {
        this.maxSlots = size;
        this.slots = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public int getUpgradeCount(UpgradeType type) {
        return (int) slots.stream()
                .filter(stack -> stack.getItem() instanceof UpgradeItem upgrade && upgrade.getUpgradeType() == type)
                .count();
    }

    public void setItem(int index, ItemStack stack) {
        if (index >= 0 && index < maxSlots) {
            slots.set(index, stack);
        }
    }

    public ItemStack getItem(int index) {
        return slots.get(index);
    }

    public int getSize() {
        return maxSlots;
    }

    public NonNullList<ItemStack> getAll() {
        return slots;
    }

    @Override
    public int getContainerSize() {
        return slots.size();
    }

    @Override
    public boolean isEmpty() {
        return slots.stream().allMatch(ItemStack::isEmpty);
    }



    @Override
    public ItemStack removeItem(int index, int count) {
        return ContainerHelper.removeItem(slots, index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = slots.get(index);
        slots.set(index, ItemStack.EMPTY);
        return stack;
    }



    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return true; // 或自訂距離檢查
    }

    @Override
    public void clearContent() {
        slots.clear();
    }

}
