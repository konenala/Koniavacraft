package com.github.nalamodikk.common.screen.player;


import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ExtraEquipmentMenu extends AbstractContainerMenu {

    public ExtraEquipmentMenu(int syncId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(ModMenuTypes.EXTRA_EQUIPMENT_MENU.get(), syncId, playerInventory);
    }
    public ExtraEquipmentMenu(int syncId, Inventory playerInventory) {
        this(ModMenuTypes.EXTRA_EQUIPMENT_MENU.get(), syncId, playerInventory);
    }

    public ExtraEquipmentMenu(MenuType<?> type, int syncId, Inventory playerInventory) {
        super(type, syncId);

        // Player inventory slots
        int slotIndex = 0;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, slotIndex + 9, 8 + col * 18, 84 + row * 18));
                slotIndex++;
            }
        }

        // Hotbar slots
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        // Placeholder: Add 12 custom armor slots here（之後會補上）
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // 目前不綁定方塊，可直接開啟
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // TODO: 可實作 shift-click 邏輯
    }
}
