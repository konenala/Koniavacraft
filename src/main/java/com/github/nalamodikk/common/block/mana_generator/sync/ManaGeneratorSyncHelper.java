
package com.github.nalamodikk.common.block.mana_generator.sync;

import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.sync.MachineSyncManager;
import net.minecraft.world.inventory.ContainerData;

import com.github.nalamodikk.common.sync.ISyncHelper;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 管理 ManaGeneratorBlockEntity 的同步欄位。
 * 實際同步邏輯委託給 MachineSyncManager，但保留語意明確的 index 與對應欄位。
 * 同時支援欄位變化比對與 ISyncHelper 統一架構。
 */
public class ManaGeneratorSyncHelper implements ISyncHelper {
    public static final int MANA_STORED_INDEX = 0;
    public static final int ENERGY_STORED_INDEX = 1;
    public static final int MODE_INDEX = 2;
    public static final int BURN_TIME_INDEX = 3;
    public static final int CURRENT_BURN_TIME_INDEX = 4;
    public static final int IS_WORKING_INDEX = 5;
    public static final int IS_PAUSED_INDEX = 6;
    public static final int SYNC_DATA_COUNT = 7;

    private final MachineSyncManager syncManager = new MachineSyncManager();
    private final int[] lastSyncedValues = new int[SYNC_DATA_COUNT];


    private void setIfChanged(int index, int newValue) {
        if (lastSyncedValues[index] != newValue) {
            lastSyncedValues[index] = newValue;
            syncManager.set(index, newValue);
        }
    }


    public void syncFrom(ManaGeneratorBlockEntity be) {
        setIfChanged(MANA_STORED_INDEX, be.getManaStorage() != null ? be.getManaStorage().getManaStored() : 0);
        setIfChanged(ENERGY_STORED_INDEX, be.getEnergyStorage() != null ? be.getEnergyStorage().getEnergyStored() : 0);
        setIfChanged(MODE_INDEX, be.getCurrentMode());
        setIfChanged(BURN_TIME_INDEX, be.getBurnTime());
        setIfChanged(CURRENT_BURN_TIME_INDEX, be.getCurrentBurnTime());
        setIfChanged(IS_WORKING_INDEX, be.isWorking() ? 1 : 0);
        setIfChanged(IS_PAUSED_INDEX, be.getFuelLogic().isPaused() ? 1 : 0);
    }

    @Override
    public void syncFrom(BlockEntity be) {
        if (be instanceof ManaGeneratorBlockEntity generator) {
            syncFrom(generator); // 呼叫你的真正邏輯
        }
    }


    @Override
    public ContainerData getContainerData() {
        return syncManager;
    }

    public void setModeIndex(int modeIndex) {
        setIfChanged(MODE_INDEX, modeIndex);
    }

    public MachineSyncManager getRawSyncManager() {
        return syncManager;
    }
}
