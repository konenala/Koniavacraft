package com.github.nalamodikk.screen.ManaCrafting;

import com.github.nalamodikk.block.ModBlocks;
import com.github.nalamodikk.block.entity.mana_crafting.AdvancedManaCraftingTableBlockEntity;
import com.github.nalamodikk.recipe.ManaCraftingTableRecipe;
import com.github.nalamodikk.screen.ModMenusTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Optional;

public class AdvancedManaCraftingTableMenu extends AbstractContainerMenu {
    private final AdvancedManaCraftingTableBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final DataSlot manaStored = DataSlot.standalone();

    public static final int OUTPUT_SLOT = 9;
    public static final int INPUT_SLOT_START = 0;
    public static final int INPUT_SLOT_END = 8;

    public AdvancedManaCraftingTableMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (AdvancedManaCraftingTableBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    public AdvancedManaCraftingTableMenu(int id, Inventory inv, AdvancedManaCraftingTableBlockEntity blockEntity, ContainerData data) {
        super(ModMenusTypes.ADVANCED_MANA_CRAFTING_TABLE_MENu.get(), id);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = data;

        // 添加物品槽
        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // 添加魔力工作台的槽位
        IItemHandler itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(new ItemStackHandler(10));
        // 9 個輸入槽
        for (int i = 0; i < 9; i++) {
            this.addSlot(new SlotItemHandler(itemHandler, i, 30 + (i % 3) * 18, 17 + (i / 3) * 18));
        }
        // 1 個輸出槽
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 輸出槽不允許手動放入物品
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (blockEntity != null) {
                    // 獲取當前配方
                    Optional<ManaCraftingTableRecipe> recipe = blockEntity.getCurrentRecipe();

                    // 確保配方存在且魔力充足
                    if (recipe.isPresent() && blockEntity.hasSufficientMana(recipe.get().getManaCost())) {
                        blockEntity.consumeMana(recipe.get().getManaCost()); // 消耗魔力

                        // 消耗合成材料
                        for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                            blockEntity.getItemHandler().extractItem(i, 1, false);
                        }

                        // 自動更新合成結果
                        blockEntity.updateCraftingResult(); // 檢查是否能夠再次合成
                    }
                }
                super.onTake(player, stack);
            }
        });

        // 添加數據追蹤
        this.addDataSlots(data);
        // 添加魔力同步的 DataSlot
        this.addDataSlot(manaStored);
    }

    private boolean isAutoCrafting = false;

    public boolean isAutoCrafting() {
        return isAutoCrafting;
    }

    public void toggleAutoCrafting() {
        this.isAutoCrafting = !this.isAutoCrafting;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (blockEntity != null) {
            blockEntity.updateCraftingResult(); // 更新合成結果

            // 確保伺服器和客戶端同步
            blockEntity.setChanged();
            if (!blockEntity.getLevel().isClientSide()) {
                BlockState state = blockEntity.getLevel().getBlockState(blockEntity.getBlockPos());
                blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), state, state, 3);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack originalStack = stackInSlot.copy();

        // 如果是合成結果槽
        if (index == OUTPUT_SLOT) {
            if (blockEntity == null || !blockEntity.hasRecipe()) {
                return ItemStack.EMPTY;
            }

            // 嘗試將合成結果移到玩家的物品欄中
            if (!this.moveItemStackTo(stackInSlot, 10, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }

            // 執行批量合成的邏輯
            int maxCraftCount = Integer.MAX_VALUE;

            // 計算可以進行的最大合成次數
            Optional<ManaCraftingTableRecipe> currentRecipe = blockEntity.getCurrentRecipe();
            if (currentRecipe.isPresent()) {
                ManaCraftingTableRecipe recipe = currentRecipe.get();

                // 計算材料可以進行合成的最大次數
                for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                    ItemStack stack = blockEntity.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        int maxForThisSlot = stack.getCount() / 1; // 每次合成需要 1 個材料
                        maxCraftCount = Math.min(maxCraftCount, maxForThisSlot);
                    } else {
                        maxCraftCount = 0;
                        break; // 如果某個槽位沒有材料，無法進行合成
                    }
                }

                // 計算魔力可以進行合成的最大次數
                int maxManaCrafts = blockEntity.getManaStored() / recipe.getManaCost();
                maxCraftCount = Math.min(maxCraftCount, maxManaCrafts);

                // 進行批量合成
                for (int i = 0; i < maxCraftCount; i++) {
                    if (blockEntity.hasRecipe() && blockEntity.hasSufficientMana(recipe.getManaCost())) {
                        blockEntity.craftItem();
                        ItemStack resultStack = slot.getItem();
                        if (!this.moveItemStackTo(resultStack, 10, this.slots.size(), true)) {
                            break; // 如果無法移動合成結果，退出循環
                        }
                    } else {
                        break;
                    }
                }
            }

            slot.onQuickCraft(stackInSlot, originalStack);
        }
        // 如果是玩家物品欄的槽位
        else if (index >= 10) {
            // 嘗試將物品移動到合成材料槽
            if (!this.moveItemStackTo(stackInSlot, INPUT_SLOT_START, INPUT_SLOT_END + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        // 如果是合成網格的槽位
        else if (index >= INPUT_SLOT_START && index <= INPUT_SLOT_END) {
            // 將材料移動到玩家物品欄中
            if (!this.moveItemStackTo(stackInSlot, 10, this.slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stackInSlot.getCount() == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        return originalStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.ADVANCED_MANA_CRAFTING_TABLE_BLOCK.get());
    }

    public int getManaStored() {
        return manaStored.get();
    }

    public int getMaxMana() {
        return manaStored.get();
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
            int currentMana = blockEntity.getManaStored();
            manaStored.set(currentMana);  // 更新魔力值
        }
    }
}
