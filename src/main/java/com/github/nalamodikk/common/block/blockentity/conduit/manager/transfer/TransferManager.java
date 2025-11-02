package com.github.nalamodikk.common.block.blockentity.conduit.manager.transfer;

import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.block.blockentity.conduit.ConduitTier;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.core.CacheManager;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.core.IOManager;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.core.StatsManager;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.network.BalancingStrategy;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.network.NetworkManager;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 導管傳輸管理器
 * 負責處理魔力傳輸邏輯、負載平衡和防循環
 */
public class TransferManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferManager.class);

    // === 常量 ===
    // ⚠️ 已棄用：使用 ConduitTier 系統取代固定速率
    @Deprecated
    private static final int LEGACY_TRANSFER_RATE = 200;
    private static final int MAX_TRANSFERS_PER_TICK = 2;

    // === 組件引用 ===
    private final ArcaneConduitBlockEntity conduit;
    private final NetworkManager networkManager;
    private final StatsManager statsManager;
    private final IOManager ioManager;

    // === 傳輸狀態 ===
    private Direction lastReceiveDirection = null;
    private Direction lastTransferDirection = null;
    private long lastTransferTick = 0;
    private final Set<Direction> busyDirections = EnumSet.noneOf(Direction.class);
    private int transfersThisTick = 0;

    // === 🆕 智能路由狀態 ===
    // 記錄最近的傳輸路徑，防止循環
    private final java.util.Deque<Direction> recentTransferPath = new java.util.ArrayDeque<>(5);
    private static final int MAX_PATH_HISTORY = 5; // 記錄最近5次傳輸

    // === 建構子 ===
    public TransferManager(ArcaneConduitBlockEntity conduit,
                           NetworkManager networkManager,
                           StatsManager statsManager,
                           IOManager ioManager) {
        this.conduit = conduit;
        this.networkManager = networkManager;
        this.statsManager = statsManager;
        this.ioManager = ioManager;
    }

    // === 主要傳輸邏輯 ===

    /**
     * 處理魔力流動
     */
    public void processManaFlow() {
        if (conduit.getManaStored() <= 0) return;
        if (conduit.getLevel() == null) return;
        if (transfersThisTick >= MAX_TRANSFERS_PER_TICK) return;

        long currentTick = conduit.getLevel().getGameTime();

        // 防循環：每tick清除忙碌標記
        if (lastTransferTick != currentTick) {
            busyDirections.clear();
            transfersThisTick = 0;
        }

        // 使用負載平衡算法找到最佳目標
        Direction bestTarget = findBestTarget(currentTick);
        if (bestTarget == null) return;

        // 防循環檢查
        if (shouldBlockTransfer(bestTarget, currentTick)) {
            return;
        }

        // 執行傳輸
        executeTransfer(bestTarget, currentTick);
    }

    /**
     * 從指定方向接收魔力
     */
    public int receiveManaFromDirection(int maxReceive, ManaAction action, Direction fromDirection) {
        int received = conduit.receiveMana(maxReceive, action);

        if (action.execute() && received > 0) {
            lastReceiveDirection = fromDirection.getOpposite();
            statsManager.recordActivity();
        }

        return received;
    }

    // === 目標選擇算法 ===

    /**
     * 找到最佳傳輸目標
     */
    private Direction findBestTarget(long currentTick) {
        // 定期重新掃描目標（通過NetworkManager）
        if (currentTick % 10 == 0) {
            // NetworkManager會自動處理目標重新掃描
        }

        // 使用負載平衡策略選擇目標
        return BalancingStrategy.selectBestTarget(
                networkManager.getValidTargets(),
                conduit,
                ioManager,
                networkManager,
                statsManager.getTickCounter()
        );
    }

    /**
     * 🆕 檢查是否應該阻止傳輸（強化版）
     */
    private boolean shouldBlockTransfer(Direction targetDir, long currentTick) {
        // 1. 本tick已經傳輸過這個方向
        if (busyDirections.contains(targetDir)) return true;

        // 2. 🆕 強化的防回流：不要立即傳回給剛給我魔力的方向
        // 增加冷卻時間到3 tick，避免短期內來回傳輸
        if (lastReceiveDirection != null &&
                targetDir == lastReceiveDirection &&
                currentTick - lastTransferTick <= 3) {
            return true;
        }

        // 3. 避免連續傳輸到同一方向（減少振盪）
        if (lastTransferDirection == targetDir && currentTick - lastTransferTick == 0) {
            return true;
        }

        // 4. 🆕 防止循環路徑：檢查是否在最近的傳輸路徑中頻繁出現
        if (isCircularPath(targetDir)) {
            LOGGER.debug("Blocked circular path to {}", targetDir);
            return true;
        }

        return false;
    }

    /**
     * 🆕 檢測循環路徑
     * 如果同一方向在最近的路徑中出現超過2次，視為循環
     */
    private boolean isCircularPath(Direction targetDir) {
        if (recentTransferPath.size() < 3) {
            return false; // 路徑太短，無法判斷
        }

        int occurrences = 0;
        for (Direction dir : recentTransferPath) {
            if (dir == targetDir) {
                occurrences++;
                if (occurrences >= 2) {
                    return true; // 同一方向出現2次以上，可能是循環
                }
            }
        }

        return false;
    }

    /**
     * 執行實際傳輸
     */
    private void executeTransfer(Direction targetDir, long currentTick) {
        CacheManager.TargetInfo target = networkManager.getTargetInfo(targetDir);
        if (target == null || !target.canReceive) return;

        // 對導管連接做額外檢查
        if (target.isConduit) {
            if (!validateConduitTransfer(targetDir)) {
                return;
            }
        }

        // ✨ 使用導管等級系統計算傳輸速率
        int transferRateLimit = getTransferRateLimit();

        // 計算最優傳輸量：考慮等級限制、當前魔力、目標空間
        int maxTransfer = Math.min(transferRateLimit, conduit.getManaStored());
        int transferAmount = Math.min(maxTransfer, target.availableSpace);

        if (transferAmount <= 0) return;

        // 執行傳輸
        BlockPos neighborPos = conduit.getBlockPos().relative(targetDir);
        IUnifiedManaHandler handler = CapabilityUtils.getNeighborMana(conduit.getLevel(), neighborPos, targetDir);

        if (handler != null) {
            // 模擬傳輸
            int simulated = handler.receiveMana(transferAmount, ManaAction.SIMULATE);
            if (simulated > 0) {
                // 執行實際傳輸
                int actualReceived = handler.receiveMana(simulated, ManaAction.EXECUTE);
                conduit.extractMana(actualReceived, ManaAction.EXECUTE);

//                LOGGER.debug("Transfer executed: {} mana from {} to {} (direction: {})",
//                        actualReceived, conduit.getBlockPos(), neighborPos, targetDir);

                // 更新狀態
                updateTransferState(targetDir, actualReceived, currentTick, true);

            } else {
                LOGGER.debug("Transfer simulation failed: tried {}, got {}", transferAmount, simulated);
                updateTransferState(targetDir, 0, currentTick, false);
            }
        } else {
            LOGGER.debug("No mana handler found at {}", neighborPos);
            updateTransferState(targetDir, 0, currentTick, false);
        }
    }

    /**
     * 驗證導管間傳輸
     */
    private boolean validateConduitTransfer(Direction targetDir) {
        BlockPos neighborPos = conduit.getBlockPos().relative(targetDir);
        BlockEntity neighborBE = conduit.getLevel().getBlockEntity(neighborPos);

        if (!(neighborBE instanceof ArcaneConduitBlockEntity neighborConduit)) {
            return false;
        }

        Direction neighborInputSide = targetDir.getOpposite();
        IOHandlerUtils.IOType neighborIOType = neighborConduit.getIOConfig(neighborInputSide);
        IOHandlerUtils.IOType myIOType = ioManager.getIOConfig(targetDir);

        // 檢查雙方的IO配置
        boolean canReceiveAtNeighbor = (neighborIOType == IOHandlerUtils.IOType.INPUT ||
                neighborIOType == IOHandlerUtils.IOType.BOTH);
        boolean canSendFromMe = (myIOType == IOHandlerUtils.IOType.OUTPUT ||
                myIOType == IOHandlerUtils.IOType.BOTH);

        if (!canReceiveAtNeighbor || !canSendFromMe ||
                neighborIOType == IOHandlerUtils.IOType.DISABLED) {
            LOGGER.debug("Transfer blocked: me={}, neighbor={}, canSend={}, canReceive={}",
                    myIOType, neighborIOType, canSendFromMe, canReceiveAtNeighbor);
            return false;
        }

        // 檢查鄰居是否有空間
        if (neighborConduit.getManaStored() >= neighborConduit.getMaxManaStored()) {
            LOGGER.debug("Transfer skipped: neighbor conduit is full");
            return false;
        }

        return true;
    }

    /**
     * 🆕 更新傳輸狀態（強化版）
     */
    private void updateTransferState(Direction direction, int amount, long currentTick, boolean success) {
        // 記錄統計
        statsManager.recordTransfer(direction, amount, success);

        // 更新傳輸狀態
        if (success && amount > 0) {
            lastTransferDirection = direction;
            lastTransferTick = currentTick;
            busyDirections.add(direction);
            transfersThisTick++;

            // 🆕 更新路徑歷史
            updateTransferPath(direction);

            // 標記改變（觸發保存）
            conduit.setChanged();
        }
    }

    /**
     * 🆕 更新傳輸路徑歷史
     */
    private void updateTransferPath(Direction direction) {
        // 添加到路徑歷史
        recentTransferPath.addLast(direction);

        // 保持歷史記錄在限制內
        while (recentTransferPath.size() > MAX_PATH_HISTORY) {
            recentTransferPath.removeFirst();
        }
    }

    // === 速率限制相關方法 ===

    /**
     * 🆕 獲取當前導管的傳輸速率限制
     * 優先使用導管自身的等級，如果未設定則使用預設值
     */
    private int getTransferRateLimit() {
        // 從導管獲取等級
        ConduitTier tier = conduit.getTier();
        if (tier != null) {
            return tier.getTransferRate();
        }

        // 向後兼容：如果導管還沒有等級系統，使用舊的固定速率
        return LEGACY_TRANSFER_RATE;
    }

    /**
     * 🆕 獲取當前導管的傳輸速率（供外部查詢）
     */
    public int getCurrentTransferRate() {
        return getTransferRateLimit();
    }

    // === NBT 序列化 ===

    /**
     * 保存到NBT
     */
    public void saveToNBT(CompoundTag tag) {
        if (lastReceiveDirection != null) {
            tag.putString("LastReceiveDirection", lastReceiveDirection.name());
        }
        if (lastTransferDirection != null) {
            tag.putString("LastTransferDirection", lastTransferDirection.name());
        }
        tag.putLong("LastTransferTick", lastTransferTick);
    }

    /**
     * 從NBT載入
     */
    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("LastReceiveDirection")) {
            try {
                lastReceiveDirection = Direction.valueOf(tag.getString("LastReceiveDirection"));
            } catch (IllegalArgumentException e) {
                lastReceiveDirection = null;
            }
        }

        if (tag.contains("LastTransferDirection")) {
            try {
                lastTransferDirection = Direction.valueOf(tag.getString("LastTransferDirection"));
            } catch (IllegalArgumentException e) {
                lastTransferDirection = null;
            }
        }

        lastTransferTick = tag.getLong("LastTransferTick");

        // 🆕 載入路徑歷史（可選，因為路徑歷史是運行時狀態）
        // 這裡我們選擇不保存路徑歷史，每次重啟時重置

        // 重置運行時狀態
        busyDirections.clear();
        transfersThisTick = 0;
        recentTransferPath.clear(); // 🆕 清除路徑歷史
    }

    // === Getter 方法 ===

    /**
     * 獲取最後接收方向
     */
    public Direction getLastReceiveDirection() {
        return lastReceiveDirection;
    }

    /**
     * 獲取最後傳輸方向
     */
    public Direction getLastTransferDirection() {
        return lastTransferDirection;
    }

    /**
     * 檢查指定方向是否正在傳輸
     */
    public boolean isTransferring(Direction direction) {
        return busyDirections.contains(direction);
    }

    /**
     * 獲取本tick已傳輸次數
     */
    public int getTransfersThisTick() {
        return transfersThisTick;
    }

    // === 🆕 智能路由相關方法 ===

    /**
     * 🆕 獲取最近的傳輸路徑（供調試使用）
     */
    public String getRecentPathDebugInfo() {
        if (recentTransferPath.isEmpty()) {
            return "No recent transfers";
        }

        StringBuilder sb = new StringBuilder("Recent path: ");
        for (Direction dir : recentTransferPath) {
            sb.append(dir.name()).append(" → ");
        }
        return sb.toString();
    }

    /**
     * 🆕 清除路徑歷史（用於手動重置）
     */
    public void clearPathHistory() {
        recentTransferPath.clear();
        LOGGER.debug("Transfer path history cleared");
    }

    /**
     * 🆕 檢查當前是否可能存在循環
     */
    public boolean hasPotentialCircularPath() {
        if (recentTransferPath.size() < 4) {
            return false;
        }

        // 簡單的循環檢測：ABAB 模式
        java.util.List<Direction> pathList = new java.util.ArrayList<>(recentTransferPath);
        int size = pathList.size();

        if (size >= 4) {
            // 檢查最後4個元素是否形成 ABAB 模式
            Direction a = pathList.get(size - 4);
            Direction b = pathList.get(size - 3);
            Direction c = pathList.get(size - 2);
            Direction d = pathList.get(size - 1);

            return (a == c && b == d && a != b);
        }

        return false;
    }
}


