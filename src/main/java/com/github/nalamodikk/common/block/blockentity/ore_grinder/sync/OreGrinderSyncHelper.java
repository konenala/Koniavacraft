package com.github.nalamodikk.common.block.blockentity.ore_grinder.sync;

import com.github.nalamodikk.common.block.blockentity.ore_grinder.OreGrinderBlockEntity;
import com.github.nalamodikk.common.sync.ISyncHelper;
import com.github.nalamodikk.common.sync.MachineSyncManager;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

public class OreGrinderSyncHelper implements ISyncHelper {
    private final MachineSyncManager syncManager;
    
    // 緩存變數
    private int mana;
    private int progress;
    private int maxProgress;

    public OreGrinderSyncHelper() {
        this.syncManager = new MachineSyncManager();
        syncManager.trackInt(() -> mana, v -> mana = v);
        syncManager.trackInt(() -> progress, v -> progress = v);
        syncManager.trackInt(() -> maxProgress, v -> maxProgress = v);
    }

    public void syncFrom(OreGrinderBlockEntity be) {
        this.mana = be.getManaStorage() != null ? be.getManaStorage().getManaStored() : 0;
        this.progress = be.getProgress();
        this.maxProgress = be.getMaxProgress();
    }

    @Override
    public void syncFrom(BlockEntity be) {
        if (be instanceof OreGrinderBlockEntity grinder) {
            syncFrom(grinder);
        }
    }

    @Override
    public ContainerData getContainerData() {
        return syncManager;
    }

    public int getMana() { return mana; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
}
