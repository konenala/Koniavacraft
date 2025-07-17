package com.github.nalamodikk.common.block.blockentity.collector.solarmana;


import com.github.nalamodikk.common.block.blockentity.collector.solarmana.sync.SolarCollectorSyncHelper;
import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
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
        this.syncHelper = blockEntity.getSyncHelper();

        this.syncHelper.syncFrom(blockEntity);
        this.addDataSlots(syncHelper.getContainerData()); // ✅ 現在就找得到方法了

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
        super(ModMenuTypes.SOLAR_MANA_COLLECTOR_MENU.get(), id);

        // 📍 獲取 BlockEntity
        this.blockEntity = (SolarManaCollectorBlockEntity) inv.player.level()
                .getBlockEntity(buf.readBlockPos());
        this.access = ContainerLevelAccess.create(inv.player.level(), blockEntity.getBlockPos());

        // 🎯 客戶端：創建假同步器，網路數據會覆蓋
        this.syncHelper = new SolarCollectorSyncHelper();

        // 📊 客戶端使用 SimpleContainerData，會被伺服器數據覆蓋
        this.addDataSlots(new SimpleContainerData(SolarCollectorSyncHelper.SyncIndex.count()));

        addPlayerInventorySlots(inv, 8, 84);
        addPlayerHotbarSlots(inv, 8, 142);
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

    public int getSpeedLevel() {
        return syncHelper.getRawSyncManager().get(SolarCollectorSyncHelper.SyncIndex.SPEED_LEVEL.ordinal());
    }

    public int getEfficiencyLevel() {
        return syncHelper.getRawSyncManager().get(SolarCollectorSyncHelper.SyncIndex.EFFICIENCY_LEVEL.ordinal());
    }


}
