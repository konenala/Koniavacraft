package com.github.nalamodikk.common.screen.player;


import com.github.nalamodikk.register.ModMenuTypes;
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

    public static final int EQUIPMENT_SLOT_COUNT = 8;


    public ExtraEquipmentMenu(int syncId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(ModMenuTypes.EXTRA_EQUIPMENT_MENU.get(), syncId, playerInventory, new SimpleContainer(EQUIPMENT_SLOT_COUNT));
    }

    public ExtraEquipmentMenu(int syncId, Inventory playerInventory) {
        this(ModMenuTypes.EXTRA_EQUIPMENT_MENU.get(), syncId, playerInventory, new SimpleContainer(EQUIPMENT_SLOT_COUNT));
    }

    public ExtraEquipmentMenu(MenuType<?> type, int syncId, Inventory playerInventory, Container equipmentHandler) {
        super(type, syncId);
        // 新增玩家欄位
        addPlayerInventorySlots(playerInventory, 8, 170);
        //新增裝備欄位
        addEquipmentSlots(equipmentHandler, 36, 22, 194, 111);
    }

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

    protected void addEquipmentSlots(Container handler, int leftX, int leftY, int rightX, int rightY) {
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(handler, i, leftX, leftY + i * 18));
        }
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(handler, 4 + i, rightX, rightY + i * 18));
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
}
