package com.github.nalamodikk.common.block.blockentity.mana_generator.sync;

import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.sync.ISyncHelper;
import com.github.nalamodikk.common.sync.MachineSyncManager;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/**
 * 管理 ManaGeneratorBlockEntity 的同步欄位。
 * 使用 enum 管理同步 index，提高語意與維護性。
 */
public class ManaGeneratorSyncHelper implements ISyncHelper {
    private int syncCounter = 0;
    private static final int FORCE_SYNC_INTERVAL = 20; // 每20 tick強制同步一次

    public enum SyncIndex {
        MANA,
        ENERGY,
        MODE,
        BURN_TIME,
        CURRENT_BURN_TIME,
        IS_WORKING,
        IS_PAUSED,
        // 💡 新增欄位
        HAS_DIAGNOSTIC_DISPLAY,
        MANA_RATE,
        ENERGY_RATE;

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
    private void forceSync(ManaGeneratorBlockEntity be) {
        syncManager.set(SyncIndex.MANA.ordinal(), be.getManaStorage() != null ? be.getManaStorage().getManaStored() : 0);
        syncManager.set(SyncIndex.ENERGY.ordinal(), be.getEnergyStorage() != null ? be.getEnergyStorage().getEnergyStored() : 0);
        syncManager.set(SyncIndex.MODE.ordinal(), be.getCurrentMode());
        syncManager.set(SyncIndex.BURN_TIME.ordinal(), be.getBurnTime());
        syncManager.set(SyncIndex.CURRENT_BURN_TIME.ordinal(), be.getCurrentBurnTime());
        syncManager.set(SyncIndex.IS_WORKING.ordinal(), be.isWorking() ? 1 : 0);
        syncManager.set(SyncIndex.IS_PAUSED.ordinal(), be.getFuelLogic().isPaused() ? 1 : 0);

        // 💡 新增欄位
        syncManager.set(SyncIndex.HAS_DIAGNOSTIC_DISPLAY.ordinal(), be.getUpgradeHandler().hasDiagnosticDisplay() ? 1 : 0);
        Optional<ManaGenFuelRateLoader.FuelRate> currentRate = be.getCurrentFuelRate();
        syncManager.set(SyncIndex.MANA_RATE.ordinal(), currentRate.map(ManaGenFuelRateLoader.FuelRate::getManaRate).orElse(0));
        syncManager.set(SyncIndex.ENERGY_RATE.ordinal(), currentRate.map(ManaGenFuelRateLoader.FuelRate::getEnergyRate).orElse(0));

        // 清除所有dirty flags，因為我們已經強制更新了
        clearDirty();
    }

    public void syncFrom(ManaGeneratorBlockEntity be) {
        setIfChanged(SyncIndex.MANA, be.getManaStorage() != null ? be.getManaStorage().getManaStored() : 0);
        setIfChanged(SyncIndex.ENERGY, be.getEnergyStorage() != null ? be.getEnergyStorage().getEnergyStored() : 0);
        setIfChanged(SyncIndex.MODE, be.getCurrentMode());
        setIfChanged(SyncIndex.BURN_TIME, be.getBurnTime());
        setIfChanged(SyncIndex.CURRENT_BURN_TIME, be.getCurrentBurnTime());
        setIfChanged(SyncIndex.IS_WORKING, be.isWorking() ? 1 : 0);
        setIfChanged(SyncIndex.IS_PAUSED, be.getFuelLogic().isPaused() ? 1 : 0);

        // 💡 新增欄位
        setIfChanged(SyncIndex.HAS_DIAGNOSTIC_DISPLAY, be.getUpgradeHandler().hasDiagnosticDisplay() ? 1 : 0);
        Optional<ManaGenFuelRateLoader.FuelRate> currentRate = be.getCurrentFuelRate();
        setIfChanged(SyncIndex.MANA_RATE, currentRate.map(ManaGenFuelRateLoader.FuelRate::getManaRate).orElse(0));
        setIfChanged(SyncIndex.ENERGY_RATE, currentRate.map(ManaGenFuelRateLoader.FuelRate::getEnergyRate).orElse(0));

        syncCounter++;
        if (syncCounter >= FORCE_SYNC_INTERVAL) {
            syncCounter = 0;
            forceSync(be); // 強制更新所有數據
        }
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
