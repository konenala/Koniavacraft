package com.github.nalamodikk.common.block.mana_generator;

import com.github.nalamodikk.common.block.mana_generator.sync.ManaGeneratorSyncHelper;
import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;


public class ManaGeneratorMenu extends AbstractContainerMenu {
    private final ManaGeneratorBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ManaGeneratorSyncHelper syncHelper;

    public ManaGeneratorMenu(int id, Inventory inv, ManaGeneratorBlockEntity blockEntity) {
        super(ModMenuTypes.MANA_GENERATOR_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.syncHelper = blockEntity.getSyncHelper(); // ✅ 使用 BE 傳過來的同步器

        this.addDataSlots(syncHelper.getContainerData());

        IItemHandler blockInventory = blockEntity.getInventory();
        this.addSlot(new SlotItemHandler(blockInventory, 0, 80, 40) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return true; // 允許所有物品放入
            }

        });

        layoutPlayerInventorySlots(inv, 8, 84);
    }

    private void layoutPlayerInventorySlots(Inventory playerInventory, int leftCol, int topRow) {
        // 玩家物品欄槽位 (3行)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, leftCol + col * 18, topRow + row * 18));
            }
        }

        // 玩家快捷欄槽位 (1行)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, leftCol + col * 18, topRow + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            if (index < 1) { // 如果是在方塊槽位
                if (!this.moveItemStackTo(stackInSlot, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) { // 如果是在玩家槽位
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    public int getMaxMana() {
        return ManaGeneratorBlockEntity.getMaxMana();
    }

    public int getMaxEnergy() {
        return ManaGeneratorBlockEntity.getMaxEnergy();
    }


    public BlockPos getBlockEntityPos() {
        return this.blockEntity.getBlockPos();
    }

    public void toggleCurrentMode() {
        int currentMode = this.getCurrentMode();
        syncHelper.getContainerData().set(ManaGeneratorBlockEntity.getModeIndex(), currentMode == 0 ? 1 : 0);
    }

    public void saveModeState() {
        if (blockEntity != null) {
            blockEntity.markUpdated(); // 確保保存當前模式到世界中
        }
    }

    public int getCurrentMode() {
        return syncHelper.getContainerData().get(ManaGeneratorBlockEntity.getModeIndex());
    }

    public int getManaStored() {
        return syncHelper.getContainerData().get(ManaGeneratorBlockEntity.getManaStoredIndex());
    }

    public int getEnergyStored() {
        return syncHelper.getContainerData().get(ManaGeneratorBlockEntity.getEnergyStoredIndex());
    }

    public int getBurnTime() {
        return syncHelper.getContainerData().get(ManaGeneratorBlockEntity.getBurnTimeIndex());
    }

    public int getCurrentBurnTime() {
        return syncHelper.getContainerData().get(ManaGeneratorBlockEntity.getCurrentBurnTimeIndex());
    }

}
