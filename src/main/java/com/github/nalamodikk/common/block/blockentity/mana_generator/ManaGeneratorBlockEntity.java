    // ⚠ 自動產生：結合 NeoForge 能量與魔力產出邏輯
    package com.github.nalamodikk.common.block.blockentity.mana_generator;

    import com.github.nalamodikk.KoniavacraftMod;
    import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
    import com.github.nalamodikk.common.block.blockentity.mana_generator.logic.*;
    import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.loader.ManaGenFuelRateLoader;
    import com.github.nalamodikk.common.block.blockentity.mana_generator.sync.ManaGeneratorSyncHelper;
    import com.github.nalamodikk.common.block.blockentity.manabase.AbstractManaMachineEntityBlock;
    import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
    import com.github.nalamodikk.common.capability.ManaStorage;
    import com.github.nalamodikk.common.compat.energy.ModNeoNalaEnergyStorage;
    import com.github.nalamodikk.common.coreapi.machine.logic.gen.EnergyGenerationHandler;
    import com.github.nalamodikk.common.coreapi.machine.logic.gen.FuelManaGenHelper;
    import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
    import com.github.nalamodikk.common.utils.upgrade.UpgradeInventory;
    import com.github.nalamodikk.common.utils.upgrade.api.IUpgradeableMachine;
    import com.github.nalamodikk.register.ModBlockEntities;
    import com.github.nalamodikk.register.ModCapabilities;
    import net.minecraft.core.BlockPos;
    import net.minecraft.core.Direction;
    import net.minecraft.core.HolderLookup;
    import net.minecraft.nbt.CompoundTag;
    import net.minecraft.network.chat.Component;
    import net.minecraft.network.protocol.Packet;
    import net.minecraft.network.protocol.game.ClientGamePacketListener;
    import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
    import net.minecraft.server.level.ServerLevel;
    import net.minecraft.world.Container;
    import net.minecraft.world.WorldlyContainer;
    import net.minecraft.world.entity.player.Inventory;
    import net.minecraft.world.entity.player.Player;
    import net.minecraft.world.inventory.AbstractContainerMenu;
    import net.minecraft.world.inventory.ContainerData;
    import net.minecraft.world.inventory.ContainerLevelAccess;
    import net.minecraft.world.item.ItemStack;
    import net.minecraft.world.level.Level;
    import net.minecraft.world.level.block.Block;
    import net.minecraft.world.level.block.entity.BlockEntity;
    import net.minecraft.world.level.block.entity.BlockEntityTicker;
    import net.minecraft.world.level.block.entity.BlockEntityType;
    import net.minecraft.world.level.block.state.BlockState;
    import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
    import net.neoforged.neoforge.capabilities.Capabilities;
    import net.neoforged.neoforge.energy.IEnergyStorage;
    import net.neoforged.neoforge.items.IItemHandler;
    import net.neoforged.neoforge.items.ItemStackHandler;
    import org.jetbrains.annotations.Nullable;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.util.EnumMap;
    import java.util.Optional;

    public class ManaGeneratorBlockEntity extends AbstractManaMachineEntityBlock implements Container, WorldlyContainer, IUpgradeableMachine {

        private static final Logger LOGGER = LoggerFactory.getLogger(ManaGeneratorBlockEntity.class);
        private final EnumMap<Direction, BlockCapabilityCache<IUnifiedManaHandler, Direction>> manaCaches = new EnumMap<>(Direction.class);
        private final EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> energyCaches = new EnumMap<>(Direction.class);
        private volatile boolean isSyncing = false;
        private long lastSyncTime = 0;
        private static final long MIN_SYNC_INTERVAL = 100; // milliseconds

        private static final int MAX_MANA = 200000;
        private static final int MAX_ENERGY = 200000;
        private static final int TICK_INTERVAL = 1;
        private static final int MANA_PER_CYCLE = 10;
        private static final int FUEL_SLOT_COUNT = 1;
        private static final int UPGRADE_SLOT_COUNT = 4;
        private static final int MANA_STORED_INDEX = 0;
        private static final int ENERGY_STORED_INDEX = 1;
        private static final int MODE_INDEX = 2;
        private static final int BURN_TIME_INDEX = 3;
        private static final int CURRENT_BURN_TIME_INDEX = 4;
        private static final int DEFAULT_ENERGY_PER_TICK = 40; // 或你想用的預設值
        // 替代原本的 UnifiedSyncManager syncManager
        private final ManaGeneratorSyncHelper syncHelper = new ManaGeneratorSyncHelper();
        private final FuelManaGenHelper manaGenHandler;
        private final EnergyGenerationHandler energyGenHandler;
        private final ManaGeneratorTicker ticker = new ManaGeneratorTicker(this);
        private final EnumMap<Direction, IOHandlerUtils.IOType> ioMap = new EnumMap<>(Direction.class);
        private final OutputHandler.OutputThrottleController outputThrottle = new OutputHandler.OutputThrottleController();

        private final ManaGeneratorStateManager stateManager = new ManaGeneratorStateManager();


        private final ItemStackHandler fuelHandler = new ItemStackHandler(FUEL_SLOT_COUNT);
        private final UpgradeInventory upgradeInventory = new UpgradeInventory(UPGRADE_SLOT_COUNT);
        private ContainerLevelAccess access;
        // ✅ 用來避免每幀都重播動畫，造成動畫 reset、跳針或閃爍
        private String currentAnimation = "";
        private boolean forceRefreshAnimation = false;

        private final ManaFuelHandler fuelLogic = new ManaFuelHandler(fuelHandler,stateManager);
        private final ManaGeneratorUpgradeHandler upgradeHandler = new ManaGeneratorUpgradeHandler(upgradeInventory);
        public OutputHandler.OutputThrottleController getOutputThrottle() {return outputThrottle;}

        public ManaGeneratorBlockEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.MANA_GENERATOR_BE.get(), pos, state, true, MAX_MANA, MAX_ENERGY, TICK_INTERVAL, MANA_PER_CYCLE);
            if (this.level != null) {
                this.access = ContainerLevelAccess.create(this.level, this.worldPosition);
            }
            this.manaGenHandler = new FuelManaGenHelper(this.manaStorage, this::getCurrentFuelRate, (amount) -> {});

            this.energyGenHandler = new EnergyGenerationHandler(this.energyStorage, () -> {
                Optional<ManaGenFuelRateLoader.FuelRate> rate = getCurrentFuelRate();
                return rate.map(ManaGenFuelRateLoader.FuelRate::getEnergyRate).orElse(DEFAULT_ENERGY_PER_TICK);
            });
            for (Direction dir : Direction.values()) {
                ioMap.put(dir, IOHandlerUtils.IOType.DISABLED); // 或從 NBT、DataComponent 還原
            }

            // 🔧 設置升級處理器到燃料邏輯
            fuelLogic.setUpgradeHandler(upgradeHandler);

        }

        @Override
        public void drops(Level level, BlockPos pos) {
            super.drops(level, pos);
            if (level == null || level.isClientSide) return;

            IItemHandler handler = this.fuelHandler;
            if (handler == null) return;

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                }
            }
        }


        public static int getManaStoredIndex() {return MANA_STORED_INDEX;}
        public static int getEnergyStoredIndex() {return ENERGY_STORED_INDEX;}
        public static int getModeIndex() {return MODE_INDEX;}
        public static int getBurnTimeIndex() {return BURN_TIME_INDEX;}
        public static int getCurrentBurnTimeIndex() {return CURRENT_BURN_TIME_INDEX;}
        public static int getMaxMana() {return MAX_MANA;}
        public static int getMaxEnergy() {return MAX_ENERGY;}
        public ManaGeneratorStateManager getStateManager() {return stateManager;}
        public FuelManaGenHelper getManaGenHandler() {return manaGenHandler;}
        public EnergyGenerationHandler getEnergyGenHandler() {return energyGenHandler;}
        public ManaStorage getManaStorage() {return manaStorage;}
        public ModNeoNalaEnergyStorage getEnergyStorage() {return energyStorage;}
        private final ManaGeneratorNbtManager nbtManager = new ManaGeneratorNbtManager(this);
        public ManaGeneratorSyncHelper getSyncHelper() {return syncHelper;}
        private int clientSyncTimer = 0;
        private static final int CLIENT_SYNC_INTERVAL = 10; // 每10 tick同步一次到客戶端
        // set
        public void setBurnTimeFromNbt(int value) {fuelLogic.setBurnTime(value);}
        public void setCurrentBurnTimeFromNbt(int value) {fuelLogic.setCurrentBurnTime(value);}
        public void forceRefreshAnimationFromNbt() {this.forceRefreshAnimation = true;}


        public EnumMap<Direction, BlockCapabilityCache<IUnifiedManaHandler,  Direction>> getManaOutputCaches() {
            return manaCaches;
        }

        public EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> getEnergyOutputCaches() {
            return energyCaches;
        }


        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
            super.saveAdditional(tag, provider);
            nbtManager.save(tag, provider);
        }


        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel serverLevel) {
                initializeCapabilityCaches(serverLevel);
            }
        }

        private void initializeCapabilityCaches(ServerLevel serverLevel) {
            for (Direction dir : Direction.values()) {
                BlockPos targetPos = worldPosition.relative(dir);
                Direction inputSide = dir.getOpposite();

                manaCaches.put(dir, BlockCapabilityCache.create(
                        ModCapabilities.MANA,
                        serverLevel,
                        targetPos,
                        inputSide,
                        () -> !this.isRemoved(),
                        () -> {}
                ));

                energyCaches.put(dir, BlockCapabilityCache.create(
                        Capabilities.EnergyStorage.BLOCK,
                        serverLevel,
                        targetPos,
                        inputSide,
                        () -> !this.isRemoved(),
                        () -> {}
                ));
            }
        }

        public @Nullable IUnifiedManaHandler getCachedManaCapability(Direction dir) {
            var cache = manaCaches.get(dir);
            return cache != null ? cache.getCapability() : null;
        }

        public @Nullable IEnergyStorage getCachedEnergyCapability(Direction dir) {
            var cache = energyCaches.get(dir);
            return cache != null ? cache.getCapability() : null;
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
            super.loadAdditional(tag, provider);
            nbtManager.load(tag, provider);
        }

        public ManaFuelHandler getFuelLogic() {
            return fuelLogic;
        }

        public ManaGeneratorUpgradeHandler getUpgradeHandler() {
            return upgradeHandler;
        }

        @Override
        public void setLevel(Level level) {
            super.setLevel(level);
            this.access = ContainerLevelAccess.create(level, this.worldPosition);
        }



        public void markUpdated() {
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }

        public Optional<ManaGenFuelRateLoader.FuelRate> getCurrentFuelRate() {
            return fuelLogic.getCurrentFuelRate();
        }


        public IItemHandler getInventory() {
            return fuelHandler;
        }



        @Override
        public void tickMachine() {
            ticker.tick();

            // ✅ 新增：定期同步到客戶端
            if (!level.isClientSide) {
                clientSyncTimer++;
                if (clientSyncTimer >= CLIENT_SYNC_INTERVAL) {
                    clientSyncTimer = 0;

                    // 確保數據最新
                    syncHelper.syncFrom(this);

                    // 如果有變化，同步到客戶端
                    if (syncHelper.hasDirty()) {
                        syncToClient();
                        syncHelper.flushSyncState(this);
                    }
                }
            }
        }

        @Override
        protected boolean canGenerate() {
            return false;
        }

        public void sync() {
            if (!shouldSync()) {
                return;
            }

            isSyncing = true;
            try {
                if (this.level != null && !this.level.isClientSide()) {
                    // 更新同步數據
                    syncHelper.syncFrom(this);

                    // 標記為已更改但不立即同步
                    super.setChanged();

                    // 記錄同步時間
                    lastSyncTime = System.currentTimeMillis();

                    // 清除dirty狀態
                    syncHelper.flushSyncState(this);
                }
            } finally {
                isSyncing = false;
            }
        }

        private boolean shouldSync() {
            if (isSyncing) {
                return false; // 防止遞歸調用
            }

            long currentTime = System.currentTimeMillis();
            return currentTime - lastSyncTime >= MIN_SYNC_INTERVAL; // 節流同步
        }

        public void forceSync() {
            if (isSyncing) {
                return;
            }

            lastSyncTime = 0; // 重置時間以允許立即同步
            sync();
        }

        public ContainerData getContainerData() {
            return syncHelper.getContainerData();
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            this.saveAdditional(tag, registries);
            return tag;
        }


        public void toggleMode() {
            if (stateManager.toggleMode(this.getBurnTime())) {
                this.setChanged();
                this.syncToClient();
                syncHelper.setModeIndex(stateManager.getCurrentModeIndex());
            }
        }


        public void syncToClient() {
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }


        public int getCurrentMode() {
            return stateManager.getCurrentModeIndex();
        }



        @Override
        public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
            IOHandlerUtils.IOType oldType = ioMap.get(direction);
            if (oldType != type) {
                ioMap.put(direction, type);
                setChanged();

                // 🔧 關鍵：通知能力系統刷新
                if (level != null && !level.isClientSide) {
                    level.invalidateCapabilities(worldPosition);

                    // ✅ 【新增】：通知鄰近方塊 (特別是導管) 重新檢查連接
                    notifyNeighborsOfIOChange();
                }
            }
        }

        @Override
        public IOHandlerUtils.IOType getIOConfig(Direction direction) {
            return ioMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
        }

        @Override
        public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
            return new EnumMap<>(ioMap); // 返回副本，避免外部修改
        }

        @Override
        public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> map) {
            boolean changed = false;
            for (Direction dir : Direction.values()) {
                IOHandlerUtils.IOType newType = map.getOrDefault(dir, IOHandlerUtils.IOType.DISABLED);
                if (!ioMap.get(dir).equals(newType)) {
                    ioMap.put(dir, newType);
                    changed = true;
                }
            }

            if (changed) {
                setChanged();

                // 🔧 關鍵：通知能力系統刷新
                if (level != null && !level.isClientSide) {
                    level.invalidateCapabilities(worldPosition);

                    // ✅ 【新增】：通知鄰近方塊重新檢查連接
                    notifyNeighborsOfIOChange();
                }
            }
        }

        // ✅ 【新增方法】：通知所有鄰居IO配置已改變
        private void notifyNeighborsOfIOChange() {
            if (level == null || level.isClientSide) return;

            BlockState currentState = level.getBlockState(worldPosition);

            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(direction);

                // 觸發鄰居方塊的 neighborChanged 方法
                level.neighborChanged(neighborPos, currentState.getBlock(), worldPosition);

                // 如果鄰居是導管，額外調用其特定的鄰居變化處理
                BlockEntity neighborBE = level.getBlockEntity(neighborPos);
                if (neighborBE instanceof ArcaneConduitBlockEntity conduit) {
                    conduit.onNeighborChanged();
                }
            }

            LOGGER.debug("魔力發電機 {} IO設定變更，已通知所有鄰居", worldPosition);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("block." + KoniavacraftMod.MOD_ID + ".mana_generator");
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new ManaGeneratorMenu(id, inv, this);
        }

        public ItemStackHandler getFuelHandler() {
            return fuelHandler;
        }

        @Override
        public UpgradeInventory getUpgradeInventory() {
            return upgradeInventory;
        }

        @Override
        public BlockEntity getBlockEntity() {
            return this;
        }

        public int getBurnTime() {
            return fuelLogic.getBurnTime();
        }

        public int getCurrentBurnTime() {
            return fuelLogic.getCurrentBurnTime();
        }

        public boolean isWorking() {
            return stateManager.isWorking();
        }


        public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockEntityType<T> type) {
            return (lvl, pos, state, blockEntity) -> {
                if (blockEntity instanceof ManaGeneratorBlockEntity entity) {
                    entity.tickMachine();
                }
            };
        }

        public void updateBlockActiveState(boolean isWorking) {
            if (level == null) return;

            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(ManaGeneratorBlock.ACTIVE) && state.getValue(ManaGeneratorBlock.ACTIVE) != isWorking) {
                level.setBlock(worldPosition, state.setValue(ManaGeneratorBlock.ACTIVE, isWorking), 3);
            }
        }

        @Override
        public int getContainerSize() {
            return fuelHandler.getSlots(); // 返回燃料槽位數量
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < fuelHandler.getSlots(); i++) {
                if (!fuelHandler.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return fuelHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return fuelHandler.extractItem(slot, amount, false);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = fuelHandler.getStackInSlot(slot);
            fuelHandler.setStackInSlot(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            fuelHandler.setStackInSlot(slot, stack);
            setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            // 使用 Container 接口提供的靜態方法，更標準
            return Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < fuelHandler.getSlots(); i++) {
                fuelHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
            setChanged();
        }

        // 3. 實現 WorldlyContainer 接口以支持方向性輸入

        @Override
        public int[] getSlotsForFace(Direction side) {
            // 返回該面可以訪問的槽位
            // 假設所有面都可以訪問燃料槽（槽位 0）
            return new int[]{0};
        }

        @Override
        public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, Direction direction) {
            // 檢查是否可以從該方向放入物品
            // 這裡可以加入燃料驗證邏輯
            return canPlaceItem(index, itemStack);
        }

        @Override
        public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
            // 檢查是否可以從該方向取出物品
            // 通常發電機不允許取出燃料，除非是空桶等副產品
            return false; // 根據需求調整
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            // 檢查物品是否可以放入指定槽位
            // 這裡應該檢查是否為有效燃料
            if (index != 0) return false; // 只有槽位 0 是燃料槽

            // 檢查是否為有效燃料
            return fuelLogic.isValidFuel(stack); // 您需要在 ManaFuelHandler 中實現這個方法
        }

        // 4. 重寫 setChanged() 以確保數據同步
        @Override
        public void setChanged() {
            super.setChanged();

            // 節流同步數據
            if (level != null && !level.isClientSide && shouldSync()) {
                sync();
            }
        }

        // Resource cleanup methods
        @Override
        public void setRemoved() {
            super.setRemoved();
            cleanup();
        }

        @Override
        public void clearRemoved() {
            super.clearRemoved();
            // Re-initialize caches when block entity is restored
            if (KoniavacraftMod.IS_DEV) {
                LOGGER.debug("ManaGeneratorBlockEntity at {} restored", worldPosition);
            }
        }

        @Override
        public void onChunkUnloaded() {
            super.onChunkUnloaded();
            cleanup();
        }

        private void cleanup() {
            // Clear capability caches to prevent memory leaks
            manaCaches.clear();
            energyCaches.clear();

            // Reset sync state
            isSyncing = false;

            // Log cleanup for debugging
            if (KoniavacraftMod.IS_DEV) {
                LOGGER.debug("ManaGeneratorBlockEntity at {} cleaned up resources", worldPosition);
            }
        }

    }
