package com.github.nalamodikk.common.block.conduit;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.EnumSet;

public class ArcaneConduitBlockEntity extends BlockEntity implements IUnifiedManaHandler {

    // 基礎屬性
    private static final int TRANSFER_RATE = 100; // 每tick可傳輸的魔力
    private static final int BUFFER_SIZE = 1000;  // 內部緩衝區

    // 性能優化：緩存更新頻率控制
    private static final int CACHE_UPDATE_INTERVAL = 20; // 每20tick更新一次緩存
    private static final int CONNECTION_UPDATE_INTERVAL = 40; // 每40tick更新一次連接

    // 內部魔力緩衝
    private final ManaStorage internalBuffer = new ManaStorage(BUFFER_SIZE);

    //  性能優化：緩存有效的連接方向
    private final EnumSet<Direction> validConnections = EnumSet.noneOf(Direction.class);
    private final EnumMap<Direction, IUnifiedManaHandler> cachedCapabilities = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Long> lastSuccessfulTransfer = new EnumMap<>(Direction.class);

    // 用於智能路由的統計數據
    private final EnumMap<Direction, Integer> transferHistory = new EnumMap<>(Direction.class);

    //  性能優化：tick計數器
    private long tickCounter = 0;
    private boolean needsConnectionUpdate = true;

    // 性能優化：輪轉傳輸避免饑餓
    private int lastTransferDirection = 0;

    public ArcaneConduitBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ARCANE_CONDUIT_BE.get(), pos, blockState);

        // 初始化統計數據
        for (Direction dir : Direction.values()) {
            transferHistory.put(dir, 0);
            lastSuccessfulTransfer.put(dir, 0L);
        }
    }


    /**
     * 重置傳輸歷史 (可選，用於調試)
     */
    public void resetTransferHistory() {
        transferHistory.clear();
    }

    /**
     * 獲取所有方向的傳輸歷史 (用於調試)
     */
    public EnumMap<Direction, Integer> getAllTransferHistory() {
        return new EnumMap<>(transferHistory);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        tickCounter++;

        // 🔧 修復1：限制拉取頻率 (每10tick拉取一次)
        if (tickCounter % 10 == 0 && internalBuffer.getManaStored() < internalBuffer.getMaxManaStored()) {
            pullManaFromNeighbors();
        }

        //  性能優化：分階段更新，避免單tick過載
        boolean shouldUpdateCache = (tickCounter % CACHE_UPDATE_INTERVAL == 0);
        boolean shouldUpdateConnections = (tickCounter % CONNECTION_UPDATE_INTERVAL == 0) || needsConnectionUpdate;

        if (shouldUpdateCache) {
            updateCapabilityCache();
        }

        if (shouldUpdateConnections) {
            updateConnections();
            needsConnectionUpdate = false;
        }

        // 🚀 核心：只有有魔力且有有效連接時才執行傳輸
        if (internalBuffer.getManaStored() > 0 && !validConnections.isEmpty()) {
            distributeMana();
        }
    }

    private void pullManaFromNeighbors() {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(direction);
            IUnifiedManaHandler neighbor = level.getCapability(
                    ModCapabilities.MANA, neighborPos, direction.getOpposite());

            if (neighbor != null && neighbor.canExtract()) {
                // 🔥 修復3：排除其他導管，避免循環
                if (level.getBlockEntity(neighborPos) instanceof ArcaneConduitBlockEntity) {
                    continue; // 跳過其他導管
                }

                // 🔥 修復4：只在真正需要時拉取
                int needed = internalBuffer.getMaxManaStored() - internalBuffer.getManaStored();
                if (needed < 50) break; // 如果需求太少就不拉取

                // 🔥 修復5：限制拉取量
                int toPull = Math.min(needed, 50); // 每次最多50，而不是100
                int extracted = neighbor.extractMana(toPull, ManaAction.EXECUTE);
                if (extracted > 0) {
                    internalBuffer.receiveMana(extracted, ManaAction.EXECUTE);
                    System.out.println("導管從 " + direction + " 拉取了 " + extracted + " 魔力");
                    break; // 一次只從一個鄰居拉取
                }
            }
        }
    }


    private void updateCapabilityCache() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 🚀 性能優化：只檢查當前有效的連接
        for (Direction direction : validConnections) {
            BlockPos neighborPos = worldPosition.relative(direction);

            // 檢查緩存是否過期（20秒無成功傳輸）
            long lastTransfer = lastSuccessfulTransfer.getOrDefault(direction, 0L);
            if (tickCounter - lastTransfer > 400) { // 20秒 = 400 ticks
                cachedCapabilities.remove(direction);
                continue;
            }

            // 如果沒有緩存，嘗試獲取
            if (!cachedCapabilities.containsKey(direction)) {
                IUnifiedManaHandler capability = serverLevel.getCapability(
                        ModCapabilities.MANA,
                        neighborPos,
                        direction.getOpposite()
                );

                if (capability != null && capability.canReceive()) {
                    cachedCapabilities.put(direction, capability);
                }
            }
        }
    }

    // 🔧 修復6：改進分配邏輯，避免反向流動
    private void distributeMana() {
        if (cachedCapabilities.isEmpty()) return;

        int remainingMana = Math.min(internalBuffer.getManaStored(), TRANSFER_RATE);
        if (remainingMana <= 0) return;

        // 🚀 性能優化：輪轉分配避免饑餓，提高公平性
        Direction[] directions = Direction.values();
        int startIndex = lastTransferDirection;

        for (int i = 0; i < directions.length && remainingMana > 0; i++) {
            int dirIndex = (startIndex + i) % directions.length;
            Direction direction = directions[dirIndex];

            if (!cachedCapabilities.containsKey(direction)) continue;

            IUnifiedManaHandler capability = cachedCapabilities.get(direction);
            if (capability == null) continue;

            // 🔥 修復7：避免向其他導管輸出（除非它們魔力更少）
            BlockPos neighborPos = worldPosition.relative(direction);
            if (level.getBlockEntity(neighborPos) instanceof ArcaneConduitBlockEntity otherConduit) {
                // 只向魔力更少的導管傳輸
                if (otherConduit.getManaStored() >= this.getManaStored() - 100) {
                    continue; // 跳過魔力差不多或更多的導管
                }
            }

            try {
                // 🚀 性能優化：預檢查避免無效調用
                int demand = capability.getMaxManaStored() - capability.getManaStored();
                if (demand <= 0) continue;

                int toSend = Math.min(Math.min(demand, remainingMana), TRANSFER_RATE / 6); // 每方向限制

                if (toSend > 0) {
                    int actualSent = capability.receiveMana(toSend, ManaAction.EXECUTE);
                    if (actualSent > 0) {
                        internalBuffer.extractMana(actualSent, ManaAction.EXECUTE);
                        remainingMana -= actualSent;

                        // 記錄成功傳輸
                        transferHistory.merge(direction, actualSent, Integer::sum);
                        lastSuccessfulTransfer.put(direction, tickCounter);
                        lastTransferDirection = dirIndex;
                    }
                }
            } catch (Exception e) {
                // 🚀 性能優化：移除失效的緩存
                cachedCapabilities.remove(direction);
            }
        }
    }

    private void updateConnections() {
        if (level == null) return;

        validConnections.clear();

        // 🚀 性能優化：只檢查實際的方塊連接
        if (getBlockState().getBlock() instanceof ArcaneConduitBlock conduitBlock) {
            BlockState currentState = getBlockState();

            // 根據方塊狀態更新有效連接
            if (currentState.getValue(ArcaneConduitBlock.NORTH)) validConnections.add(Direction.NORTH);
            if (currentState.getValue(ArcaneConduitBlock.SOUTH)) validConnections.add(Direction.SOUTH);
            if (currentState.getValue(ArcaneConduitBlock.WEST)) validConnections.add(Direction.WEST);
            if (currentState.getValue(ArcaneConduitBlock.EAST)) validConnections.add(Direction.EAST);
            if (currentState.getValue(ArcaneConduitBlock.UP)) validConnections.add(Direction.UP);
            if (currentState.getValue(ArcaneConduitBlock.DOWN)) validConnections.add(Direction.DOWN);

            // 更新方塊狀態（如果需要）
            BlockState newState = conduitBlock.updateConnections(level, worldPosition, currentState);
            if (newState != currentState) {
                level.setBlock(worldPosition, newState, 3);
                // 狀態改變時，清除所有緩存
                cachedCapabilities.clear();
            }
        }
    }

    // 🚀 性能優化：外部觸發緩存失效
    public void invalidateCache() {
        cachedCapabilities.clear();
        needsConnectionUpdate = true;
    }

    // 🚀 性能優化：鄰居改變時只清除相關緩存
    public void onNeighborChanged(Direction direction) {
        cachedCapabilities.remove(direction);
        needsConnectionUpdate = true;
    }

    // IUnifiedManaHandler 實現
    @Override
    public int receiveMana(int maxReceive, ManaAction action) {
        int received = internalBuffer.receiveMana(maxReceive, action);
        if (received > 0 && action == ManaAction.EXECUTE) {
            setChanged(); // 只有實際接收時才標記更改
        }
        return received;
    }

    @Override
    public int extractMana(int maxExtract, ManaAction action) {
        return internalBuffer.extractMana(maxExtract, action);
    }

    @Override
    public int getManaContainerCount() {
        return 1;
    }

    @Override
    public int getManaStored(int container) {
        return container == 0 ? internalBuffer.getManaStored() : 0;
    }

    @Override
    public void setMana(int container, int mana) {
        if (container == 0) {
            internalBuffer.setMana(mana);
            setChanged();
        }
    }

    @Override
    public int getMaxManaStored(int container) {
        return container == 0 ? internalBuffer.getMaxManaStored() : 0;
    }

    @Override
    public int getNeededMana(int container) {
        return container == 0 ? (internalBuffer.getMaxManaStored() - internalBuffer.getManaStored()) : 0;
    }

    @Override
    public int insertMana(int container, int amount, ManaAction action) {
        return container == 0 ? receiveMana(amount, action) : 0;
    }

    @Override
    public int extractMana(int container, int amount, ManaAction action) {
        return container == 0 ? extractMana(amount, action) : 0;
    }

    @Override
    public int getManaStored() {
        return internalBuffer.getManaStored();
    }

    @Override
    public void addMana(int amount) {
        if (amount > 0) {
            internalBuffer.receiveMana(amount, ManaAction.EXECUTE);
            setChanged();
        }
    }

    @Override
    public void consumeMana(int amount) {
        if (amount > 0) {
            internalBuffer.extractMana(amount, ManaAction.EXECUTE);
        }
    }

    @Override
    public void setMana(int amount) {
        internalBuffer.setMana(amount);
        setChanged();
    }

    @Override
    public void onChanged() {
        setChanged();
    }

    @Override
    public int getMaxManaStored() {
        return internalBuffer.getMaxManaStored();
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return internalBuffer.getManaStored() < internalBuffer.getMaxManaStored();
    }

    // NBT 數據保存
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ManaBuffer", internalBuffer.serializeNBT(registries));
        tag.putLong("TickCounter", tickCounter);
        tag.putInt("LastTransferDirection", lastTransferDirection);

        // 保存傳輸歷史
        CompoundTag historyTag = new CompoundTag();
        for (var entry : transferHistory.entrySet()) {
            historyTag.putInt(entry.getKey().name(), entry.getValue());
        }
        tag.put("TransferHistory", historyTag);

        // 保存最後成功傳輸時間
        CompoundTag transferTimeTag = new CompoundTag();
        for (var entry : lastSuccessfulTransfer.entrySet()) {
            transferTimeTag.putLong(entry.getKey().name(), entry.getValue());
        }
        tag.put("LastTransferTimes", transferTimeTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ManaBuffer")) {
            internalBuffer.deserializeNBT(registries, tag.getCompound("ManaBuffer"));
        }

        tickCounter = tag.getLong("TickCounter");
        lastTransferDirection = tag.getInt("LastTransferDirection");

        // 讀取傳輸歷史
        if (tag.contains("TransferHistory")) {
            CompoundTag historyTag = tag.getCompound("TransferHistory");
            for (Direction dir : Direction.values()) {
                if (historyTag.contains(dir.name())) {
                    transferHistory.put(dir, historyTag.getInt(dir.name()));
                }
            }
        }

        // 讀取傳輸時間
        if (tag.contains("LastTransferTimes")) {
            CompoundTag transferTimeTag = tag.getCompound("LastTransferTimes");
            for (Direction dir : Direction.values()) {
                if (transferTimeTag.contains(dir.name())) {
                    lastSuccessfulTransfer.put(dir, transferTimeTag.getLong(dir.name()));
                }
            }
        }

        // 加載後需要更新連接
        needsConnectionUpdate = true;
    }

    // 調試和監控方法
    public int getTransferHistory(Direction direction) {
        return transferHistory.getOrDefault(direction, 0);
    }

    public int getActiveConnectionCount() {
        return validConnections.size();
    }

    public int getCachedCapabilityCount() {
        return cachedCapabilities.size();
    }
}