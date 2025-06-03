package com.github.nalamodikk.common.block.mana_crafting;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class UpdatingSlotItemHandler extends SlotItemHandler {
    private final AbstractContainerMenu menu;

    public UpdatingSlotItemHandler(IItemHandler handler, int index, int x, int y, AbstractContainerMenu menu) {
        super(handler, index, x, y);
        this.menu = menu;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        menu.slotsChanged(this.container); // ✅ 強制觸發更新
    }
}
