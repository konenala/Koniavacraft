package com.github.nalamodikk.common.block.collector.manacollector;


import com.github.nalamodikk.common.block.collector.manacollector.sync.SolarCollectorSyncHelper;
import com.github.nalamodikk.common.register.ModMenuTypes;
import com.github.nalamodikk.common.utils.upgrade.UpgradeSlot;
import com.github.nalamodikk.common.utils.upgrade.api.IUpgradeableMachine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;


public class SolarManaCollectorMenu extends AbstractContainerMenu {
    private final SolarManaCollectorBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final SolarCollectorSyncHelper syncHelper;


    public SolarManaCollectorMenu(int id, Inventory inv, SolarManaCollectorBlockEntity blockEntity) {
        super(ModMenuTypes.SOLAR_MANA_COLLECTOR_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.syncHelper = new SolarCollectorSyncHelper();

        this.syncHelper.syncFrom(blockEntity);
        this.addDataSlots(syncHelper.getContainerData()); // ✅ 現在就找得到方法了
        if (blockEntity instanceof IUpgradeableMachine machine) {
            Container upgrades = machine.getUpgradeInventory();
            checkContainerSize(upgrades, 4);
            this.addSlot(new UpgradeSlot(upgrades, 0, 47, 37));
            this.addSlot(new UpgradeSlot(upgrades, 1, 65, 37));
            this.addSlot(new UpgradeSlot(upgrades, 2, 83, 37));
            this.addSlot(new UpgradeSlot(upgrades, 3, 101, 37));
        }
        addPlayerInventorySlots(inv, 8, 84);
        addPlayerHotbarSlots(inv, 8, 142);
    }

    public void addPlayerInventorySlots(Inventory inv, int startX, int startY) {
        // 玩家主背包 (3x9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9 + 9;
                int x = startX + col * 18;
                int y = startY + row * 18;
                this.addSlot(new Slot(inv, index, x, y));
            }
        }
    }

    public void addPlayerHotbarSlots(Inventory inv, int startX, int startY) {
        // 玩家快捷欄 (1x9)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, startX + col * 18, startY));
        }
    }

    public SolarManaCollectorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (SolarManaCollectorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, blockEntity.getBlockState().getBlock());
    }

    public SolarManaCollectorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getManaStored() {
        return syncHelper.getManaStored();
    }

    public int getMaxMana() {
        return syncHelper.getMaxMana();
    }

    public boolean isGenerating() {
        return syncHelper.isGenerating();
    }

}
