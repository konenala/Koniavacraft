package com.github.nalamodikk.common.block.conduit.manager;

import com.github.nalamodikk.common.block.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 導管網路管理器
 * 負責掃描網路拓撲、管理連接和維護網路狀態
 */
public class ConduitNetworkManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConduitNetworkManager.class);

    // === 常量 ===
    private static final int NETWORK_SCAN_INTERVAL = 600; // 30秒

    // === 組件引用 ===
    private final ArcaneConduitBlockEntity conduit;
    private final ConduitCacheManager cacheManager;
    private final ConduitIOManager ioManager;

    // === 網路狀態 ===
    private final Set<BlockPos> networkNodes = new HashSet<>();
    private final Map<Direction, ConduitCacheManager.ManaEndpoint> endpoints = new HashMap<>();
    private boolean networkDirty = true;
    private int tickOffset;
    private boolean isScanning = false;

    // === 建構子 ===
    public ConduitNetworkManager(ArcaneConduitBlockEntity conduit,
                                 ConduitCacheManager cacheManager,
                                 ConduitIOManager ioManager,
                                 int tickOffset) {
        this.conduit = conduit;
        this.cacheManager = cacheManager;
        this.ioManager = ioManager;
        this.tickOffset = tickOffset;
    }

    // === 網路更新 ===

    /**
     * 根據需要更新網路狀態
     */
    public void updateIfNeeded(long tickCounter) {
        // 錯開網路掃描：避免所有導管同時掃描
        if ((tickCounter + tickOffset) % NETWORK_SCAN_INTERVAL == 0 || networkDirty) {
            // 只有1/4的導管會執行完整掃描，其他使用簡化版
            if (tickCounter % 4 == 0 || networkDirty) {
                scanNetworkTopology();
            } else {
                quickEndpointCheck(); // 輕量級檢查
            }
            networkDirty = false;
        }
    }

    /**
     * 標記網路為髒狀態，需要重新掃描
     */
    public void markDirty() {
        networkDirty = true;
        cacheManager.clearAllTargetCache();
    }

    // === 目標獲取 ===

    /**
     * 獲取所有有效的傳輸目標
     */
    /**
     * 🔧 修復：獲取所有有效的傳輸目標 - 防止遞迴
     */
    public List<Direction> getValidTargets() {
        // 🚨 遞迴防護：如果正在掃描，返回空列表
        if (isScanning) {
            LOGGER.warn("Prevented infinite recursion in getValidTargets() at {}",
                    conduit.getBlockPos());
            return new ArrayList<>();
        }

        // 檢查是否需要重新掃描目標
        if (cacheManager.needsTargetRescan()) {
            try {
                isScanning = true; // 🔒 設置掃描標記
                rescanTargets();
            } finally {
                isScanning = false; // 🔓 確保標記被清除
            }
        }

        List<Direction> validTargets = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            // 檢查我是否能輸出到這個方向
            if (!ioManager.canOutput(dir)) {
                continue;
            }

            ConduitCacheManager.TargetInfo target = cacheManager.getCachedTarget(dir);
            if (target != null && target.canReceive) {
                // 對導管做額外檢查
                if (target.isConduit && !validateConduitConnection(dir)) {
                    continue;
                }
                validTargets.add(dir);
            }
        }

        return validTargets;
    }


    /**
     * 獲取指定方向的目標信息
     */
    public ConduitCacheManager.TargetInfo getTargetInfo(Direction direction) {
        return cacheManager.getCachedTarget(direction);
    }

    // === 連接驗證 ===

    /**
     * 驗證導管間連接是否有效
     */
    private boolean validateConduitConnection(Direction dir) {
        BlockPos neighborPos = conduit.getBlockPos().relative(dir);
        BlockEntity neighborBE = conduit.getLevel().getBlockEntity(neighborPos);

        if (!(neighborBE instanceof ArcaneConduitBlockEntity neighborConduit)) {
            return false;
        }

        Direction neighborInputSide = dir.getOpposite();
        IOHandlerUtils.IOType neighborIOType = neighborConduit.getIOConfig(neighborInputSide);

        // 🚨 只做基本檢查，避免複雜邏輯
        if (neighborIOType == IOHandlerUtils.IOType.DISABLED) {
            return false;
        }

        // ⚡ 直接檢查魔力容量，避免調用其他方法
        return neighborConduit.getManaStored() < neighborConduit.getMaxManaStored();
    }

    // === 網路掃描 ===

    /**
     * 掃描網路拓撲
     */
    private void scanNetworkTopology() {
        if (!(conduit.getLevel() instanceof ServerLevel)) return;

        long now = System.currentTimeMillis();

        // 檢查緩存是否有效
        if (cacheManager.isCacheValid(30000) && !networkDirty) {
            var cached = cacheManager.getSharedCache();
            var cachedNodes = cacheManager.getSharedNetworkNodes();

            if (cached != null && cachedNodes != null) {
                endpoints.clear();
                endpoints.putAll(cached);
                networkNodes.clear();
                networkNodes.addAll(cachedNodes);
                return;
            }
        }

        // 執行實際掃描
        performNetworkScan();
    }

    /**
     * 執行實際的網路掃描
     */
    private void performNetworkScan() {
        networkNodes.clear();
        endpoints.clear();

        for (Direction dir : Direction.values()) {
            if (ioManager.isDisabled(dir)) continue;

            BlockPos neighborPos = conduit.getBlockPos().relative(dir);
            IUnifiedManaHandler handler = CapabilityUtils.getNeighborMana(conduit.getLevel(), neighborPos, dir);
            if (handler == null) continue;

            boolean isConduit = conduit.getLevel().getBlockEntity(neighborPos) instanceof ArcaneConduitBlockEntity;

            // 計算優先級
            int priority = ioManager.getPriority(dir);
            if (handler.canReceive() && handler.getManaStored() < handler.getMaxManaStored() / 2) {
                priority += 10; // 空容器優先級加成
            }

            endpoints.put(dir, new ConduitCacheManager.ManaEndpoint(handler, isConduit, priority));
            if (!isConduit) {
                networkNodes.add(neighborPos); // 只記錄非導管節點
            }
        }

        // 更新緩存
        cacheManager.setSharedCache(endpoints);
        cacheManager.setSharedNetworkNodes(networkNodes);

        LOGGER.debug("Network scan completed for {} - found {} endpoints",
                conduit.getBlockPos(), endpoints.size());
    }

    /**
     * 重新掃描目標
     */
    private void rescanTargets() {
        if (conduit.getLevel() == null) return;

        // 🚨 如果已經在掃描，直接返回
        if (isScanning) {
            LOGGER.warn("Prevented recursive rescanTargets() call at {}",
                    conduit.getBlockPos());
            return;
        }

        LOGGER.debug("Starting target rescan for {}", conduit.getBlockPos());

        for (Direction dir : Direction.values()) {
            // 檢查我是否能輸出到這個方向
            if (!ioManager.canOutput(dir)) {
                continue;
            }

            BlockPos neighborPos = conduit.getBlockPos().relative(dir);
            IUnifiedManaHandler handler = CapabilityUtils.getNeighborMana(conduit.getLevel(), neighborPos, dir);

            if (handler != null) {
                BlockEntity neighborBE = conduit.getLevel().getBlockEntity(neighborPos);
                boolean isConduit = neighborBE instanceof ArcaneConduitBlockEntity;

                // 🔧 簡化導管檢查：避免複雜邏輯導致遞迴
                if (isConduit) {
                    ArcaneConduitBlockEntity neighborConduit = (ArcaneConduitBlockEntity) neighborBE;
                    Direction neighborInputSide = dir.getOpposite();
                    IOHandlerUtils.IOType neighborIOType = neighborConduit.getIOConfig(neighborInputSide);

                    // 🚨 移除任何可能調用到 getValidTargets() 的代碼
                    if (neighborIOType == IOHandlerUtils.IOType.DISABLED) {
                        LOGGER.debug("Skipping conduit at {} - disabled on side {}", neighborPos, neighborInputSide);
                        continue;
                    }

                    // ⚡ 直接檢查魔力存儲，避免複雜方法調用
                    if (neighborConduit.getManaStored() >= neighborConduit.getMaxManaStored()) {
                        LOGGER.debug("Skipping conduit at {} - full", neighborPos);
                        continue;
                    }
                }

                // 設定目標緩存
                cacheManager.setCachedTarget(dir, handler, isConduit);

                LOGGER.debug("Found target at {} (conduit: {}, canReceive: {}, space: {})",
                        neighborPos, isConduit, handler.canReceive(),
                        handler.getMaxManaStored() - handler.getManaStored());
            }
        }

        cacheManager.updateLastScanTime();
        LOGGER.debug("Target rescan completed for {}", conduit.getBlockPos());
    }
    /**
     * 輕量級端點檢查
     */
    private void quickEndpointCheck() {
        if (conduit.getLevel() == null || endpoints.isEmpty()) return;

        // 只檢查一個端點，避免每次都檢查全部
        Direction[] dirs = endpoints.keySet().toArray(new Direction[0]);
        long tickCounter = conduit.getLevel().getGameTime();
        Direction dirToCheck = dirs[(int) (tickCounter % dirs.length)];

        ConduitCacheManager.ManaEndpoint endpoint = endpoints.get(dirToCheck);
        if (endpoint != null) {
            BlockPos neighborPos = conduit.getBlockPos().relative(dirToCheck);
            IUnifiedManaHandler current = CapabilityUtils.getNeighborMana(conduit.getLevel(), neighborPos, dirToCheck);

            if (current == null || current != endpoint.handler) {
                // 這個端點無效了，移除並標記需要重新掃描
                endpoints.remove(dirToCheck);
                networkDirty = true;

                // 清除對應的緩存
                cacheManager.clearTargetCache(dirToCheck);
            }
        }
    }

    // === 連接狀態查詢 ===

    /**
     * 檢查指定方向是否有連接
     */
    public boolean hasConnection(Direction direction) {
        return endpoints.containsKey(direction);
    }

    /**
     * 檢查指定方向是否連接到另一個導管
     */
    public boolean isConnectedToConduit(Direction direction) {
        ConduitCacheManager.ManaEndpoint endpoint = endpoints.get(direction);
        return endpoint != null && endpoint.isConduit;
    }

    /**
     * 獲取活躍連接數量
     */
    public int getActiveConnectionCount() {
        return (int) endpoints.values().stream()
                .filter(endpoint -> !endpoint.isConduit)
                .count();
    }

    // === 鄰居變化處理 ===

    /**
     * 處理鄰居變化事件
     */
    public void onNeighborChanged() {
        LOGGER.debug("Neighbor changed for conduit at {}", conduit.getBlockPos());

        // 清除所有緩存
        cacheManager.invalidateAll();

        // 清除本地狀態
        endpoints.clear();
        networkNodes.clear();

        // 標記需要重新掃描
        networkDirty = true;

        LOGGER.debug("Network state reset for conduit at {}", conduit.getBlockPos());
    }

    /**
     * 處理方向配置變化
     */
    public void onDirectionConfigChanged(Direction direction) {
        // 清除該方向的緩存
        endpoints.remove(direction);
        cacheManager.invalidateDirection(direction);

        // 標記網路需要重新掃描
        networkDirty = true;
    }

    // === 清理和維護 ===

    /**
     * 執行被動清理
     */
    public void performPassiveCleanup() {
        try {
            // 只清理明顯無效的緩存條目
            Set<Direction> invalidDirections = new HashSet<>();

            for (Map.Entry<Direction, ConduitCacheManager.ManaEndpoint> entry : endpoints.entrySet()) {
                Direction dir = entry.getKey();
                BlockPos neighborPos = conduit.getBlockPos().relative(dir);

                // 安全檢查：只檢查已載入的區塊
                if (conduit.getLevel().isLoaded(neighborPos)) {
                    BlockEntity neighborBE = conduit.getLevel().getBlockEntity(neighborPos);

                    // 如果鄰居不再是導管或不存在，標記為無效
                    if (!(neighborBE instanceof ArcaneConduitBlockEntity)) {
                        invalidDirections.add(dir);
                    }
                }
            }

            // 移除無效的緩存
            for (Direction dir : invalidDirections) {
                endpoints.remove(dir);
                cacheManager.clearTargetCache(dir);
            }

            if (!invalidDirections.isEmpty()) {
                LOGGER.debug("Passive cleanup removed {} invalid references", invalidDirections.size());
            }

        } catch (Exception e) {
            LOGGER.debug("Error during passive cleanup: {}", e.getMessage());
        }
    }

    // === Getter 方法 ===

    /**
     * 獲取所有端點的副本
     */
    public Map<Direction, ConduitCacheManager.ManaEndpoint> getEndpoints() {
        return new HashMap<>(endpoints);
    }

    /**
     * 獲取網路節點集合的副本
     */
    public Set<BlockPos> getNetworkNodes() {
        return new HashSet<>(networkNodes);
    }

    /**
     * 檢查網路是否為髒狀態
     */
    public boolean isNetworkDirty() {
        return networkDirty;
    }
}