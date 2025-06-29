package com.github.nalamodikk.common.screen.player;

import com.github.nalamodikk.common.player.equipment.EquipmentType;
import com.github.nalamodikk.common.player.equipment.slot.SpecificEquipmentSlot;
import com.github.nalamodikk.register.ModDataAttachments;
import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ExtraEquipmentMenu extends AbstractContainerMenu {
    public static final int NINE_GRID_SLOT_COUNT = 9;
    public static final int EQUIPMENT_SLOT_COUNT = 8;

    private final Player player;
    private final NonNullList<ItemStack> gridRef;
    private final NonNullList<ItemStack> extraEquipmentRef;
    private final Container nineGridHandler;
    private final Container extraEquipmentHandler; // 🔥 新增：保存對 handler 的引用

    public ExtraEquipmentMenu(int syncId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(ModMenuTypes.EXTRA_EQUIPMENT_MENU.get(), syncId, playerInventory, new SimpleContainer(EQUIPMENT_SLOT_COUNT));
    }

    public ExtraEquipmentMenu(int syncId, Inventory playerInventory) {
        this(ModMenuTypes.EXTRA_EQUIPMENT_MENU.get(), syncId, playerInventory, new SimpleContainer(EQUIPMENT_SLOT_COUNT));
    }

    public ExtraEquipmentMenu(MenuType<?> type, int syncId, Inventory playerInventory, Container equipmentHandler) {
        super(type, syncId);
        this.player = playerInventory.player;

        // 新增玩家欄位
        addPlayerInventorySlots(playerInventory, 8, 170);

        // *** 新增：原版裝備槽位（直接同步） ***
        addVanillaEquipmentSlots(playerInventory, 8, 23);

        // === 🔥 修正：飾品裝備欄位的同步機制 ===
        NonNullList<ItemStack> extraEquipment = player.getData(ModDataAttachments.EXTRA_EQUIPMENT.get());
        if (extraEquipment == null) {
            extraEquipment = NonNullList.withSize(EQUIPMENT_SLOT_COUNT, ItemStack.EMPTY);
            player.setData(ModDataAttachments.EXTRA_EQUIPMENT.get(), extraEquipment);
        }
        this.extraEquipmentRef = extraEquipment;

        // 🔥 修正：建立有同步功能的 handler
        this.extraEquipmentHandler = new SimpleContainer(extraEquipment.size()) {
            @Override
            public void setChanged() {
                // 立即同步到 DataAttachment
                for (int i = 0; i < this.getContainerSize(); i++) {
                    extraEquipmentRef.set(i, this.getItem(i));
                }
                player.setData(ModDataAttachments.EXTRA_EQUIPMENT.get(), extraEquipmentRef);
                super.setChanged();
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                super.setItem(slot, stack);
                // 🔥 新增：每次設置物品時立即同步
                if (slot >= 0 && slot < extraEquipmentRef.size()) {
                    extraEquipmentRef.set(slot, stack);
                    player.setData(ModDataAttachments.EXTRA_EQUIPMENT.get(), extraEquipmentRef);
                }
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                ItemStack removed = super.removeItem(slot, amount);
                // 🔥 新增：移除物品時立即同步
                if (slot >= 0 && slot < extraEquipmentRef.size()) {
                    extraEquipmentRef.set(slot, this.getItem(slot));
                    player.setData(ModDataAttachments.EXTRA_EQUIPMENT.get(), extraEquipmentRef);
                }
                return removed;
            }
        };

        // 🔥 修正：初始化 handler 內容
        for (int i = 0; i < extraEquipment.size(); i++) {
            this.extraEquipmentHandler.setItem(i, extraEquipment.get(i));
        }

        // 新增額外裝備欄位（使用修正後的 handler）
        addExtraEquipmentSlots(this.extraEquipmentHandler, 61, 23);

        // === 🔥 修正：9格儲存欄位的同步機制 ===
        NonNullList<ItemStack> grid = player.getData(ModDataAttachments.NINE_GRID.get());
        if (grid == null) {
            grid = NonNullList.withSize(NINE_GRID_SLOT_COUNT, ItemStack.EMPTY);
            player.setData(ModDataAttachments.NINE_GRID.get(), grid);
        }
        this.gridRef = grid;

        // 🔥 修正：建立 handler 並覆寫所有相關方法
        this.nineGridHandler = new SimpleContainer(grid.size()) {
            @Override
            public void setChanged() {
                // 立即同步到 DataAttachment
                for (int i = 0; i < this.getContainerSize(); i++) {
                    gridRef.set(i, this.getItem(i));
                }
                player.setData(ModDataAttachments.NINE_GRID.get(), gridRef);
                super.setChanged();
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                super.setItem(slot, stack);
                // 🔥 新增：每次設置物品時立即同步
                if (slot >= 0 && slot < gridRef.size()) {
                    gridRef.set(slot, stack);
                    player.setData(ModDataAttachments.NINE_GRID.get(), gridRef);
                }
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                ItemStack removed = super.removeItem(slot, amount);
                // 🔥 新增：移除物品時立即同步
                if (slot >= 0 && slot < gridRef.size()) {
                    gridRef.set(slot, this.getItem(slot));
                    player.setData(ModDataAttachments.NINE_GRID.get(), gridRef);
                }
                return removed;
            }
        };

        // 🔥 修正：初始化 handler 內容
        for (int i = 0; i < grid.size(); i++) {
            this.nineGridHandler.setItem(i, grid.get(i));
        }

        addNineGridSlots(this.nineGridHandler, 176, 170);
    }

    // 新增玩家欄位
    protected void addPlayerInventorySlots(Inventory playerInventory, int startX, int startY) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slot = col + row * 9 + 9;
                this.addSlot(new Slot(playerInventory, slot, startX + col * 18, startY + row * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, startX + i * 18, startY + 58));
        }
    }

    /**
     * 原版裝備槽位 - 直接與玩家背包同步
     */
    protected void addVanillaEquipmentSlots(Inventory playerInventory, int baseX, int baseY) {
        // 原版裝備欄位：頭盔(39)、胸甲(38)、腿甲(37)、靴子(36)
        this.addSlot(new SpecificEquipmentSlot(playerInventory, 39, baseX, baseY, EquipmentType.HELMET));
        this.addSlot(new SpecificEquipmentSlot(playerInventory, 38, baseX, baseY + 18, EquipmentType.CHESTPLATE));
        this.addSlot(new SpecificEquipmentSlot(playerInventory, 37, baseX, baseY + 36, EquipmentType.LEGGINGS));
        this.addSlot(new SpecificEquipmentSlot(playerInventory, 36, baseX, baseY + 54, EquipmentType.BOOTS));
    }

    protected void addSpecificEquipmentSlots(Container handler, int baseX, int baseY) {
        EquipmentType[] types = {
                EquipmentType.SHOULDER_PAD, EquipmentType.ARM_ARMOR,
                EquipmentType.BELT, EquipmentType.GLOVES,
                EquipmentType.GOGGLES, EquipmentType.ENGINE,
                EquipmentType.REACTOR, EquipmentType.EXOSKELETON
        };

        for (int i = 0; i < types.length; i++) {
            int row = i / 4;
            int col = i % 4;
            this.addSlot(new SpecificEquipmentSlot(handler, i,
                    baseX + col * 18, baseY + row * 18, types[i]));
        }
    }

    // 額外裝備欄位
    protected void addExtraEquipmentSlots(Container handler, int baseX, int baseY) {
        int slotIndex = 0;
        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 4; row++) {
                this.addSlot(new Slot(handler, slotIndex++, baseX + col * 18, baseY + row * 18));
            }
        }
    }

    // 新增裝備儲存欄位
    protected void addNineGridSlots(Container handler, int baseX, int baseY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                this.addSlot(new Slot(handler, index, baseX + col * 18, baseY + row * 18));
            }
        }
    }

    // 🔥 修正：確保在關閉界面時保存數據
    @Override
    public void removed(Player player) {
        super.removed(player);

        // 🔥 新增：強制保存額外裝備數據
        for (int i = 0; i < this.extraEquipmentHandler.getContainerSize(); i++) {
            this.extraEquipmentRef.set(i, this.extraEquipmentHandler.getItem(i));
        }
        player.setData(ModDataAttachments.EXTRA_EQUIPMENT.get(), this.extraEquipmentRef);

        // 🔥 新增：強制保存九宮格數據
        for (int i = 0; i < this.nineGridHandler.getContainerSize(); i++) {
            this.gridRef.set(i, this.nineGridHandler.getItem(i));
        }
        player.setData(ModDataAttachments.NINE_GRID.get(), this.gridRef);

        // 🔥 新增：標記玩家數據已更改（觸發保存）
        if (!player.level().isClientSide) {
            player.inventoryMenu.broadcastChanges();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // TODO: shift-click 實作
    }

    // 🔥 新增：獲取額外裝備 handler（用於調試）
    public Container getExtraEquipmentHandler() {
        return this.extraEquipmentHandler;
    }

    // 🔥 新增：獲取九宮格 handler（用於調試）
    public Container getNineGridHandler() {
        return this.nineGridHandler;
    }
}