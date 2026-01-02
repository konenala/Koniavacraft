package com.github.nalamodikk.common.block.blockentity.mana_infuser.sync;

import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserBlockEntity;
import com.github.nalamodikk.common.sync.ISyncHelper;
import com.github.nalamodikk.common.sync.MachineSyncManager;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ManaInfuserSyncHelper implements ISyncHelper {
    private final MachineSyncManager syncManager;
    
    // 緩存變數
    private int currentMana;
    private int maxMana;
    private int progress;
    private int maxProgress;
    private boolean isWorking;

    public ManaInfuserSyncHelper() {
        this.syncManager = new MachineSyncManager();
        syncManager.trackInt(() -> currentMana, v -> currentMana = v);
        syncManager.trackInt(() -> maxMana, v -> maxMana = v);
        syncManager.trackInt(() -> progress, v -> progress = v);
        syncManager.trackInt(() -> maxProgress, v -> maxProgress = v);
        syncManager.trackBoolean(() -> isWorking, v -> isWorking = v);
    }

    public void syncFrom(ManaInfuserBlockEntity be) {
        this.currentMana = be.getCurrentMana();
        this.maxMana = be.getMaxMana();
        this.progress = be.getInfusionProgress();
        this.maxProgress = be.getMaxInfusionTime();
        this.isWorking = be.isWorking();
    }

    @Override
    public void syncFrom(BlockEntity be) {
        if (be instanceof ManaInfuserBlockEntity infuser) {
            syncFrom(infuser);
        }
    }

    @Override
    public ContainerData getContainerData() {
        return syncManager;
    }

    public int getCurrentMana() { return currentMana; }
    public int getMaxMana() { return maxMana; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public boolean isWorking() { return isWorking; }
}
