package com.github.nalamodikk.common.block.conduit;// 🏗️ 簡化後的 ArcaneConduitBlockEntity.java 主要修改

// === 1. 在頂部添加所有 Manager imports ===

import com.github.nalamodikk.common.block.conduit.manager.*;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ArcaneConduitBlockEntity extends BlockEntity implements IUnifiedManaHandler, IConfigurableBlock {

    // === 保留的常量和靜態字段 ===
    private static final int BUFFER_SIZE = 100;
    private static final int NETWORK_SCAN_INTERVAL = 600;
    public static final Logger LOGGER = LogUtils.getLogger();

    // 保留全域緩存管理的靜態字段（由CacheManager管理）
    private static int globalTickOffset = 0;
    private static final Map<BlockPos, Integer> conduitTickOffsets = new ConcurrentHashMap<>();

    // === 🆕 組件化核心 ===
    private final ManaStorage buffer = new ManaStorage(BUFFER_SIZE);
    private final ConduitIOManager ioManager;
    private final ConduitStatsManager statsManager;
    private final ConduitCacheManager cacheManager;
    private final ConduitNetworkManager networkManager;
    private final ConduitTransferManager transferManager;

    // === 簡化的狀態 ===
    private int tickOffset;



    // === 🆕 簡化的建構子 ===
    public ArcaneConduitBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ARCANE_CONDUIT_BE.get(), pos, blockState);

        // 初始化所有管理器
        this.ioManager = new ConduitIOManager();
        this.statsManager = new ConduitStatsManager();
        this.cacheManager = new ConduitCacheManager(pos);

        // 設定tick偏移
        this.tickOffset = conduitTickOffsets.computeIfAbsent(pos,
                k -> (globalTickOffset++) % NETWORK_SCAN_INTERVAL);

        // 初始化需要相互引用的管理器
        this.networkManager = new ConduitNetworkManager(this, cacheManager, ioManager, tickOffset);
        this.transferManager = new ConduitTransferManager(this, networkManager, statsManager, ioManager);

        // 設定回調監聽器
        setupEventListeners();
    }

    // === 🆕 設定事件監聽器 ===
    private void setupEventListeners() {
        ioManager.setChangeListener(new ConduitIOManager.IOConfigChangeListener() {
            @Override
            public void onIOConfigChanged(Direction direction, IOHandlerUtils.IOType newType) {
                handleIOConfigChange(direction, newType);
            }

            @Override
            public void onPriorityChanged(Direction direction, int newPriority) {
                handlePriorityChange(direction, newPriority);
            }
        });
    }

    // === 🆕 事件處理回調 ===
    private void handleIOConfigChange(Direction direction, IOHandlerUtils.IOType newType) {
        // 通知網路管理器
        networkManager.onDirectionConfigChanged(direction);
        setChanged();

        // 通知相鄰導管
        if (level != null && !level.isClientSide) {
            BlockPos neighborPos = worldPosition.relative(direction);
            BlockEntity neighborBE = level.getBlockEntity(neighborPos);
            if (neighborBE instanceof ArcaneConduitBlockEntity neighborConduit) {
                neighborConduit.markNetworkDirty();
            }
        }

        // 更新方塊狀態連接
        updateBlockStateConnections();
    }

    private void handlePriorityChange(Direction direction, int newPriority) {
        networkManager.markDirty();
        setChanged();
    }

    // === 🆕 超級簡化的 tick 方法 ===
    public void tick() {
        if (level == null || level.isClientSide) return;

        // 更新統計管理器
        statsManager.tick();

        // 如果閒置且沒有魔力，降低處理頻率
        if (statsManager.isIdle() && buffer.getManaStored() == 0) {
            if (statsManager.getTickCounter() % 200 != tickOffset % 200) {
                return;
            }
        }

        // 各管理器協調工作
        networkManager.updateIfNeeded(statsManager.getTickCounter());
        transferManager.processManaFlow();

        // 定期維護
        if (statsManager.getTickCounter() % 72000 == tickOffset) { // 1小時
            performMaintenance();
        }

        if (statsManager.getTickCounter() % 12000 == tickOffset) { // 10分鐘
            networkManager.performPassiveCleanup();
        }
    }

    // === 🆕 簡化的維護方法 ===
    private void performMaintenance() {
        statsManager.performMaintenance();
        cacheManager.cleanup();
    }

    // === 🆕 委派給管理器的方法 ===

    // IO 配置委派
    @Override
    public IOHandlerUtils.IOType getIOConfig(Direction direction) {
        return ioManager.getIOConfig(direction);
    }

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        ioManager.setIOConfig(direction, type);
    }

    @Override
    public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
        return ioManager.getIOMap();
    }

    @Override
    public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> newIOMap) {
        ioManager.setIOMap(newIOMap);
    }

    // 優先級委派
    public void setPriority(Direction direction, int priority) {
        ioManager.setPriority(direction, priority);
    }

    public int getPriority(Direction direction) {
        return ioManager.getPriority(direction);
    }

    public void resetAllPriorities() {
        ioManager.resetAllPriorities();
    }

    // 統計委派
    public int getActiveConnectionCount() {
        return networkManager.getActiveConnectionCount();
    }

    public Map<Direction, ConduitStatsManager.TransferStats> getTransferStats() {
        return statsManager.getAllTransferStats();
    }

    public int getTransferHistory(Direction direction) {
        return statsManager.getTransferHistory(direction);
    }

    public boolean isTransferringMana(Direction direction) {
        ConduitStatsManager.TransferStats stats = statsManager.getTransferStats(direction);
        if (stats == null) return false;

        long currentTime = System.currentTimeMillis();
        return (currentTime - stats.lastTransfer) < 1000;
    }

    // 連接查詢委派
    public boolean hasConnectionInDirection(Direction direction) {
        return networkManager.hasConnection(direction);
    }

    public boolean isConnectedToConduit(Direction direction) {
        return networkManager.isConnectedToConduit(direction);
    }

    // === 🆕 簡化的接收魔力方法 ===
    public int receiveManaFromDirection(int maxReceive, ManaAction action, Direction fromDirection) {
        return transferManager.receiveManaFromDirection(maxReceive, action, fromDirection);
    }

    // === 🆕 簡化的鄰居變化處理 ===
    public void onNeighborChanged() {
        LOGGER.debug("Neighbor changed for conduit at {}", worldPosition);

        // 委派給網路管理器
        networkManager.onNeighborChanged();

        // 更新方塊狀態
        if (level != null && !level.isClientSide) {
            updateBlockStateConnections();

            // 通知所有相鄰的導管也重新掃描
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(dir);
                BlockEntity neighborBE = level.getBlockEntity(neighborPos);

                if (neighborBE instanceof ArcaneConduitBlockEntity neighborConduit) {
                    neighborConduit.markNetworkDirty();
                }
            }
        }

        LOGGER.debug("Network state reset for conduit at {}", worldPosition);
    }

    // === 🆕 簡化的移除處理 ===
    @Override
    public void setRemoved() {
        LOGGER.debug("Removing conduit at {}", worldPosition);

        try {
            // 委派給緩存管理器清理
            cacheManager.invalidateAll();

            LOGGER.debug("Conduit removed successfully: {}", worldPosition);
        } catch (Exception e) {
            LOGGER.error("Error during cleanup: {}", e.getMessage());
        }

        super.setRemoved();
    }

    // === 🆕 簡化的網路標記 ===
    public void markNetworkDirty() {
        networkManager.markDirty();
        setChanged();
    }

    // === 🆕 超級簡化的 NBT 序列化 ===
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 保存緩衝區
        tag.put("Buffer", buffer.serializeNBT(registries));

        // 委派給各管理器
        ioManager.saveToNBT(tag);
        statsManager.saveToNBT(tag);
        transferManager.saveToNBT(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // 載入緩衝區
        if (tag.contains("Buffer")) {
            buffer.deserializeNBT(registries, tag.getCompound("Buffer"));
        }

        // 委派給各管理器
        ioManager.loadFromNBT(tag);
        statsManager.loadFromNBT(tag);
        transferManager.loadFromNBT(tag);

        // 標記網路需要重新掃描
        networkManager.markDirty();
    }

    // === 🆕 簡化的載入處理 ===
    @Override
    public void onLoad() {
        super.onLoad();

        if (level != null) {
            // 標記需要驗證網路狀態
            networkManager.markDirty();
            statsManager.recordActivity();
        }
    }

    // === 保留的 IUnifiedManaHandler 實現 ===
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
    public int getManaContainerCount() {
        return 1;
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
        return container == 0 ? buffer.receiveMana(amount, action) : 0;
    }

    @Override
    public int extractMana(int container, int amount, ManaAction action) {
        return container == 0 ? buffer.extractMana(amount, action) : 0;
    }

    // === 保留的用戶交互邏輯 ===
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

    // === 保留的信息顯示方法 ===
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

    // === 保留的輔助方法 ===
    private void updateBlockStateConnections() {
        BlockState currentState = level.getBlockState(worldPosition);
        if (currentState.getBlock() instanceof ArcaneConduitBlock conduitBlock) {
            BlockState newState = conduitBlock.updateConnections(level, worldPosition, currentState);
            if (newState != currentState) {
                level.setBlock(worldPosition, newState, 3);
            }
        }
    }

    // === 🆕 靜態清理方法（保留但簡化） ===
    public static void clearAllStaticCachesGracefully() {
        try {
            LOGGER.info("Starting graceful static cache cleanup");

            // 委派給緩存管理器
            ConduitCacheManager.clearAllStaticCaches();

            // 清理其他靜態數據
            conduitTickOffsets.clear();

            LOGGER.info("Graceful cleanup completed");

        } catch (Exception e) {
            LOGGER.error("Error during graceful cleanup: {}", e.getMessage());
        }
    }

    public static void performMaintenanceCleanup() {
        ConduitCacheManager.performGlobalMaintenance();
    }
}

// === 🎉 重構完成！ ===
// 主類從 1400+ 行縮減到約 400 行
// 所有複雜邏輯都分離到專門的管理器中
// 代碼結構清晰，易於維護和擴展