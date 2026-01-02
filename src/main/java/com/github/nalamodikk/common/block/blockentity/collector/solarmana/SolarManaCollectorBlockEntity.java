    package com.github.nalamodikk.common.block.blockentity.collector.solarmana;

    import com.github.nalamodikk.common.block.blockentity.collector.solarmana.manager.SolarUpgradeManager;
    import com.github.nalamodikk.common.block.blockentity.collector.solarmana.sync.SolarCollectorSyncHelper;
    import com.github.nalamodikk.common.block.blockentity.mana_generator.logic.OutputHandler;
    import com.github.nalamodikk.common.block.blockentity.manabase.AbstractManaCollectorBlock;
    import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
    import com.github.nalamodikk.common.capability.ManaStorage;
    import com.github.nalamodikk.common.capability.mana.ManaAction;
    import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
    import com.github.nalamodikk.common.utils.SkyUtils;
    import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
    import com.github.nalamodikk.common.utils.nbt.NbtUtils;
    import com.github.nalamodikk.common.utils.upgrade.UpgradeInventory;
    import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
    import com.github.nalamodikk.common.utils.upgrade.api.IUpgradeableMachine;
    import com.github.nalamodikk.register.ModBlockEntities;
    import com.github.nalamodikk.register.ModCapabilities;
    import com.mojang.logging.LogUtils;
    import net.minecraft.core.BlockPos;
    import net.minecraft.core.Direction;
    import net.minecraft.core.HolderLookup;
    import net.minecraft.core.particles.ParticleTypes;
    import net.minecraft.nbt.CompoundTag;
    import net.minecraft.network.Connection;
    import net.minecraft.network.chat.Component;
    import net.minecraft.network.protocol.Packet;
    import net.minecraft.network.protocol.game.ClientGamePacketListener;
    import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
    import net.minecraft.server.level.ServerLevel;
    import net.minecraft.world.MenuProvider;
    import net.minecraft.world.entity.player.Inventory;
    import net.minecraft.world.entity.player.Player;
    import net.minecraft.world.inventory.AbstractContainerMenu;
    import net.minecraft.world.level.block.entity.BlockEntity;
    import net.minecraft.world.level.block.state.BlockState;
    import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
    import net.neoforged.neoforge.capabilities.Capabilities;
    import net.neoforged.neoforge.energy.IEnergyStorage;
    import org.slf4j.Logger;

    import java.util.EnumMap;

    /**
     * 🌞 太陽能魔力收集器 - 完整修復版
     *
     * 🎯 修復內容：
     * - ✅ 修復重登世界物品消失問題
     * - ✅ 正確的 Menu 創建邏輯
     * - ✅ 改善數據持久化
     * - ✅ 優化同步時機
     *
     * 💡 設計理念：
     * - 🔧 主類保持簡潔，委派給管理器
     * - 📊 數據同步穩定可靠
     * - 💾 數據持久化防護性強
     * - ⚡ 性能優化適度
     */
    public class SolarManaCollectorBlockEntity extends AbstractManaCollectorBlock implements IConfigurableBlock, MenuProvider, IUpgradeableMachine {
        private static final Logger LOGGER = LogUtils.getLogger();
        private static final int MAX_MANA = 80000;

        // === 🔧 核心組件 ===
        private final SolarCollectorSyncHelper syncHelper = new SolarCollectorSyncHelper();
        private final SolarUpgradeManager upgradeManager;

        // === 📊 狀態數據 ===
        private boolean generating = false;
        private final EnumMap<Direction, IOHandlerUtils.IOType> ioMap = new EnumMap<>(Direction.class);

        // === ⚡ 性能緩存 ===
        private final EnumMap<Direction, BlockCapabilityCache<IUnifiedManaHandler, Direction>> manaCaches = new EnumMap<>(Direction.class);
        private final EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> energyCaches = new EnumMap<>(Direction.class);

        public SolarManaCollectorBlockEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), pos, state, 800, 0, 0);
            this.upgradeManager = new SolarUpgradeManager(this);
            ioMap.put(Direction.DOWN, IOHandlerUtils.IOType.OUTPUT);
            // 其他方向預設為 DISABLED
            for (Direction dir : Direction.values()) {
                if (!ioMap.containsKey(dir)) {
                    ioMap.put(dir, IOHandlerUtils.IOType.DISABLED);
                }
            }
            LOGGER.debug("🌞 太陽能收集器初始化：位置 {}", pos);
        }

        // === 📊 公開接口 ===

        public SolarCollectorSyncHelper getSyncHelper() {
            return syncHelper;
        }

        // === ⚡ 核心邏輯 ===


        public boolean isDaytime() {
            if (level == null) return true; // 只有這裡可以有預設值
            // 🔧 統一使用與 canGenerate() 相同的判定邏輯
            return level.isDay();
        }

        // 🏗️ 分離關注點版本 - 符合你的架構偏好

        @Override
        public void tickMachine() {
            // 🔄 狀態管理：委派給專門方法
            updateGeneratingState();

            // 📊 數據同步：每 tick 都執行
            handleDataSync();

            // ⚡ 魔力生成：按間隔執行
            handleManaGeneration();
        }

        // 🔄 狀態更新邏輯
        private void updateGeneratingState() {
            boolean oldGenerating = this.generating;
            boolean canGenerate = canGenerate();
            boolean hasSpace = !manaStorage.isFull();

            // 🎯 真正的生成狀態：能發電 + 有空間
            this.generating = canGenerate && hasSpace;

            // 🔧 狀態變化時通知客戶端
            if (oldGenerating != this.generating && level instanceof ServerLevel) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                setChanged();

                LOGGER.debug("🔄 狀態變化: {} -> {}, 條件: 可發電={}, 有空間={}",
                        oldGenerating, this.generating, canGenerate, hasSpace);
            }
        }

        // 📊 數據同步邏輯
        private void handleDataSync() {
            // 🔧 修復：只在伺服器端且有打開的 Menu 時才同步
            if (level instanceof ServerLevel && syncHelper.getContainerData() != null) {
                syncHelper.syncFrom(this);
            }
        }

        // ⚡ 魔力生成邏輯
        private void handleManaGeneration() {
            // 檢查生成間隔
            int interval = upgradeManager.getUpgradedInterval();
            if (level.getGameTime() % interval != 0) return;

            // 只有在真正發電時才執行生成
            if (!this.generating) return;

            // 執行實際生成
            performManaGeneration();
        }

        // 🎯 執行實際的魔力生成
        private void performManaGeneration() {
            int amount = upgradeManager.getUpgradedOutput();
            int inserted = manaStorage.insertMana(amount, ManaAction.EXECUTE);

            if (inserted > 0 && level instanceof ServerLevel server) {
                // 🔌 處理輸出
                handleManaOutput(server);

                // 🎨 視覺效果
                createGenerationEffects(server);
            }
        }

        // 🔌 魔力輸出處理
        private void handleManaOutput(ServerLevel server) {
            boolean didOutput = OutputHandler.tryOutput(server, worldPosition, manaStorage, null, ioMap, manaCaches, energyCaches);

            // 診斷邏輯
            if (!didOutput && !hasLoggedOutputFailure) {
                hasLoggedOutputFailure = true;
                LOGGER.warn("⚠️ 輸出失敗: 位置={}, 魔力={}/{}", worldPosition, manaStorage.getManaStored(), manaStorage.getMaxManaStored());
            }

            if (didOutput) {
                if (hasLoggedOutputFailure) {
                    hasLoggedOutputFailure = false;
                    LOGGER.info("✅ 輸出恢復: {}", worldPosition);
                }

                // 魔力值變化，通知更新
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                setChanged();
            }
        }

        // 🎨 生成視覺效果
        private void createGenerationEffects(ServerLevel server) {
            server.sendParticles(
                    ParticleTypes.ENCHANT,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.1,
                    worldPosition.getZ() + 0.5,
                    2, 0.2, 0.1, 0.2, 0.0
            );
        }
        // 🆕 添加輸出失敗標記
        private boolean hasLoggedOutputFailure = false;


        //是否可以發電方法 - 使用高度圖的極速穩定方案
        @Override
        protected boolean canGenerate() {
            if (!(level instanceof ServerLevel server)) return false;

            final BlockPos skyPos = worldPosition;
            return server.isDay()
                && server.dimensionType().hasSkyLight()
                && SkyUtils.isOpenToSkyByHeightmap(server, skyPos) // 🚀 使用高度圖，極速穩定
                && !server.isRainingAt(skyPos.above())
                && !server.isThundering();
        }






        // === 💾 數據持久化 ===

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);

            try {
                // 🔧 委派給組件保存 - 傳入正確的 registries
                NbtUtils.write(tag, "Mana", manaStorage, registries);
                upgradeManager.saveToNBT(tag, registries); // ✅ 傳入 registries
                NbtUtils.writeEnumIOTypeMap(tag, "IOMap", ioMap);

                // 📊 保存狀態
                tag.putBoolean("Generating", generating);

                // 🔍 調試日誌
//                LOGGER.debug("🌞 保存太陽能收集器: 位置 {}, 魔力 {}, 升級管理器已保存",
//                        worldPosition, manaStorage.getManaStored());

            } catch (Exception e) {
                LOGGER.error("💥 保存太陽能收集器失敗: {}", worldPosition, e);
            }
        }


        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);

            try {
                // 🔧 委派給組件加載 - 傳入正確的 registries
                NbtUtils.read(tag, "Mana", manaStorage, registries);
                upgradeManager.loadFromNBT(tag, registries); // ✅ 使用修復後的方法
                setIOMap(NbtUtils.readEnumIOTypeMap(tag, "IOMap"));

                // 📊 加載狀態
                generating = tag.getBoolean("Generating");

                // 🆕 關鍵修復：載入完成後立即同步數據
                syncHelper.syncFrom(this);

                LOGGER.debug("🌞 載入太陽能收集器: 位置 {}, 魔力 {}, 升級管理器已載入",
                        worldPosition, manaStorage.getManaStored());

            } catch (Exception e) {
                LOGGER.error("💥 載入太陽能收集器失敗: {}", worldPosition, e);
                generating = false;
            }
        }

        private void ensureUpgradeDataSync() {
            if (level == null || level.isClientSide()) return;

            // 檢查實際升級數量
            int actualSpeed = upgradeManager.getUpgradeInventory().getUpgradeCount(UpgradeType.SPEED);
            int actualEff = upgradeManager.getUpgradeInventory().getUpgradeCount(UpgradeType.EFFICIENCY);

            // 檢查同步數據
            int syncSpeed = syncHelper.getSpeedLevel();
            int syncEff = syncHelper.getEfficiencyLevel();

            // 如果不一致，強制同步
            if (actualSpeed != syncSpeed || actualEff != syncEff) {
                LOGGER.debug("🔄 檢測到升級數據不一致，強制同步: 實際速度={}, 同步速度={}, 實際效率={}, 同步效率={}",
                        actualSpeed, syncSpeed, actualEff, syncEff);

                syncHelper.syncFrom(this);
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                setChanged();
            }
        }


        // === 🎮 GUI 接口 ===

        @Override
        public Component getDisplayName() {
            return Component.translatable("block.koniava.solar_mana_collector");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
            // 🎯 創建 Menu 前確保數據已同步
            syncHelper.syncFrom(this);

            if (!level.isClientSide()) {
                // 伺服器端再次確保同步
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }

            return new SolarManaCollectorMenu(id, playerInventory, this);
        }

        // === 🔌 能力系統 ===

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel serverLevel) {
                initializeCapabilityCaches(serverLevel);


                // 🆕 伺服器端載入後立即同步一次數據
                syncHelper.syncFrom(this);

                // 🆕 通知客戶端更新
                serverLevel.scheduleTick(worldPosition, getBlockState().getBlock(), 1);

                LOGGER.debug("🌞 伺服器端載入完成: 位置={}, 已排程同步", worldPosition);
            } else if (level != null && level.isClientSide()) {
                // 🆕 客戶端載入時的日誌
                LOGGER.debug("🌞 客戶端載入完成: 位置={}", worldPosition);
            }
        }

        private void initializeCapabilityCaches(ServerLevel serverLevel) {
            for (Direction dir : Direction.values()) {
                BlockPos targetPos = worldPosition.relative(dir);
                Direction inputSide = dir.getOpposite();

                LOGGER.debug("🔧 初始化快取: 方向={}, 目標位置={}, 輸入側={}",
                        dir, targetPos, inputSide);

                manaCaches.put(dir, BlockCapabilityCache.create(
                        ModCapabilities.MANA,
                        serverLevel,
                        targetPos,
                        inputSide,
                        () -> !this.isRemoved(),
                        () -> {
                            LOGGER.debug("🔄 魔力快取失效: 方向={}", dir);
                        }
                ));

                energyCaches.put(dir, BlockCapabilityCache.create(
                        Capabilities.EnergyStorage.BLOCK,
                        serverLevel,
                        targetPos,
                        inputSide,
                        () -> !this.isRemoved(),
                        () -> {
                            LOGGER.debug("🔄 能量快取失效: 方向={}", dir);
                        }
                ));
            }

            LOGGER.info("✅ 太陽能收集器快取初始化完成: 位置={}", worldPosition);
        }
        // === 🔧 升級系統接口 ===

        @Override
        public UpgradeInventory getUpgradeInventory() {
            return upgradeManager.getUpgradeInventory();
        }

        @Override
        public BlockEntity getBlockEntity() {
            return this;
        }

        // === 📊 數據接口 ===

        public ManaStorage getManaStorage() {
            return manaStorage;
        }

        public int getManaStored() {
            return this.manaStorage.getManaStored();
        }

        public boolean isCurrentlyGenerating() {
            return this.generating;
        }

        public void setCurrentlyGenerating(boolean value) {
            this.generating = value;
        }

        public static int getMaxMana() {
            return MAX_MANA;
        }

        // === 🔧 配置接口 ===


        @Override
        public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
            ioMap.put(direction, type);
            setChanged(); // 🔄 配置變更時保存
        }

        @Override
        public IOHandlerUtils.IOType getIOConfig(Direction direction) {
            return ioMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
        }

        @Override
        public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
            return ioMap;
        }

        @Override
        public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> map) {
            ioMap.clear();
            ioMap.putAll(map);
            setChanged(); // 🔄 配置變更時保存
        }

        /**
         * 🌐 獲取同步標籤 - 發送給客戶端的完整數據
         * 當客戶端載入區塊時會調用此方法
         */
        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            CompoundTag tag = super.getUpdateTag(registries);

            // 🔧 包含所有需要同步的數據
            saveAdditional(tag, registries);

            LOGGER.debug("🌐 伺服器準備同步標籤: 位置={}, 升級數據已包含", worldPosition);
            return tag;
        }


        /**
         * 🌐 處理收到的同步標籤 - 客戶端接收數據
         * 客戶端收到同步數據時會調用此方法
         */
        @Override
        public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
            // 🔧 載入所有同步的數據
            loadAdditional(tag, registries);

            // 🆕 載入完成後立即同步到GUI
            syncHelper.syncFrom(this);

            LOGGER.debug("🌐 客戶端收到同步標籤: 位置={}, 升級數據已更新", worldPosition);
        }

        /**
         * 🌐 獲取更新封包 - 用於區塊更新時同步
         * 當調用 level.sendBlockUpdated 時會使用此方法
         */
        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        /**
         * 🌐 處理數據封包 - 客戶端處理區塊更新
         * 客戶端收到區塊更新封包時會調用此方法
         */
        @Override
        public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
            CompoundTag tag = pkt.getTag();
            if (tag != null) {
                handleUpdateTag(tag, registries);
                LOGGER.debug("🌐 客戶端處理數據封包: 位置={}", worldPosition);
            }
        }

    }
