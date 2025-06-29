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
    private final NonNullList<ItemStack> extraEquipmentRef; // 新增這個
    private final Container nineGridHandler; // 加上這一行！


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

        // === 飾品裝備欄位的同步機制 ===
        NonNullList<ItemStack> extraEquipment = player.getData(ModDataAttachments.EXTRA_EQUIPMENT.get());
        if (extraEquipment == null) {
            extraEquipment = NonNullList.withSize(8, ItemStack.EMPTY);
            player.setData(ModDataAttachments.EXTRA_EQUIPMENT.get(), extraEquipment);
        }
        this.extraEquipmentRef = extraEquipment;

        // 建立有同步功能的 handler
        Container extraSlotHandler = new SimpleContainer(extraEquipment.size()) {
            @Override
            public void setChanged() {
                for (int i = 0; i < this.getContainerSize(); i++) {
                    extraEquipmentRef.set(i, this.getItem(i));
                }
                player.setData(ModDataAttachments.EXTRA_EQUIPMENT.get(), extraEquipmentRef);
                super.setChanged();
            }
        };

        // 初始化 handler 內容
        for (int i = 0; i < extraEquipment.size(); i++) {
            extraSlotHandler.setItem(i, extraEquipment.get(i));
        }

        // 新增裝備欄位（使用有同步功能的 handler）
        addExtraEquipmentSlots(extraSlotHandler, 61, 23);

        // === 9格儲存欄位的同步機制 ===
        NonNullList<ItemStack> grid = player.getData(ModDataAttachments.NINE_GRID.get());
        if (grid == null) {
            grid = NonNullList.withSize(NINE_GRID_SLOT_COUNT, ItemStack.EMPTY);
            player.setData(ModDataAttachments.NINE_GRID.get(), grid);
        }
        this.gridRef = grid;

        // 建立 handler 並覆寫 setChanged 實作同步回 Attachment
        this.nineGridHandler = new SimpleContainer(grid.size()) {
            @Override
            public void setChanged() {
                for (int i = 0; i < this.getContainerSize(); i++) {
                    gridRef.set(i, this.getItem(i));
                }
                player.setData(ModDataAttachments.NINE_GRID.get(), gridRef);
                super.setChanged();
            }
        };

        for (int i = 0; i < grid.size(); i++) {
            this.nineGridHandler.setItem(i, grid.get(i));
        }

        addNineGridSlots(this.nineGridHandler, 176, 170);
    }
    //新增玩家欄位
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

    protected void addVanillaEquipmentSlots(Inventory playerInventory, int baseX, int baseY) {
        // 原版裝備欄位：頭盔、胸甲、腿甲、靴子
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(playerInventory, 39 - i, baseX, baseY + i * 18));
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

    // 新增裝備儲存欄位(想法來自魔法金屬manametalmpd)
    protected void addNineGridSlots(Container handler, int baseX, int baseY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                this.addSlot(new Slot(handler, index, baseX + col * 18, baseY + row * 18));
            }
        }
    }



    @Override
    public void removed(Player player) {
        super.removed(player);
        // 不再需要寫回，因為已在 setChanged() 時即時同步
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
