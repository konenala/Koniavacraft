package com.github.nalamodikk.common.block.conduit;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能魔力導管 - 結合防循環和智能路由
 *
 * 特色功能：
 * 1. 防循環傳輸系統
 * 2. 智能目標選擇
 * 3. 優先級路由
 * 4. 性能優化緩存
 */
public class ArcaneConduitBlockEntity extends BlockEntity implements IUnifiedManaHandler, IConfigurableBlock {

    // 移除未使用的 LOGGER

    // === 常量配置（1000+導管優化）===
    private static final int BUFFER_SIZE = 100;
    private static final int TRANSFER_RATE = 200;
    private static final int NETWORK_SCAN_INTERVAL = 600; // 30秒 (大幅延長)
    private static final int TARGET_CACHE_DURATION = 2000; // 2秒 (延長緩存)
    private static final int IDLE_THRESHOLD = 600; // 30秒無活動視為閒置
    private static final int MAX_TRANSFERS_PER_TICK = 2; // 限制每tick傳輸次數

    // === 性能優化：分批處理 ===
    private static int globalTickOffset = 0; // 錯開不同導管的處理時間
    private static final Map<BlockPos, Integer> conduitTickOffsets = new ConcurrentHashMap<>();

    // === 核心組件 ===
    private final ManaStorage buffer = new ManaStorage(BUFFER_SIZE);
    private final EnumMap<Direction, IOHandlerUtils.IOType> ioConfig = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Integer> routePriority = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, TransferStats> transferStats = new EnumMap<>(Direction.class);

    // === 防循環系統 ===
    private Direction lastReceiveDirection = null;
    private Direction lastTransferDirection = null;
    private long lastTransferTick = 0;
    private final Set<Direction> busyDirections = EnumSet.noneOf(Direction.class);

    // === 智能目標緩存 ===
    private final Map<Direction, TargetInfo> cachedTargets = new EnumMap<>(Direction.class);
    private long lastTargetScan = 0;

    // === 網路管理 ===
    private final Set<BlockPos> networkNodes = new HashSet<>();
    private final Map<Direction, ManaEndpoint> endpoints = new EnumMap<>(Direction.class);
    private long tickCounter = 0;
    private boolean networkDirty = true;

    // === 性能優化狀態 ===
    private boolean isIdle = false; // 閒置狀態
    private long lastActivity; // 最後活動時間（在建構子中初始化）
    private int transfersThisTick = 0; // 本tick已傳輸次數
    private int tickOffset; // 個別導管的tick偏移

    // === 全域緩存 ===
    private static final Map<BlockPos, Long> lastScanTime = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Map<Direction, ManaEndpoint>> sharedCache = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Set<BlockPos>> sharedNetworkNodes = new ConcurrentHashMap<>();

    // === 內部數據結構 ===

    private static class TargetInfo {
        final int availableSpace;
        final int storedMana;
        final boolean canReceive;
        final long scanTime;
        final boolean isConduit; // ← 新增這個字段

        TargetInfo(IUnifiedManaHandler handler, boolean isConduit) {
            this.availableSpace = handler.getMaxManaStored() - handler.getManaStored();
            this.storedMana = handler.getManaStored();
            this.canReceive = handler.canReceive() && availableSpace > 0;
            this.scanTime = System.currentTimeMillis();
            this.isConduit = isConduit; // ← 新增這行

        }

        boolean isValid() {
            return System.currentTimeMillis() - scanTime < TARGET_CACHE_DURATION;
        }

        int getPriority() {
            return availableSpace; // 空間越大優先級越高
        }
    }

    private static class ManaEndpoint {
        final IUnifiedManaHandler handler;
        final boolean isConduit;
        final int priority;
        long lastAccess;

        ManaEndpoint(IUnifiedManaHandler handler, boolean isConduit, int priority) {
            this.handler = handler;
            this.isConduit = isConduit;
            this.priority = priority;
            this.lastAccess = System.currentTimeMillis();
        }
    }

    private static class TransferStats {
        int totalTransferred = 0;
        int successfulTransfers = 0;
        int failedTransfers = 0;
        long lastTransfer = 0;
        double averageRate = 0.0;


        void recordTransfer(int amount, boolean success) {
            if (success) {
                totalTransferred += amount;
                successfulTransfers++;
                averageRate = averageRate * 0.9 + amount * 0.1;
            } else {
                failedTransfers++;
            }
            lastTransfer = System.currentTimeMillis();
        }

        double getReliability() {
            int total = successfulTransfers + failedTransfers;
            return total > 0 ? (double) successfulTransfers / total : 1.0;
        }
    }

    // === 建構子 ===

    public ArcaneConduitBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ARCANE_CONDUIT_BE.get(), pos, blockState);

        // 初始化配置
        for (Direction dir : Direction.values()) {
            ioConfig.put(dir, IOHandlerUtils.IOType.BOTH);
            transferStats.put(dir, new TransferStats());
            routePriority.put(dir, 50);
        }

        // === 1000+導管優化：錯開處理時間 ===
        this.tickOffset = conduitTickOffsets.computeIfAbsent(pos,
                k -> (globalTickOffset++) % NETWORK_SCAN_INTERVAL);
        this.lastActivity = System.currentTimeMillis();
    }

    // === 主要Tick邏輯（1000+導管優化版）===

    public void tick() {
        if (level == null || level.isClientSide) return;

        tickCounter++;
        transfersThisTick = 0;

        // === 閒置檢測：無魔力且長時間無活動則休眠 ===
        long currentTime = System.currentTimeMillis();
        boolean hasActivity = buffer.getManaStored() > 0 ||
                (currentTime - lastActivity) < IDLE_THRESHOLD;

        if (!hasActivity && !networkDirty) {
            isIdle = true;
            // 閒置導管每10秒只處理一次
            if (tickCounter % 200 != tickOffset % 200) {
                return;
            }
        } else {
            isIdle = false;
        }

        // === 錯開網路掃描：避免所有導管同時掃描 ===
        if ((tickCounter + tickOffset) % NETWORK_SCAN_INTERVAL == 0 || networkDirty) {
            // 只有1/4的導管會執行完整掃描，其他使用簡化版
            if (tickCounter % 4 == 0 || networkDirty) {
                scanNetworkTopology();
            } else {
                quickEndpointCheck(); // 輕量級檢查
            }
            networkDirty = false;
        }

        // === 處理魔力流動（限制頻率）===
        if (!isIdle) {
            handleManaFlow();
        }

        // === 大幅減少清理頻率：1小時一次 ===
        if (tickCounter % 72000 == tickOffset) { // 1小時，且錯開時間
            cleanupStaleData();
        }
    }

    // === 核心傳輸邏輯（1000+導管優化版）===

    private void handleManaFlow() {
        if (buffer.getManaStored() <= 0) return;

        // === 安全檢查：確保 level 不為空 ===
        if (level == null) return;

        // === 限制每tick傳輸次數，避免單個導管佔用過多資源 ===
        if (transfersThisTick >= MAX_TRANSFERS_PER_TICK) return;

        long currentTick = level.getGameTime();

        // 防循環：每tick清除忙碌標記
        if (lastTransferTick != currentTick) {
            busyDirections.clear();
        }

        // 找到最佳目標
        Direction bestTarget = findBestTarget(currentTick);
        if (bestTarget == null) return;

        // 防循環檢查
        if (shouldBlockTransfer(bestTarget, currentTick)) {
            return;
        }

        // 執行傳輸
        executeSmartTransfer(bestTarget, currentTick);
    }

    private Direction findBestTarget(long currentTick) {
        if (level == null) return null; // ✅ 安全檢查

        // 定期重新掃描目標
        if (currentTick % 10 == 0 || needsTargetRescan()) {
            rescanTargets();
        }

        Direction bestDir = null;
        int maxPriority = 0;

        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType myIOType = ioConfig.get(dir);
            if (myIOType == IOHandlerUtils.IOType.DISABLED ||
                    myIOType == IOHandlerUtils.IOType.INPUT) {
                continue;
            }

            TargetInfo target = cachedTargets.get(dir);
            if (target != null && target.canReceive) {

                if (target.isConduit) {
                    BlockPos neighborPos = worldPosition.relative(dir);
                    BlockEntity neighborBE = level.getBlockEntity(neighborPos);

                    if (neighborBE instanceof ArcaneConduitBlockEntity neighborConduit) {
                        Direction neighborInputSide = dir.getOpposite();
                        IOHandlerUtils.IOType neighborIOType = neighborConduit.getIOConfig(neighborInputSide);

                        if (neighborIOType == IOHandlerUtils.IOType.DISABLED ||
                                neighborIOType == IOHandlerUtils.IOType.OUTPUT) {
                            continue;
                        }
                    }
                }

                int priority = target.getPriority() + routePriority.get(dir);
                if (priority > maxPriority) {
                    maxPriority = priority;
                    bestDir = dir;
                }
            }
        }

        return bestDir;
    }

    private boolean shouldBlockTransfer(Direction targetDir, long currentTick) {
        // 1. 本tick已經傳輸過這個方向
        if (busyDirections.contains(targetDir)) return true;

        // 2. 不要立即傳回給剛給我魔力的方向
        if (lastReceiveDirection != null &&
                targetDir == lastReceiveDirection &&
                currentTick - lastTransferTick <= 1) return true;

        // 3. 避免連續傳輸到同一方向（減少振盪）
        return lastTransferDirection == targetDir && currentTick - lastTransferTick == 0;
    }

    private void executeSmartTransfer(Direction targetDir, long currentTick) {
        // === 安全檢查 ===
        if (level == null) return;

        TargetInfo target = cachedTargets.get(targetDir);
        if (target == null || !target.canReceive) return;

        // ✅ 新增：再次檢查目標的IO配置（雙重保險）
        if (target.isConduit) {
            BlockPos neighborPos = worldPosition.relative(targetDir);
            BlockEntity neighborBE = level.getBlockEntity(neighborPos);

            if (neighborBE instanceof ArcaneConduitBlockEntity neighborConduit) {
                Direction neighborInputSide = targetDir.getOpposite();
                IOHandlerUtils.IOType neighborIOType = neighborConduit.getIOConfig(neighborInputSide);

                if (neighborIOType == IOHandlerUtils.IOType.DISABLED ||
                        neighborIOType == IOHandlerUtils.IOType.OUTPUT) {
                    return; // 目標不能接收，取消傳輸
                }
            }
        }

        // 計算最優傳輸量
        int maxTransfer = Math.min(TRANSFER_RATE, buffer.getManaStored());
        int transferAmount = Math.min(maxTransfer, target.availableSpace);

        if (transferAmount <= 0) return;

        // 執行傳輸
        BlockPos neighborPos = worldPosition.relative(targetDir);
        IUnifiedManaHandler handler = CapabilityUtils.getNeighborMana(level, neighborPos, targetDir);

        if (handler != null) {
            // 模擬傳輸
            int simulated = handler.receiveMana(transferAmount, ManaAction.SIMULATE);
            if (simulated > 0) {
                // 執行實際傳輸
                int actualReceived = handler.receiveMana(simulated, ManaAction.EXECUTE);
                buffer.extractMana(actualReceived, ManaAction.EXECUTE);

                // 更新統計和狀態
                updateTransferStats(targetDir, actualReceived);
                lastTransferDirection = targetDir;
                lastTransferTick = currentTick;
                busyDirections.add(targetDir);

                setChanged();
            }
        }
    }


    // === 目標掃描與緩存 ===

    private void rescanTargets() {
        if (level == null) return; // ✅ 安全檢查

        cachedTargets.clear();

        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType myIOType = ioConfig.get(dir);
            if (myIOType == IOHandlerUtils.IOType.DISABLED ||
                    myIOType == IOHandlerUtils.IOType.INPUT) {
                continue;
            }

            BlockPos neighborPos = worldPosition.relative(dir);
            IUnifiedManaHandler handler = CapabilityUtils.getNeighborMana(level, neighborPos, dir);

            if (handler != null) {
                BlockEntity neighborBE = level.getBlockEntity(neighborPos);
                boolean isConduit = neighborBE instanceof ArcaneConduitBlockEntity;

                if (isConduit) {
                    ArcaneConduitBlockEntity neighborConduit = (ArcaneConduitBlockEntity) neighborBE;
                    Direction neighborInputSide = dir.getOpposite();
                    IOHandlerUtils.IOType neighborIOType = neighborConduit.getIOConfig(neighborInputSide);

                    if (neighborIOType == IOHandlerUtils.IOType.DISABLED ||
                            neighborIOType == IOHandlerUtils.IOType.OUTPUT) {
                        continue;
                    }
                }

                cachedTargets.put(dir, new TargetInfo(handler, isConduit));
            }
        }

        lastTargetScan = System.currentTimeMillis();
    }
    private int calculateMachinePriority(IUnifiedManaHandler handler) {
        int maxMana = handler.getMaxManaStored();
        int currentMana = handler.getManaStored();
        int availableSpace = maxMana - currentMana;
        return maxMana == 0 ? 0 : (availableSpace * 100) / maxMana;
    }

    private void updateTransferStats(Direction direction, int amount) {
        TransferStats stats = transferStats.get(direction);
        if (stats != null) {
            stats.recordTransfer(amount, true);
            lastActivity = System.currentTimeMillis();
            transfersThisTick++;
        }
    }

    private boolean needsTargetRescan() {
        return System.currentTimeMillis() - lastTargetScan > 5000 || cachedTargets.isEmpty(); // 延長到5秒
    }

    // === 輕量級端點檢查（1000+導管優化）===

    /**
     * 輕量級檢查，只驗證現有端點是否仍然有效
     * 避免完整的網路拓撲掃描
     */
    private void quickEndpointCheck() {
        // === 安全檢查 ===
        if (level == null || endpoints.isEmpty()) return;

        // 只檢查一個端點，避免每次都檢查全部
        Direction[] dirs = endpoints.keySet().toArray(new Direction[0]);
        // 移除永遠為false的檢查，因為上面已經檢查了 isEmpty()

        Direction dirToCheck = dirs[(int)(tickCounter % dirs.length)];
        ManaEndpoint endpoint = endpoints.get(dirToCheck);

        if (endpoint != null) {
            BlockPos neighborPos = worldPosition.relative(dirToCheck);
            IUnifiedManaHandler current = CapabilityUtils.getNeighborMana(level, neighborPos, dirToCheck);

            if (current == null || current != endpoint.handler) {
                // 這個端點無效了，移除並標記需要重新掃描
                endpoints.remove(dirToCheck);
                networkDirty = true;

                // 清除對應的緩存
                cachedTargets.remove(dirToCheck);
            }
        }
    }

    // === 網路拓撲掃描（1000+導管優化版）===

    private void scanNetworkTopology() {
        if (!(level instanceof ServerLevel)) return;

        long now = System.currentTimeMillis();
        long lastScan = lastScanTime.getOrDefault(worldPosition, 0L);

        // === 大幅延長緩存有效期：從3秒改為30秒 ===
        if (now - lastScan < 30000 && !networkDirty) {
            Map<Direction, ManaEndpoint> cached = sharedCache.get(worldPosition);
            Set<BlockPos> cachedNodes = sharedNetworkNodes.get(worldPosition);

            if (cached != null && cachedNodes != null) {
                endpoints.clear();
                endpoints.putAll(cached);
                networkNodes.clear();
                networkNodes.addAll(cachedNodes);
                return;
            }
        }

        // === 簡化掃描：只掃描必要的方向 ===
        networkNodes.clear();
        endpoints.clear();

        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType ioType = ioConfig.get(dir);
            if (ioType == IOHandlerUtils.IOType.DISABLED) continue;

            BlockPos neighborPos = worldPosition.relative(dir);

            // === 批次查詢優化：減少 capability 查詢次數 ===
            IUnifiedManaHandler handler = CapabilityUtils.getNeighborMana(level, neighborPos, dir);
            if (handler == null) continue;

            boolean isConduit = level.getBlockEntity(neighborPos) instanceof ArcaneConduitBlockEntity;

            // === 簡化優先級計算，減少浮點運算 ===
            int priority = routePriority.get(dir);
            if (handler.canReceive() && handler.getManaStored() < handler.getMaxManaStored() / 2) {
                priority += 10; // 簡化的空容器優先級
            }

            endpoints.put(dir, new ManaEndpoint(handler, isConduit, priority));
            if (!isConduit) {
                networkNodes.add(neighborPos); // 只記錄非導管節點
            }
        }

        // 更新緩存
        lastScanTime.put(worldPosition, now);
        sharedCache.put(worldPosition, new HashMap<>(endpoints));
        sharedNetworkNodes.put(worldPosition, new HashSet<>(networkNodes));

        // === 定期清理全域緩存，避免記憶體洩漏 ===
        if (now % 100000 < 50) { // 大約每10萬毫秒清理一次
            cleanupGlobalCache();
        }
    }

    /**
     * 清理全域緩存，避免記憶體累積
     */
    private static void cleanupGlobalCache() {
        long now = System.currentTimeMillis();

        // 移除超過1分鐘沒更新的緩存
        lastScanTime.entrySet().removeIf(entry -> now - entry.getValue() > 60000);

        // 清理對應的緩存數據
        sharedCache.entrySet().removeIf(entry -> !lastScanTime.containsKey(entry.getKey()));
        sharedNetworkNodes.entrySet().removeIf(entry -> !lastScanTime.containsKey(entry.getKey()));
    }

    // === 接收魔力（帶方向追蹤）===

    public int receiveManaFromDirection(int maxReceive, ManaAction action, Direction fromDirection) {
        int received = buffer.receiveMana(maxReceive, action);

        if (action.execute() && received > 0) {
            lastReceiveDirection = fromDirection.getOpposite();
            lastActivity = System.currentTimeMillis(); // 記錄活動
            cachedTargets.clear(); // 狀態改變，清除緩存
        }

        return received;
    }

    // === 清理與維護 ===

    private void cleanupStaleData() {
        long now = System.currentTimeMillis();

        // 衰減長時間無傳輸的統計
        transferStats.values().forEach(stats -> {
            if (now - stats.lastTransfer > 300000) { // 5分鐘
                stats.averageRate *= 0.8;
            }
        });
    }

    public void onNeighborChanged() {
        // 清除緩存
        sharedCache.remove(worldPosition);
        sharedNetworkNodes.remove(worldPosition);
        lastScanTime.remove(worldPosition);
        networkDirty = true;

        // ✅ 新增：更新 BlockState
        if (level != null && !level.isClientSide) {
            updateBlockStateConnections();
        }
    }


    // === 優先級管理 ===

    public void setPriority(Direction direction, int priority) {
        // ✅ 移除100的硬限制，改為Integer安全範圍
        int clampedPriority = Math.max(Integer.MIN_VALUE + 1, Math.min(Integer.MAX_VALUE - 1, priority));

        if (routePriority.get(direction) != clampedPriority) {
            routePriority.put(direction, clampedPriority);
            networkDirty = true;
            setChanged();
        }
    }


    public int getPriority(Direction direction) {
        return routePriority.getOrDefault(direction, 0); // ✅ 默認值改為0，更符合AE2習慣
    }


    public void resetAllPriorities() {
        for (Direction dir : Direction.values()) {
            routePriority.put(dir, 0); // ✅ 重置為0而不是50
        }
        networkDirty = true;
        setChanged();
    }

    // === IO配置管理 ===

    @Override
    public IOHandlerUtils.IOType getIOConfig(Direction direction) {
        return ioConfig.getOrDefault(direction, IOHandlerUtils.IOType.BOTH);
    }

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        IOHandlerUtils.IOType oldType = ioConfig.get(direction);
        if (oldType != type) {
            ioConfig.put(direction, type);
            networkDirty = true;
            setChanged();

            // 清除該方向的緩存
            endpoints.remove(direction);

            // 觸發連接狀態更新
            if (level != null && !level.isClientSide) {
                BlockState currentState = level.getBlockState(worldPosition);
                if (currentState.getBlock() instanceof ArcaneConduitBlock conduitBlock) {
                    BlockState newState = conduitBlock.updateConnections(level, worldPosition, currentState);
                    if (newState != currentState) {
                        level.setBlock(worldPosition, newState, 3);
                    }
                }
            }
        }
    }
    // 在 setIOConfig 方法中的最後添加
    private void updateBlockStateConnections() {
        BlockState currentState = level.getBlockState(worldPosition);
        if (currentState.getBlock() instanceof ArcaneConduitBlock conduitBlock) {
            BlockState newState = conduitBlock.updateConnections(level, worldPosition, currentState);
            if (newState != currentState) {
                level.setBlock(worldPosition, newState, 3);
            }
        }
    }

    @Override
    public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
        return new EnumMap<>(ioConfig);
    }

    @Override
    public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> newIOMap) {
        boolean changed = false;
        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType newType = newIOMap.getOrDefault(dir, IOHandlerUtils.IOType.BOTH);
            if (ioConfig.get(dir) != newType) {
                ioConfig.put(dir, newType);
                changed = true;
            }
        }

        if (changed) {
            networkDirty = true;
            endpoints.clear();
            setChanged();

            // 觸發連接狀態更新
            if (level != null && !level.isClientSide) {
                BlockState currentState = level.getBlockState(worldPosition);
                if (currentState.getBlock() instanceof ArcaneConduitBlock conduitBlock) {
                    BlockState newState = conduitBlock.updateConnections(level, worldPosition, currentState);
                    if (newState != currentState) {
                        level.setBlock(worldPosition, newState, 3);
                    }
                }
            }
        }
    }

    // === 用戶交互 ===

    public InteractionResult onUse(BlockState state, Level level, BlockPos pos,
                                   Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.getItem() instanceof BasicTechWandItem wand) {
            BasicTechWandItem.TechWandMode mode = wand.getMode(heldItem);
            Direction hitFace = hit.getDirection();

            switch (mode) {
                case DIRECTION_CONFIG -> {
                    IOHandlerUtils.IOType current = getIOConfig(hitFace);
                    IOHandlerUtils.IOType next = IOHandlerUtils.nextIOType(current);
                    setIOConfig(hitFace, next);

                    player.displayClientMessage(Component.translatable(
                            "message.koniava.wrench.conduit_mode",
                            Component.translatable("direction.koniava." + hitFace.name().toLowerCase()),
                            Component.translatable("mode.koniava." + next.name().toLowerCase())
                    ), true);

                    return InteractionResult.SUCCESS;
                }

                case CONFIGURE_IO -> {
                    showConduitInfo(player);
                    return InteractionResult.SUCCESS;
                }
            }
        }

        if (heldItem.isEmpty()) {
            showConduitInfo(player);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void showConduitInfo(Player player) {
        player.displayClientMessage(Component.translatable("message.koniava.conduit.info_header"), false);

        player.displayClientMessage(Component.translatable(
                "message.koniava.conduit.mana_status",
                getManaStored(), getMaxManaStored()), false);

        player.displayClientMessage(Component.translatable(
                "message.koniava.conduit.connections",
                getActiveConnectionCount()), false);

        // 顯示IO配置
        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType type = getIOConfig(dir);
            String color = switch (type) {
                case INPUT -> "§2";
                case OUTPUT -> "§c";
                case BOTH -> "§b";
                case DISABLED -> "§8";
            };

            player.displayClientMessage(Component.translatable(
                    "message.koniava.conduit.direction_config",
                    Component.translatable("direction.koniava." + dir.name().toLowerCase()),
                    Component.literal(color).append(Component.translatable("mode.koniava." + type.name().toLowerCase()))
            ), false);
        }
    }

    // === 統計與調試 ===

    public int getActiveConnectionCount() {
        return (int) endpoints.values().stream()
                .filter(endpoint -> !endpoint.isConduit)
                .count();
    }

    public Map<Direction, TransferStats> getTransferStats() {
        return new EnumMap<>(transferStats);
    }

    /**
     * 獲取傳輸歷史（渲染器需要）
     */
    public int getTransferHistory(Direction direction) {
        TransferStats stats = transferStats.get(direction);
        return stats != null ? stats.totalTransferred : 0;
    }

    // === IUnifiedManaHandler 實現 ===

    @Override
    public int receiveMana(int maxReceive, ManaAction action) {
        return buffer.receiveMana(maxReceive, action);
    }

    @Override
    public int extractMana(int maxExtract, ManaAction action) {
        return buffer.extractMana(maxExtract, action);
    }

    @Override
    public int getManaStored() {
        return buffer.getManaStored();
    }

    @Override
    public int getMaxManaStored() {
        return buffer.getMaxManaStored();
    }

    @Override
    public void addMana(int amount) {
        buffer.receiveMana(amount, ManaAction.EXECUTE);
        setChanged();
    }

    @Override
    public void consumeMana(int amount) {
        buffer.extractMana(amount, ManaAction.EXECUTE);
        setChanged();
    }

    @Override
    public void setMana(int amount) {
        buffer.setMana(amount);
        setChanged();
    }

    @Override
    public void onChanged() {
        setChanged();
    }

    @Override
    public boolean canExtract() {
        return buffer.getManaStored() > 0;
    }

    @Override
    public boolean canReceive() {
        return buffer.getManaStored() < buffer.getMaxManaStored();
    }

    // === 多容器支援（簡化實現）===

    @Override
    public int getManaContainerCount() { return 1; }

    @Override
    public int getManaStored(int container) {
        return container == 0 ? buffer.getManaStored() : 0;
    }

    @Override
    public void setMana(int container, int mana) {
        if (container == 0) {
            buffer.setMana(mana);
            setChanged();
        }
    }

    @Override
    public int getMaxManaStored(int container) {
        return container == 0 ? buffer.getMaxManaStored() : 0;
    }

    @Override
    public int getNeededMana(int container) {
        return container == 0 ? buffer.getMaxManaStored() - buffer.getManaStored() : 0;
    }

    @Override
    public int insertMana(int container, int amount, ManaAction action) {
        return container == 0 ? buffer.receiveMana(amount, action) : 0;
    }

    @Override
    public int extractMana(int container, int amount, ManaAction action) {
        return container == 0 ? buffer.extractMana(amount, action) : 0;
    }

    // === NBT 序列化 ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 保存緩衝區
        tag.put("Buffer", buffer.serializeNBT(registries));

        // 保存IO配置
        CompoundTag ioTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            ioTag.putString(dir.name(), ioConfig.get(dir).name());
        }
        tag.put("IOConfig", ioTag);

        // 保存優先級
        CompoundTag priorityTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            priorityTag.putInt(dir.name(), routePriority.get(dir));
        }
        tag.put("RoutePriority", priorityTag);

        // 保存統計數據
        CompoundTag statsTag = new CompoundTag();
        for (var entry : transferStats.entrySet()) {
            CompoundTag dirStats = new CompoundTag();
            TransferStats stats = entry.getValue();
            dirStats.putInt("Total", stats.totalTransferred);
            dirStats.putInt("Success", stats.successfulTransfers);
            dirStats.putInt("Failed", stats.failedTransfers);
            dirStats.putDouble("Rate", stats.averageRate);
            statsTag.put(entry.getKey().name(), dirStats);
        }
        tag.put("Stats", statsTag);

        tag.putLong("TickCounter", tickCounter);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // 載入緩衝區
        if (tag.contains("Buffer")) {
            buffer.deserializeNBT(registries, tag.getCompound("Buffer"));
        }

        // 載入IO配置
        if (tag.contains("IOConfig")) {
            CompoundTag ioTag = tag.getCompound("IOConfig");
            for (Direction dir : Direction.values()) {
                if (ioTag.contains(dir.name())) {
                    try {
                        IOHandlerUtils.IOType type = IOHandlerUtils.IOType.valueOf(ioTag.getString(dir.name()));
                        ioConfig.put(dir, type);
                    } catch (IllegalArgumentException e) {
                        ioConfig.put(dir, IOHandlerUtils.IOType.BOTH);
                    }
                }
            }
        }

        // 載入優先級
        if (tag.contains("RoutePriority")) {
            CompoundTag priorityTag = tag.getCompound("RoutePriority");
            for (Direction dir : Direction.values()) {
                if (priorityTag.contains(dir.name())) {
                    routePriority.put(dir, priorityTag.getInt(dir.name()));
                }
            }
        }

        // 載入統計數據
        if (tag.contains("Stats")) {
            CompoundTag statsTag = tag.getCompound("Stats");
            for (Direction dir : Direction.values()) {
                if (statsTag.contains(dir.name())) {
                    CompoundTag dirStats = statsTag.getCompound(dir.name());
                    TransferStats stats = transferStats.get(dir);
                    stats.totalTransferred = dirStats.getInt("Total");
                    stats.successfulTransfers = dirStats.getInt("Success");
                    stats.failedTransfers = dirStats.getInt("Failed");
                    stats.averageRate = dirStats.getDouble("Rate");
                }
            }
        }

        tickCounter = tag.getLong("TickCounter");
        networkDirty = true;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        // 清理緩存
        sharedCache.remove(worldPosition);
        sharedNetworkNodes.remove(worldPosition);
        lastScanTime.remove(worldPosition);
    }


}