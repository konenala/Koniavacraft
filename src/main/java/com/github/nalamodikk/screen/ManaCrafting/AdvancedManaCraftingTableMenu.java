package com.github.nalamodikk.screen.ManaCrafting;

import com.github.nalamodikk.block.ModBlocks;
import com.github.nalamodikk.block.entity.mana_crafting.AdvancedManaCraftingTableBlockEntity;
import com.github.nalamodikk.screen.ModMenusTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class AdvancedManaCraftingTableMenu extends AbstractContainerMenu {
    private final AdvancedManaCraftingTableBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

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
        IItemHandler itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(() -> new IllegalStateException("Missing item handler capability"));
        // 9 個輸入槽
        for (int i = 0; i < 9; i++) {
            this.addSlot(new SlotItemHandler(itemHandler, i, 30 + (i % 3) * 18, 17 + (i / 3) * 18));
        }
        // 1 個輸出槽
        this.addSlot(new SlotItemHandler(itemHandler, 9, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // 添加數據追蹤
        this.addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (pIndex < 36) { // Player inventory slots
                if (!this.moveItemStackTo(itemstack1, 36, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 36, false)) { // Block inventory slots
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.ADVANCED_MANA_CRAFTING_TABLE_BLOCK.get());
    }

    public int getManaStored() {
        return this.data.get(0);
    }

    public int getMaxMana() {
        return this.data.get(1);
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
}
