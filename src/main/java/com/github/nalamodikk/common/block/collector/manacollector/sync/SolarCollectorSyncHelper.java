package com.github.nalamodikk.common.block.collector.manacollector.sync;

import com.github.nalamodikk.common.block.collector.manacollector.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.common.sync.MachineSyncManager;
import net.minecraft.world.inventory.ContainerData;

/**
 * 用來同步 SolarManaCollectorBlockEntity 的 mana 數值與發電狀態。
 */
public class SolarCollectorSyncHelper {

    public enum SyncIndex {
        MANA,
        MAX_MANA,
        GENERATING;

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
