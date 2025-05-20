package com.github.nalamodikk.client.screenAPI.gui.slot;

import com.github.nalamodikk.common.item.upgrade.UpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class UpgradeSlot extends Slot {
    public UpgradeSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof UpgradeItem;
    }
}
