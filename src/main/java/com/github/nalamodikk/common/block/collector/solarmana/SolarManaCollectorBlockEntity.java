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
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
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

public class SolarManaCollectorBlockEntity extends AbstractManaCollectorBlock implements IConfigurableBlock, MenuProvider , IUpgradeableMachine {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_MANA = 80000;
    private final SolarCollectorSyncHelper syncHelper = new SolarCollectorSyncHelper();
    private boolean generating = false;
    private final SolarUpgradeManager upgradeManager;
    private final EnumMap<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, BlockCapabilityCache<IUnifiedManaHandler, Direction>> manaCaches = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> energyCaches = new EnumMap<>(Direction.class);


    public SolarManaCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), pos, state, 800, 40, 5);
        this.upgradeManager = new SolarUpgradeManager(this);

    }


    public SolarCollectorSyncHelper getSyncHelper() {
        return syncHelper;
    }

    @Override
    public void tickMachine() {
        this.generating = canGenerate();
        syncHelper.syncFrom(this);

        // 🚀 直接使用升級管理器，不需要再計算 speedLevel 和 efficiencyLevel
        int interval = upgradeManager.getUpgradedInterval();
        if (level.getGameTime() % interval != 0) return;

        if (!canGenerate()) return;

        int amount = upgradeManager.getUpgradedOutput();

        int inserted = manaStorage.insertMana(amount, ManaAction.EXECUTE);
        if (inserted > 0 && level instanceof ServerLevel server) {
            OutputHandler.tryOutput(server, worldPosition, manaStorage, null, ioMap, manaCaches, energyCaches);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();

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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        NbtUtils.write(tag, "Mana", manaStorage, registries);
        upgradeManager.saveToNBT(tag); // 委派給管理器
        NbtUtils.writeEnumBooleanMap(tag, "DirectionConfig", directionConfig);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        NbtUtils.read(tag, "Mana", manaStorage, registries);
        upgradeManager.loadFromNBT(tag); // 委派給管理器
        NbtUtils.readEnumBooleanMap(tag, "DirectionConfig", directionConfig);
    }



    @Override
    public Component getDisplayName() {
        return Component.translatable("block.koniava.solar_mana_collector");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(this.getBlockPos()); // ✅ 你 client 端的 menu 需要這個
        return new SolarManaCollectorMenu(id, playerInventory, buf);
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

    @Override
    public UpgradeInventory getUpgradeInventory() {
        return upgradeManager.getUpgradeInventory();
    }
    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }


    public ManaStorage getManaStorage() {
        return manaStorage;
    }


    private final EnumMap<Direction, IOHandlerUtils.IOType> ioMap = new EnumMap<>(Direction.class);

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        ioMap.put(direction, type);
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
    }

    @Override
    public boolean isOutput(Direction direction) {
        return directionConfig.getOrDefault(direction, false);
    }

    public int getManaStored() {
        return this.manaStorage.getManaStored(); // 假設你有 manaStorage 欄位
    }


    public boolean isCurrentlyGenerating() {
        return this.generating; // 假設你有一個 boolean 欄位叫 generating
    }

    public void setCurrentlyGenerating(boolean value) {
        this.generating = value;
    }

    public static int getMaxMana() {return MAX_MANA;}

}