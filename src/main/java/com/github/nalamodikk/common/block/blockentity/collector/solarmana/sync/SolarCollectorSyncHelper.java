package com.github.nalamodikk.common.block.blockentity.collector.solarmana.sync;

import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.common.sync.ISyncHelper;
import com.github.nalamodikk.common.sync.MachineSyncManager;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 用來同步 SolarManaCollectorBlockEntity 的 mana 數值與發電狀態。
 */
public class SolarCollectorSyncHelper implements ISyncHelper {

    public enum SyncIndex {
        MANA,
        MAX_MANA,
        GENERATING,
        SPEED_LEVEL,
        EFFICIENCY_LEVEL;

        public static int count() {
            return values().length;
        }
    }

    private final MachineSyncManager syncManager = new MachineSyncManager(SyncIndex.count());

    /**
     * 將 block entity 當前狀態同步至同步欄位
     */
    public void syncFrom(SolarManaCollectorBlockEntity be) {
        syncManager.set(SyncIndex.MANA.ordinal(), be.getManaStored());
        syncManager.set(SyncIndex.MAX_MANA.ordinal(), SolarManaCollectorBlockEntity.getMaxMana());
        syncManager.set(SyncIndex.GENERATING.ordinal(), be.isCurrentlyGenerating() ? 1 : 0);
        syncManager.set(SyncIndex.SPEED_LEVEL.ordinal(), be.getUpgradeInventory().getUpgradeCount(UpgradeType.SPEED));
        syncManager.set(SyncIndex.EFFICIENCY_LEVEL.ordinal(), be.getUpgradeInventory().getUpgradeCount(UpgradeType.EFFICIENCY));
    }

    @Override
    public void syncFrom(BlockEntity be) {
        if (be instanceof SolarManaCollectorBlockEntity solarMana) {
           this.syncFrom(solarMana);
        }
    }

    public ContainerData getContainerData() {
        return syncManager;
    }

    public void setGeneratingState(SolarManaCollectorBlockEntity be, int value) {
        be.setCurrentlyGenerating(value != 0);
    }

    public int getManaStored() {
        return syncManager.get(SyncIndex.MANA.ordinal());
    }

    public int getMaxMana() {
        return syncManager.get(SyncIndex.MAX_MANA.ordinal());
    }

    public boolean isGenerating() {
        return syncManager.get(SyncIndex.GENERATING.ordinal()) != 0;
    }


    public MachineSyncManager getRawSyncManager() {
        return syncManager;
    }
}
