package com.github.nalamodikk.common.screen.block.shared;

import com.github.nalamodikk.register.ModMenuTypes;
import com.github.nalamodikk.common.utils.upgrade.UpgradeSlot;
import com.github.nalamodikk.common.utils.upgrade.api.IUpgradeableMachine;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class UpgradeMenu extends AbstractContainerMenu {
    private final Container upgradeInventory;
    private final IUpgradeableMachine machine;

    public UpgradeMenu(int id, Inventory playerInv, Container upgradeInventory,IUpgradeableMachine machine) {
        super(ModMenuTypes.UPGRADE_MENU.get(), id);
        this.upgradeInventory = upgradeInventory;
        this.machine = machine;

        checkContainerSize(upgradeInventory, 4);

        // 升級槽（可自行調整位置）
        this.addSlot(new UpgradeSlot(upgradeInventory, 0, 47, 37));
        this.addSlot(new UpgradeSlot(upgradeInventory, 1, 65, 37));
        this.addSlot(new UpgradeSlot(upgradeInventory, 2, 83, 37));
        this.addSlot(new UpgradeSlot(upgradeInventory, 3, 101, 37));

        // 玩家背包槽
        this.addPlayerInventoryAndHotbar(playerInv, 7, 83);

    }

    protected void addPlayerInventoryAndHotbar(Inventory playerInv, int offsetX, int offsetY) {
        // 玩家主背包（3 行 9 欄）
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slotIndex = col + row * 9 + 9;
                int x = offsetX + col * 18;
                int y = offsetY + row * 18;
                this.addSlot(new Slot(playerInv, slotIndex, x, y));
            }
        }

        // 快捷欄（1 行 9 欄）
        for (int col = 0; col < 9; ++col) {
            int x = offsetX + col * 18;
            int y = offsetY + 58;
            this.addSlot(new Slot(playerInv, col, x, y));
        }
    }


    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;

        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();

        // 升級欄位範圍（index 0 ~ 3）
        if (index < 4) {
            if (!this.moveItemStackTo(original, 4, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(original, 0, 4, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, original);
        return copy;
    }


    @Override
    public boolean stillValid(Player player) {
        return true; // 根據實際需求判斷範圍
    }
    public IUpgradeableMachine getMachine() {
        return this.machine;
    }



}
