package com.github.nalamodikk.common.block.blockentity.mana_infuser;

import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModMenuTypes;
import com.github.nalamodikk.register.ModRecipes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 🔮 魔力注入機界面容器
 *
 * 功能：
 * - 管理輸入/輸出槽位
 * - 處理物品轉移
 * - 提供數據同步
 */
public class ManaInfuserMenu extends AbstractContainerMenu {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    // 槽位佈局常量
    private static final int INPUT_SLOT_X = 48;
    private static final int INPUT_SLOT_Y = 35;
    private static final int OUTPUT_SLOT_X = 122;
    private static final int OUTPUT_SLOT_Y = 34;
    // private final ContainerLevelAccess access;

    // 玩家背包槽位起始位置
    private static final int INVENTORY_START_X = 8;
    private static final int INVENTORY_START_Y = 84;
    private static final int HOTBAR_START_Y = 142;

    private final ManaInfuserBlockEntity blockEntity;

    // 🆕 數據同步容器
    private final ContainerData data;
    private final ContainerLevelAccess access;

    // 🆕 數據索引常量
    private static final int DATA_CURRENT_MANA = 0;
    private static final int DATA_MAX_MANA = 1;
    private static final int DATA_PROGRESS = 2;
    private static final int DATA_MAX_PROGRESS = 3;
    private static final int DATA_IS_WORKING = 4;
    private static final int DATA_SIZE = 5;

    public ManaInfuserMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, null, new SimpleContainerData(DATA_SIZE));
    }

    // 🔧 修正伺服器端構造函數
    public ManaInfuserMenu(int id, Inventory playerInventory, ManaInfuserBlockEntity blockEntity) {
        this(id, playerInventory, blockEntity, createDataContainer(blockEntity));
    }

    // 🆕 統一的私有構造函數
    private ManaInfuserMenu(int id, Inventory playerInventory, ManaInfuserBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MANA_INFUSER.get(), id);

        this.blockEntity = blockEntity;
        this.data = data;
        this.access = blockEntity != null ?
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()) :
                ContainerLevelAccess.NULL;

        // 🆕 添加數據槽位追蹤
        this.addDataSlots(this.data);

        // 添加機器槽位
        if (blockEntity != null) {
            IItemHandler itemHandler = blockEntity.getItemHandler();
            if (itemHandler != null) {
                addMachineSlots(itemHandler);
            }
        }

        // 添加玩家背包槽位
        addPlayerSlots(playerInventory);
    }

    /**
     * 🔧 添加機器槽位
     */
    private void addMachineSlots(IItemHandler itemHandler) {
        // 輸入槽
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT, INPUT_SLOT_X, INPUT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // 檢查是否有對應的注入配方
                return blockEntity != null && hasRecipeForItem(stack);
            }
        });

        // 輸出槽（只能取出，不能放入）
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 輸出槽不允許手動放入
            }
        });
    }

    private static ContainerData createDataContainer(ManaInfuserBlockEntity blockEntity) {
        if (blockEntity == null) {
            return new SimpleContainerData(DATA_SIZE);
        }

        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_CURRENT_MANA -> blockEntity.getCurrentMana();
                    case DATA_MAX_MANA -> blockEntity.getMaxMana();
                    case DATA_PROGRESS -> blockEntity.getInfusionProgress();
                    case DATA_MAX_PROGRESS -> blockEntity.getMaxInfusionTime();
                    case DATA_IS_WORKING -> blockEntity.isWorking() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // 客戶端不需要設置數據，只讀取
                // 數據變更由伺服器端的 BlockEntity 負責
            }

            @Override
            public int getCount() {
                return DATA_SIZE;
            }
        };
    }

    // 🔧 修正數據查詢方法，使用同步的數據

    /**
     * 🔮 獲取魔力儲存狀態（用於GUI顯示）
     */
    public int getCurrentMana() {
        // ✅ 使用 ContainerData 的同步數據，不直接訪問 BlockEntity
        return data.get(DATA_CURRENT_MANA);
    }

    public int getMaxMana() {
        // ✅ 使用 ContainerData 的同步數據
        return data.get(DATA_MAX_MANA);
    }
    /**
     * ⚡ 獲取注入進度（用於進度條顯示）
     */
   
    public int getInfusionProgress() {
        // ✅ 使用 ContainerData 的同步數據
        return data.get(DATA_PROGRESS);
    }

    public int getMaxInfusionTime() {
        // ✅ 使用 ContainerData 的同步數據
        return data.get(DATA_MAX_PROGRESS);
    }

    /**
     * 🔧 獲取工作狀態
     */
    public boolean isWorking() {
        // ✅ 使用 ContainerData 的同步數據
        return data.get(DATA_IS_WORKING) == 1;
    }

    /**
     * 📦 獲取當前配方信息（只在伺服器端可用）
     */
    public ManaInfuserRecipe getCurrentRecipe() {
        // 配方信息太複雜，不通過 ContainerData 同步
        // 只在伺服器端使用
        return blockEntity != null ? blockEntity.getCurrentRecipe() : null;
    }

    /**
     * 📊 獲取魔力百分比（0-100）
     */
    public int getManaPercentage() {
        int maxMana = getMaxMana();
        if (maxMana <= 0) return 0;
        return Math.min(100, (getCurrentMana() * 100) / maxMana);
    }

    /**
     * ⚡ 獲取進度百分比（0-100）
     */
    public int getProgressPercentage() {
        int maxProgress = getMaxInfusionTime();
        if (maxProgress <= 0) return 0;
        return Math.min(100, (getInfusionProgress() * 100) / maxProgress);
    }


    /**
     * 🎒 添加玩家背包槽位
     */
    private void addPlayerSlots(Inventory playerInventory) {
        // 主背包（3x9 = 27 slots）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory,
                        col + row * 9 + 9, // slot index
                        INVENTORY_START_X + col * 18,
                        INVENTORY_START_Y + row * 18));
            }
        }

        // 快捷欄（1x9 = 9 slots）
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory,
                    col, // slot index (0-8)
                    INVENTORY_START_X + col * 18,
                    HOTBAR_START_Y));
        }
    }

    /**
     * 🔍 檢查物品是否有配方
     */
    private boolean hasRecipeForItem(ItemStack stack) {
        if (blockEntity == null || blockEntity.getLevel() == null) return false;

        ManaInfuserRecipe.ManaInfuserInput input = new ManaInfuserRecipe.ManaInfuserInput(stack);
        return blockEntity.getLevel().getRecipeManager()
                .getRecipeFor(ModRecipes.MANA_INFUSER_TYPE.get(), input, blockEntity.getLevel())
                .isPresent();
    }

    // === 🔄 物品轉移邏輯 ===

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            // 機器槽位數量
            int machineSlots = 2;

            if (index < machineSlots) {
                // 從機器槽位移動到玩家背包
                if (!this.moveItemStackTo(slotStack, machineSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 從玩家背包移動到機器槽位

                // 先嘗試放入輸入槽
                if (hasRecipeForItem(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    // === ✅ 驗證邏輯 ===

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.MANA_INFUSER.get());
    }

    // === 📊 數據同步 ===


}