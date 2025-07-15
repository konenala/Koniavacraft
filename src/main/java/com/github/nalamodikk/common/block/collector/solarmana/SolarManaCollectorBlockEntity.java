package com.github.nalamodikk.common.block.collector.solarmana;

import com.github.nalamodikk.common.block.collector.solarmana.manager.SolarUpgradeManager;
import com.github.nalamodikk.common.block.collector.solarmana.sync.SolarCollectorSyncHelper;
import com.github.nalamodikk.common.block.mana_generator.logic.OutputHandler;
import com.github.nalamodikk.common.block.manabase.AbstractManaCollectorBlock;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.common.utils.nbt.NbtUtils;
import com.github.nalamodikk.common.utils.upgrade.UpgradeInventory;
import com.github.nalamodikk.common.utils.upgrade.api.IUpgradeableMachine;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModCapabilities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
        super(ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), pos, state, 800, 40, 5);
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

    @Override
    public void tickMachine() {
        // 🔄 狀態更新
        this.generating = canGenerate();

        // 📊 同步數據（每 tick 更新，確保客戶端數據實時）
        syncHelper.syncFrom(this);

        // ⚡ 能量生成邏輯
        int interval = upgradeManager.getUpgradedInterval();
        if (level.getGameTime() % interval != 0) return;

        if (!canGenerate()) return;

        int amount = upgradeManager.getUpgradedOutput();
        int inserted = manaStorage.insertMana(amount, ManaAction.EXECUTE);

        if (inserted > 0 && level instanceof ServerLevel server) {
            // 🔌 輸出處理
            OutputHandler.tryOutput(server, worldPosition, manaStorage, null, ioMap, manaCaches, energyCaches);

            // 📡 通知更新
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();

            // 🎨 粒子效果
            server.sendParticles(
                    ParticleTypes.ENCHANT,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.1,
                    worldPosition.getZ() + 0.5,
                    2, 0.2, 0.1, 0.2, 0.0
            );
        }
    }

    protected boolean canGenerate() {
        return level.isDay() && !level.isRaining() && level.canSeeSky(worldPosition.above());
    }

    // === 💾 數據持久化 ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 🔧 委派給組件保存
        NbtUtils.write(tag, "Mana", manaStorage, registries);
        upgradeManager.saveToNBT(tag);
        NbtUtils.writeEnumIOTypeMap(tag, "IOMap", ioMap);

        // 📊 保存狀態
        tag.putBoolean("Generating", generating);

    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // 🔧 委派給組件加載
        NbtUtils.read(tag, "Mana", manaStorage, registries);
        upgradeManager.loadFromNBT(tag);
        setIOMap(NbtUtils.readEnumIOTypeMap(tag, "IOMap"));
        // 📊 加載狀態
        generating = tag.getBoolean("Generating");

        LOGGER.debug("🌞 加載太陽能收集器數據：位置 {}, 升級槽位 {}",
                worldPosition, upgradeManager.getUpgradeInventory().getContainerSize());
    }

    // === 🎮 GUI 接口 ===

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.koniava.solar_mana_collector");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        // 🎯 關鍵修復：伺服器端直接使用 BlockEntity 構造函數
        return new SolarManaCollectorMenu(id, playerInventory, this);
    }

    // === 🔌 能力系統 ===

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


}