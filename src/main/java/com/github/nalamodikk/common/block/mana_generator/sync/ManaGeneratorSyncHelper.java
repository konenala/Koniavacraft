package com.github.nalamodikk.common.block.mana_generator.sync;

import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.sync.ISyncHelper;
import com.github.nalamodikk.common.sync.MachineSyncManager;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 管理 ManaGeneratorBlockEntity 的同步欄位。
 * 使用 enum 管理同步 index，提高語意與維護性。
 */
public class ManaGeneratorSyncHelper implements ISyncHelper {

    public enum SyncIndex {
        MANA,
        ENERGY,
        MODE,
        BURN_TIME,
        CURRENT_BURN_TIME,
        IS_WORKING,
        IS_PAUSED;

        public static int count() {
            return values().length;
        }
    }

    private final MachineSyncManager syncManager = new MachineSyncManager(SyncIndex.count());
    private final boolean[] dirtyFlags = new boolean[SyncIndex.count()];

    private final int[] lastSyncedValues = new int[SyncIndex.count()];



    private void setIfChanged(SyncIndex index, int newValue) {
        int ordinal = index.ordinal();
        if (lastSyncedValues[ordinal] != newValue) {
            lastSyncedValues[ordinal] = newValue;
            syncManager.set(ordinal, newValue);
            dirtyFlags[ordinal] = true; // ✅ 資料有變才標記為 dirty
        }
    }


    public void syncFrom(ManaGeneratorBlockEntity be) {
        setIfChanged(SyncIndex.MANA, be.getManaStorage() != null ? be.getManaStorage().getManaStored() : 0);
        setIfChanged(SyncIndex.ENERGY, be.getEnergyStorage() != null ? be.getEnergyStorage().getEnergyStored() : 0);
        setIfChanged(SyncIndex.MODE, be.getCurrentMode());
        setIfChanged(SyncIndex.BURN_TIME, be.getBurnTime());
        setIfChanged(SyncIndex.CURRENT_BURN_TIME, be.getCurrentBurnTime());
        setIfChanged(SyncIndex.IS_WORKING, be.isWorking() ? 1 : 0);
        setIfChanged(SyncIndex.IS_PAUSED, be.getFuelLogic().isPaused() ? 1 : 0);
    }

    @Override
    public void syncFrom(BlockEntity be) {
        if (be instanceof ManaGeneratorBlockEntity generator) {
            syncFrom(generator);
        }
    }

    @Override
    public ContainerData getContainerData() {
        return syncManager;
    }

    public void setModeIndex(int modeIndex) {
        setIfChanged(SyncIndex.MODE, modeIndex);
    }

    public MachineSyncManager getRawSyncManager() {
        return syncManager;
    }

    public boolean hasDirty() {
        for (boolean flag : dirtyFlags) {
            if (flag) return true;
        }
        return false;
    }

    public void flushSyncState(BlockEntity be) {
        if (hasDirty()) {
            clearDirty();
        }
    }

    private void clearDirty() {
        for (int i = 0; i < dirtyFlags.length; i++) {
            dirtyFlags[i] = false;
        }
    }

}
