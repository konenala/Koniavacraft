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

    private final MachineSyncManager syncManager;

    // 本地緩存變數
    private int manaStored;
    private int maxMana;
    private boolean isGenerating;
    private int speedLevel;
    private int efficiencyLevel;
    private boolean isDaytime;
    
    // 🆕 追蹤是否已經同步過升級數據
    private boolean hasValidUpgradeData = false;

    public SolarCollectorSyncHelper() {
        this.syncManager = new MachineSyncManager();
        
        // 註冊追蹤變數
        syncManager.trackInt(() -> manaStored, v -> manaStored = v);
        syncManager.trackInt(() -> maxMana, v -> maxMana = v);
        syncManager.trackBoolean(() -> isGenerating, v -> isGenerating = v);
        syncManager.trackInt(() -> speedLevel, v -> speedLevel = v);
        syncManager.trackInt(() -> efficiencyLevel, v -> efficiencyLevel = v);
        syncManager.trackBoolean(() -> isDaytime, v -> isDaytime = v);
    }

    /**
     * 將 block entity 當前狀態同步至同步欄位
     */
    public void syncFrom(SolarManaCollectorBlockEntity be) {
        this.manaStored = be.getManaStored();
        this.maxMana = SolarManaCollectorBlockEntity.getMaxMana();
        this.isGenerating = be.isCurrentlyGenerating();

        // 🔧 關鍵修復：直接從升級管理器獲取數據
        this.speedLevel = be.getUpgradeInventory().getUpgradeCount(UpgradeType.SPEED);
        this.efficiencyLevel = be.getUpgradeInventory().getUpgradeCount(UpgradeType.EFFICIENCY);

        this.isDaytime = be.isDaytime();
        
        // 🆕 標記已經有有效的升級數據
        this.hasValidUpgradeData = true;
    }
    
    // 🆕 獲取時間狀態
    public boolean isDaytime() {
        return isDaytime;
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

    // 舊方法的相容適配
    public void setGeneratingState(SolarManaCollectorBlockEntity be, int value) {
        be.setCurrentlyGenerating(value != 0);
    }

    public int getManaStored() {
        return manaStored;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public boolean isGenerating() {
        return isGenerating;
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public int getEfficiencyLevel() {
        return efficiencyLevel;
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
