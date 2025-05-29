
package com.github.nalamodikk.common.block.mana_generator.sync;

import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.sync.UnifiedSyncManager;
import net.minecraft.world.inventory.ContainerData;

public class ManaGeneratorSyncHelper {
    public static final int MANA_STORED_INDEX = 0;
    public static final int ENERGY_STORED_INDEX = 1;
    public static final int MODE_INDEX = 2;
    public static final int BURN_TIME_INDEX = 3;
    public static final int CURRENT_BURN_TIME_INDEX = 4;
    public static final int SYNC_DATA_COUNT = 5;
    public static final int IS_WORKING_INDEX = 6;
    public static final int IS_PAUSED_INDEX = 7;

    private final UnifiedSyncManager syncManager = new UnifiedSyncManager(SYNC_DATA_COUNT);

    public void syncFrom(ManaGeneratorBlockEntity be) {
        syncManager.set(MANA_STORED_INDEX, be.getManaStorage() != null ? be.getManaStorage().getManaStored() : 0);
        syncManager.set(ENERGY_STORED_INDEX, be.getEnergyStorage() != null ? be.getEnergyStorage().getEnergyStored() : 0);
        syncManager.set(MODE_INDEX, be.getCurrentMode());
        syncManager.set(BURN_TIME_INDEX, be.getBurnTime());
        syncManager.set(CURRENT_BURN_TIME_INDEX, be.getCurrentBurnTime());
        syncManager.set(IS_WORKING_INDEX, be.isWorking() ? 1 : 0);
        syncManager.set(IS_PAUSED_INDEX, be.getFuelLogic().isPaused() ? 1 : 0);

    }

    public ContainerData getContainerData() {
        return syncManager.getContainerData();
    }

    public void setModeIndex(int modeIndex) {
        syncManager.set(MODE_INDEX, modeIndex);
    }

    public UnifiedSyncManager getRawSyncManager() {
        return syncManager;
    }
}
