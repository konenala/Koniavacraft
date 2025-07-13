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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 🚀 修正版動態升級菜單 - 使用mana_gui_slot.png動態繪製
 *
 * 🤔 概念解釋：為什麼不拉伸背景？
 * - 拉伸會讓玩家背包槽位變形（18x18 → 18x25）
 * - 背景材質的細節會被破壞
 * - 整體視覺效果會很醜
 *
 * 💡 設計理念：動態槽位 + 固定背景
 * - 🎯 保持原有背景不變
 * - 🎨 使用mana_gui_slot.png動態繪製升級槽
 * - 📐 智能計算槽位位置和GUI高度
 * - 🔧 玩家背包位置自動調整
 */
public class UpgradeMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeMenu.class);

    private final Container upgradeInventory;
    private final IUpgradeableMachine machine;
    private final int upgradeSlotCount;

    public UpgradeMenu(int id, Inventory playerInv, Container upgradeInventory, IUpgradeableMachine machine) {
        super(ModMenuTypes.UPGRADE_MENU.get(), id);
        this.upgradeInventory = upgradeInventory;
        this.machine = machine;
        this.upgradeSlotCount = upgradeInventory.getContainerSize();

        // ✅ 移除硬編碼檢查，支援任意槽位數量
        // checkContainerSize(upgradeInventory, 4); // 不再限制為4個

        // 🎯 動態添加升級槽位
        addDynamicUpgradeSlots();

        // 🎨 動態計算玩家背包位置
        int playerInventoryY = calculatePlayerInventoryY();
        this.addPlayerInventoryAndHotbar(playerInv, 7, playerInventoryY);
    }

    /**
     * 🎯 動態添加升級槽位
     * 根據實際槽位數量自動計算佈局
     */
    private void addDynamicUpgradeSlots() {
        if (upgradeSlotCount <= 0) return;

        // 🎨 佈局策略
        int slotsPerRow = Math.min(4, upgradeSlotCount); // 每行最多4個
        int rows = (upgradeSlotCount + slotsPerRow - 1) / slotsPerRow; // 向上取整

        // 🎯 居中計算起始位置
        int slotSpacing = 18;
        int totalWidth = slotsPerRow * slotSpacing - 2; // 減去最後一個間距
        int startX = (176 - totalWidth) / 2; // GUI寬度176，居中
        int startY = 35; // 標題下方


        // 🔧 添加所有升級槽位
        for (int i = 0; i < upgradeSlotCount; i++) {
            int row = i / slotsPerRow;
            int col = i % slotsPerRow;

            // 📐 精確計算每個槽位位置
            int x = startX + col * slotSpacing;
            int y = startY + row * slotSpacing;

            this.addSlot(new UpgradeSlot(upgradeInventory, i, x, y));
        }
    }

    /**
     * 📐 計算玩家背包的Y位置
     * 確保不與升級槽位重疊
     */
    private int calculatePlayerInventoryY() {
        if (upgradeSlotCount <= 4) {
            // 🎯 4槽位以下：使用原有位置
            return 83;
        } else {
            // 🎨 超過4槽位：動態計算
            int rows = (upgradeSlotCount + 3) / 4; // 向上取整
            int upgradeAreaHeight = rows * 18;
            int startY = 35;
            int spacing = 25; // 升級區域和背包間距

            int playerInventoryY = startY + upgradeAreaHeight + spacing;
            return playerInventoryY;
        }
    }

    /**
     * 📊 計算GUI總高度
     * 供Screen使用
     */
    public int calculateGUIHeight() {
        if (upgradeSlotCount <= 4) {
            // 🎯 4槽位以下：使用原有高度
            return 166;
        } else {
            // 🎨 超過4槽位：動態計算
            int playerInventoryY = calculatePlayerInventoryY();
            int playerInventoryHeight = 76; // 3行背包 + 1行快捷欄 + 間距
            int bottomMargin = 8;

            int totalHeight = playerInventoryY + playerInventoryHeight + bottomMargin;
            return totalHeight;
        }
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

        // 🎯 動態升級欄位範圍（0 ~ upgradeSlotCount-1）
        if (index < upgradeSlotCount) {
            // 從升級槽移到玩家背包
            if (!this.moveItemStackTo(original, upgradeSlotCount, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 從玩家背包移到升級槽
            if (!this.moveItemStackTo(original, 0, upgradeSlotCount, false)) {
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

    /**
     * 📊 獲取升級槽位數量
     * 供Screen使用
     */
    public int getUpgradeSlotCount() {
        return upgradeSlotCount;
    }

    /**
     * 🔧 獲取升級庫存
     * 供Screen使用（如果需要的話）
     */
    public Container getUpgradeInventory() {
        return upgradeInventory;
    }
}