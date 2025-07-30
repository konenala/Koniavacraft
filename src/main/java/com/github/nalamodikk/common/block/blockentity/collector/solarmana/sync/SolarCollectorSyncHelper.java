package com.github.nalamodikk.common.block.blockentity.collector.solarmana.sync;

import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.common.sync.ISyncHelper;
import com.github.nalamodikk.common.sync.MachineSyncManager;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import com.mojang.logging.LogUtils;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

/**
 * 用來同步 SolarManaCollectorBlockEntity 的 mana 數值與發電狀態。
 */
public class SolarCollectorSyncHelper implements ISyncHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    public enum SyncIndex {
        MANA,
        MAX_MANA,
        GENERATING,
        SPEED_LEVEL,
        EFFICIENCY_LEVEL,
        IS_DAYTIME; // 🆕 添加時間狀態同步

        public static int count() {
            return values().length;
        }
    }

    private final MachineSyncManager syncManager = new MachineSyncManager(SyncIndex.count());

    // 🆕 追蹤是否已經同步過升級數據
    private boolean hasValidUpgradeData = false;

    /**
     * 將 block entity 當前狀態同步至同步欄位
     */
    public void syncFrom(SolarManaCollectorBlockEntity be) {
        syncManager.set(SyncIndex.MANA.ordinal(), be.getManaStored());
        syncManager.set(SyncIndex.MAX_MANA.ordinal(), SolarManaCollectorBlockEntity.getMaxMana());
        syncManager.set(SyncIndex.GENERATING.ordinal(), be.isCurrentlyGenerating() ? 1 : 0);

        // 🔧 關鍵修復：直接從升級管理器獲取數據
        int speedCount = be.getUpgradeInventory().getUpgradeCount(UpgradeType.SPEED);
        int effCount = be.getUpgradeInventory().getUpgradeCount(UpgradeType.EFFICIENCY);

        syncManager.set(SyncIndex.SPEED_LEVEL.ordinal(), speedCount);
        syncManager.set(SyncIndex.EFFICIENCY_LEVEL.ordinal(), effCount);

        // 🆕 標記已經有有效的升級數據
        this.hasValidUpgradeData = true;
        syncManager.set(SyncIndex.IS_DAYTIME.ordinal(), be.isDaytime() ? 1 : 0);

        // 🔍 調試輸出
    }
    // 🆕 獲取時間狀態
    public boolean isDaytime() {
        return syncManager.get(SyncIndex.IS_DAYTIME.ordinal()) != 0;
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

    // 🆕 檢查是否有有效的升級數據
    public boolean hasValidUpgradeData() {
        return hasValidUpgradeData;
    }

    // 🆕 重置同步狀態（用於新的 Menu 創建）
    public void resetSyncState() {
        hasValidUpgradeData = false;
    }
}
