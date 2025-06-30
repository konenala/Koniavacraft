package com.github.nalamodikk.common.block.conduit;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

import java.util.*;

/**
 * 融合 Mekanism 和 EnderIO 風格的智能魔力導管
 *
 * Mekanism 風格: 直通式傳輸，最小緩衝
 * EnderIO 風格: 智能路由，負載平衡
 * 你的特色: 統計記憶，性能優化
 */
public class ArcaneConduitBlockEntity extends BlockEntity implements IUnifiedManaHandler, IConfigurableBlock {

    private static final Logger LOGGER = LogUtils.getLogger();

    // === Mekanism 風格：最小化緩衝設計 ===
    private static final int BUFFER_SIZE = 100;     // 極小緩衝，主要用於臨時平衡
    private static final int TRANSFER_RATE = 200;   // 每tick傳輸量
    private static final int PULL_RATE = 50;        // 每次拉取量（防止過度拉取）

    // === EnderIO 風格：智能路由配置 ===
    private static final int NETWORK_SCAN_INTERVAL = 60;  // 3秒掃描一次網絡
    private static final int CACHE_REFRESH_INTERVAL = 20; // 1秒刷新緩存
    private static final int BALANCE_CHECK_INTERVAL = 10; // 0.5秒檢查負載平衡

    // === 核心組件 ===
    private final ManaStorage buffer = new ManaStorage(BUFFER_SIZE);
    private final EnumMap<Direction, IOHandlerUtils.IOType> ioConfig = new EnumMap<>(Direction.class);

    // === EnderIO 風格：智能網絡管理 ===
    private final Set<BlockPos> networkNodes = new HashSet<>();           // 網絡中的所有節點
    private final Map<Direction, ManaEndpoint> endpoints = new EnumMap<>(Direction.class);
    private final Map<Direction, Integer> routePriority = new EnumMap<>(Direction.class);

    // === 你的特色：統計與學習系統 ===
    private final EnumMap<Direction, TransferStats> transferStats = new EnumMap<>(Direction.class);

    // === 性能控制 ===
    private long tickCounter = 0;
    private boolean networkDirty = true;
    private int roundRobinIndex = 0;

    // === 內部數據結構 ===

    /**
     * Mekanism 風格：端點信息
     */
    private static class ManaEndpoint {
        final IUnifiedManaHandler handler;
        final boolean isConduit;          // 是否為導管（避免循環）
        final int priority;               // 優先級（距離或配置）
        long lastAccess;

        ManaEndpoint(IUnifiedManaHandler handler, boolean isConduit, int priority) {
            this.handler = handler;
            this.isConduit = isConduit;
            this.priority = priority;
            this.lastAccess = System.currentTimeMillis();
        }
    }

    /**
     * 你的特色：傳輸統計
     */
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
                // 指數移動平均
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

    public ArcaneConduitBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ARCANE_CONDUIT_BE.get(), pos, blockState);

        // 初始化 IO 配置（默認全雙向）
        for (Direction dir : Direction.values()) {
            ioConfig.put(dir, IOHandlerUtils.IOType.BOTH);
            transferStats.put(dir, new TransferStats());
            routePriority.put(dir, 50); // 默認優先級
        }
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        tickCounter++;

        // === EnderIO 風格：分階段處理 ===
        if (tickCounter % NETWORK_SCAN_INTERVAL == 0 || networkDirty) {
            scanNetworkTopology();
            networkDirty = false;
        }

        if (tickCounter % CACHE_REFRESH_INTERVAL == 0) {
            refreshEndpointCache();
        }

        if (tickCounter % BALANCE_CHECK_INTERVAL == 0) {
            performLoadBalancing();
        }

        // === Mekanism 風格：主動處理流量 ===
        handleManaFlow();

        // === 清理過期數據 ===
        if (tickCounter % 1200 == 0) { // 每分鐘清理
            cleanupStaleData();
        }
    }

    /**
     * EnderIO 風格：網絡拓撲掃描
     */
    private void scanNetworkTopology() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        networkNodes.clear();
        endpoints.clear();

        for (Direction dir : Direction.values()) {
            if (ioConfig.get(dir) == IOHandlerUtils.IOType.DISABLED) continue;

            BlockPos neighborPos = worldPosition.relative(dir);

            // 使用你的工具類查詢能力
            IUnifiedManaHandler handler = CapabilityUtils.getNeighborMana(level, neighborPos, dir);
            if (handler == null) continue;

            // 檢查是否為導管（避免循環）
            boolean isConduit = level.getBlockEntity(neighborPos) instanceof ArcaneConduitBlockEntity;

            // 計算優先級（距離 + 配置 + 歷史性能）
            int priority = calculatePriority(dir, handler, isConduit);

            endpoints.put(dir, new ManaEndpoint(handler, isConduit, priority));
            networkNodes.add(neighborPos);
        }

        LOGGER.debug("掃描網絡: 發現 {} 個端點", endpoints.size());
    }

    /**
     * EnderIO 風格：優先級計算
     */
    private int calculatePriority(Direction dir, IUnifiedManaHandler handler, boolean isConduit) {
        int basePriority = routePriority.get(dir);

        // 導管優先級較低（避免在導管間過度傳輸）
        if (isConduit) basePriority -= 20;

        // 根據歷史性能調整
        TransferStats stats = transferStats.get(dir);
        double reliability = stats.getReliability();
        basePriority += (int) (reliability * 30 - 15); // -15 到 +15

        // 根據目標需求調整
        if (handler.canReceive()) {
            int demand = handler.getMaxManaStored() - handler.getManaStored();
            if (demand > TRANSFER_RATE) basePriority += 10; // 高需求優先
        }

        return Math.max(0, Math.min(100, basePriority));
    }

    /**
     * Mekanism 風格：刷新端點緩存
     */
    private void refreshEndpointCache() {
        // 檢查端點是否仍然有效
        endpoints.entrySet().removeIf(entry -> {
            Direction dir = entry.getKey();
            ManaEndpoint endpoint = entry.getValue();

            // 重新獲取能力
            IUnifiedManaHandler current = CapabilityUtils.getNeighborMana(level,
                    worldPosition.relative(dir), dir);

            return current == null || current != endpoint.handler;
        });
    }

    /**
     * EnderIO 風格：負載平衡處理
     */
    private void performLoadBalancing() {
        if (buffer.getManaStored() <= 0) return;

        // 獲取所有可輸出的端點
        List<Map.Entry<Direction, ManaEndpoint>> outputs = endpoints.entrySet().stream()
                .filter(e -> canOutput(e.getKey()) && e.getValue().handler.canReceive())
                .sorted((a, b) -> Integer.compare(b.getValue().priority, a.getValue().priority))
                .toList();

        if (outputs.isEmpty()) return;

        int totalToTransfer = Math.min(buffer.getManaStored(), TRANSFER_RATE);

        // === EnderIO 風格：智能分配 ===
        distributeIntelligently(outputs, totalToTransfer);
    }

    /**
     * 你的特色：智能分配算法
     */
    private void distributeIntelligently(List<Map.Entry<Direction, ManaEndpoint>> outputs, int totalAmount) {
        if (outputs.isEmpty()) return;

        // 計算每個端點的需求和權重
        List<TransferTarget> targets = new ArrayList<>();
        int totalWeight = 0;

        for (var entry : outputs) {
            Direction dir = entry.getKey();
            ManaEndpoint endpoint = entry.getValue();
            IUnifiedManaHandler handler = endpoint.handler;

            int demand = handler.getMaxManaStored() - handler.getManaStored();
            if (demand <= 0) continue;

            // 權重 = 優先級 × 需求比例 × 可靠性
            TransferStats stats = transferStats.get(dir);
            double reliability = stats.getReliability();
            double demandRatio = Math.min(1.0, (double) demand / TRANSFER_RATE);

            int weight = (int) (endpoint.priority * demandRatio * reliability);

            targets.add(new TransferTarget(dir, handler, demand, weight));
            totalWeight += weight;
        }

        // 按權重分配
        int remaining = totalAmount;
        for (TransferTarget target : targets) {
            if (remaining <= 0) break;

            int allocation = totalWeight > 0 ?
                    (totalAmount * target.weight / totalWeight) :
                    (remaining / targets.size());

            allocation = Math.min(allocation, Math.min(remaining, target.demand));

            if (allocation > 0) {
                performTransfer(target.direction, target.handler, allocation);
                remaining -= allocation;
            }
        }
    }

    /**
     * Mekanism 風格：執行傳輸
     */
    private void performTransfer(Direction dir, IUnifiedManaHandler target, int amount) {
        // 模擬傳輸
        int accepted = target.receiveMana(amount, ManaAction.SIMULATE);
        if (accepted <= 0) {
            transferStats.get(dir).recordTransfer(0, false);
            return;
        }

        // 實際傳輸
        int extracted = buffer.extractMana(accepted, ManaAction.EXECUTE);
        if (extracted > 0) {
            int inserted = target.receiveMana(extracted, ManaAction.EXECUTE);

            // 記錄統計
            transferStats.get(dir).recordTransfer(inserted, inserted > 0);

            if (inserted != extracted) {
                // 如果沒有完全插入，返還剩餘魔力
                buffer.receiveMana(extracted - inserted, ManaAction.EXECUTE);
            }

            setChanged();
        }
    }

    /**
     * Mekanism 風格：處理魔力流動
     */
    private void handleManaFlow() {
        // 1. 從輸入端拉取魔力（限制拉取量）
        if (buffer.getManaStored() < BUFFER_SIZE) {
            pullManaFromInputs();
        }
    }

    /**
     * 改進的拉取邏輯
     */
    private void pullManaFromInputs() {
        int needed = BUFFER_SIZE - buffer.getManaStored();
        if (needed <= 0) return;

        // 輪轉拉取，避免總是從同一個源拉取
        Direction[] dirs = Direction.values();
        int attempts = 0;

        while (needed > 0 && attempts < dirs.length) {
            Direction dir = dirs[roundRobinIndex];
            roundRobinIndex = (roundRobinIndex + 1) % dirs.length;
            attempts++;

            if (!canInput(dir)) continue;

            ManaEndpoint endpoint = endpoints.get(dir);
            if (endpoint == null || endpoint.isConduit) continue; // 不從其他導管拉取

            IUnifiedManaHandler source = endpoint.handler;
            if (!source.canExtract()) continue;

            int toPull = Math.min(needed, PULL_RATE);
            int extracted = source.extractMana(toPull, ManaAction.EXECUTE);

            if (extracted > 0) {
                buffer.receiveMana(extracted, ManaAction.EXECUTE);
                needed -= extracted;

                // 記錄拉取統計
                transferStats.get(dir).recordTransfer(extracted, true);
                setChanged();

                // 限制：每tick只從一個源拉取
                break;
            }
        }
    }

    /**
     * 清理過期數據
     */
    private void cleanupStaleData() {
        long now = System.currentTimeMillis();

        // 清理長時間無傳輸的統計
        transferStats.values().forEach(stats -> {
            if (now - stats.lastTransfer > 60000) { // 1分鐘
                stats.averageRate *= 0.5; // 衰減
            }
        });
    }

    // === 工具方法 ===

    private boolean canInput(Direction dir) {
        IOHandlerUtils.IOType type = ioConfig.get(dir);
        return type == IOHandlerUtils.IOType.INPUT || type == IOHandlerUtils.IOType.BOTH;
    }

    private boolean canOutput(Direction dir) {
        IOHandlerUtils.IOType type = ioConfig.get(dir);
        return type == IOHandlerUtils.IOType.OUTPUT || type == IOHandlerUtils.IOType.BOTH;
    }

    // === 渲染器需要的方法 ===

    public int getActiveConnectionCount() {
        return (int) endpoints.values().stream()
                .filter(endpoint -> !endpoint.isConduit)
                .count();
    }

    public int getTransferHistory(Direction direction) {
        TransferStats stats = transferStats.get(direction);
        return stats != null ? stats.totalTransferred : 0;
    }

    // === 調試接口 ===

    public Map<Direction, TransferStats> getTransferStats() {
        return new EnumMap<>(transferStats);
    }

    public Set<BlockPos> getNetworkNodes() {
        return new HashSet<>(networkNodes);
    }

    public void setDirectionConfig(Direction dir, IOHandlerUtils.IOType type) {
        ioConfig.put(dir, type);
        networkDirty = true;
        setChanged();
    }

    // === 內部類 ===

    private record TransferTarget(Direction direction, IUnifiedManaHandler handler,
                                  int demand, int weight) {}

    // === IUnifiedManaHandler 完整實現 ===

    @Override
    public int receiveMana(int maxReceive, ManaAction action) {
        return buffer.receiveMana(maxReceive, action);
    }

    @Override
    public int extractMana(int maxExtract, ManaAction action) {
        return buffer.extractMana(maxExtract, action);
    }

    @Override
    public int getManaContainerCount() {
        return 1; // 導管只有一個緩衝容器
    }

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
        if (container == 0) {
            return buffer.receiveMana(amount, action);
        }
        return 0;
    }

    @Override
    public int extractMana(int container, int amount, ManaAction action) {
        if (container == 0) {
            return buffer.extractMana(amount, action);
        }
        return 0;
    }

    @Override
    public int getManaStored() {
        return buffer.getManaStored();
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
    public int getMaxManaStored() {
        return buffer.getMaxManaStored();
    }

    @Override
    public boolean canExtract() {
        return buffer.getManaStored() > 0; // 有魔力時才能被提取
    }

    @Override
    public boolean canReceive() {
        return buffer.getManaStored() < buffer.getMaxManaStored();
    }

    // === NBT 處理 ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 保存緩衝區
        tag.put("Buffer", buffer.serializeNBT(registries));

        // 保存 IO 配置
        CompoundTag ioTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            ioTag.putString(dir.name(), ioConfig.get(dir).name());
        }
        tag.put("IOConfig", ioTag);

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

        // 加載緩衝區
        if (tag.contains("Buffer")) {
            buffer.deserializeNBT(registries, tag.getCompound("Buffer"));
        }

        // 加載 IO 配置
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

        // 加載統計數據
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
        networkDirty = true; // 加載後需要重新掃描網絡
    }

    // === 網絡同步觸發 ===

    public void onNeighborChanged() {
        networkDirty = true;
    }

    @Override
    public IOHandlerUtils.IOType getIOConfig(Direction direction) {
        return ioConfig.getOrDefault(direction, IOHandlerUtils.IOType.BOTH);
    }

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        IOHandlerUtils.IOType oldType = ioConfig.get(direction);
        if (oldType != type) {
            ioConfig.put(direction, type);
            networkDirty = true; // 觸發網絡重新掃描
            setChanged();

            // 清除該方向的緩存
            endpoints.remove(direction);

            // 日誌記錄
            LOGGER.debug("導管 {} 方向 {} 設定從 {} 改為 {}",
                    worldPosition, direction, oldType, type);
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
            endpoints.clear(); // 清除所有緩存
            setChanged();
            LOGGER.debug("導管 {} 批量更新IO配置", worldPosition);
        }
    }
    public InteractionResult onUse(BlockState state, Level level, BlockPos pos,
                                   Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack heldItem = player.getMainHandItem();

        // 檢查是否手持科技魔杖
        if (heldItem.getItem() instanceof BasicTechWandItem wand) {
            BasicTechWandItem.TechWandMode mode = wand.getMode(heldItem);
            Direction hitFace = hit.getDirection();

            switch (mode) {
                case DIRECTION_CONFIG -> {
                    // 單方向配置模式
                    IOHandlerUtils.IOType current = getIOConfig(hitFace);
                    IOHandlerUtils.IOType next = IOHandlerUtils.nextIOType(current);
                    setIOConfig(hitFace, next);

                    String dirName = hitFace.name().toLowerCase();
                    String typeName = next.name().toLowerCase();

                    player.displayClientMessage(Component.translatable(
                            "message.koniava.wrench.conduit_mode",
                            Component.translatable("direction.koniava." + dirName),
                            Component.translatable("mode.koniava." + typeName)
                    ), true);

                    return InteractionResult.SUCCESS;
                }

                case CONFIGURE_IO -> {
                    // 打開IO配置GUI（如果你有UniversalConfigMenu的話）
                    if (player instanceof ServerPlayer serverPlayer) {
                        // 這個可以先註釋掉，等你確認有GUI再啟用
                        // openIOConfigurationGUI(serverPlayer, heldItem);

                        // 暫時顯示當前配置
                        showConduitInfo(player);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }

        // 空手右鍵顯示信息
        if (heldItem.isEmpty()) {
            showConduitInfo(player);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }


    private void showConduitInfo(Player player) {
        // 使用本地化的標題
        player.displayClientMessage(Component.translatable("message.koniava.conduit.info_header"), false);

        // 使用本地化的魔力狀態
        player.displayClientMessage(Component.translatable(
                "message.koniava.conduit.mana_status",
                getManaStored(),
                getMaxManaStored()
        ), false);

        // 使用本地化的連接數
        player.displayClientMessage(Component.translatable(
                "message.koniava.conduit.connections",
                getActiveConnectionCount()
        ), false);

        // 顯示各方向IO配置 - 使用本地化
        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType type = getIOConfig(dir);

            // 獲取本地化的方向名稱和類型名稱
            Component dirName = Component.translatable("direction.koniava." + dir.name().toLowerCase());
            Component typeName = Component.translatable("mode.koniava." + type.name().toLowerCase());

            String color = switch (type) {
                case INPUT -> "§2"; // 深綠色
                case OUTPUT -> "§c"; // 紅色
                case BOTH -> "§b"; // 青色
                case DISABLED -> "§8"; // 深灰色
            };

            // 使用本地化的配置顯示格式
            player.displayClientMessage(Component.translatable(
                    "message.koniava.conduit.direction_config",
                    dirName,
                    Component.literal(color).append(typeName)
            ), false);
        }
    }

}