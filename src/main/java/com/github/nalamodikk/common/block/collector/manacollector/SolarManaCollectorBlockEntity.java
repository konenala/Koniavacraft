package com.github.nalamodikk.common.block.collector.manacollector;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.collector.manacollector.sync.SolarCollectorSyncHelper;
import com.github.nalamodikk.common.block.mana_generator.logic.OutputHandler;
import com.github.nalamodikk.common.block.manabase.AbstractManaCollectorBlock;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.register.ModBlockEntities;
import com.github.nalamodikk.common.utils.block.DirectionIOController;
import com.github.nalamodikk.common.utils.nbt.NbtUtils;
import com.github.nalamodikk.common.utils.upgrade.UpgradeInventory;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;

import org.slf4j.Logger;

import java.util.EnumMap;

public class SolarManaCollectorBlockEntity extends AbstractManaCollectorBlock implements IConfigurableBlock, MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_MANA = 80000;
    private final SolarCollectorSyncHelper syncHelper = new SolarCollectorSyncHelper();
    private boolean generating = false;

    private final UpgradeInventory upgrades = new UpgradeInventory(4);
    private final EnumMap<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);
    private static final int BASE_OUTPUT = 5;


    public SolarManaCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), pos, state, 800, 40, 5);
    }


    public SolarCollectorSyncHelper getSyncHelper() {
        return syncHelper;
    }

    @Override
    public void tickServer() {
        syncHelper.getRawSyncManager().set(SolarCollectorSyncHelper.SyncIndex.GENERATING.ordinal(),canGenerate() ? 1 : 0);
        syncHelper.syncFrom(this);

        int interval = Math.max(10, 200 - upgrades.getUpgradeCount(UpgradeType.SPEED) * 20);
        if (level.getGameTime() % interval != 0) return;

        if (!canGenerate()) return;

        int efficiencyLevel = upgrades.getUpgradeCount(UpgradeType.EFFICIENCY);
        int amount = (int)(BASE_OUTPUT * (1 + efficiencyLevel * 0.1));

        int inserted = manaStorage.insertMana(amount, ManaAction.EXECUTE);
        if (inserted > 0) {
            OutputHandler.tryOutput((ServerLevel) level, worldPosition, manaStorage, null, directionConfig);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();

            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.0);
            }
        }
    }

    protected boolean canGenerate() {
        return level.isDay() && !level.isRaining() && level.canSeeSky(worldPosition.above());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        NbtUtils.write(tag, "Mana", manaStorage, registries);
        NbtUtils.write(tag, "Upgrades", upgrades, registries);
        NbtUtils.writeEnumBooleanMap(tag, "DirectionConfig", directionConfig);    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {

        NbtUtils.read(tag, "Mana", manaStorage, registries);
        NbtUtils.read(tag, "Upgrades", upgrades, registries);
        NbtUtils.readEnumBooleanMap(tag, "DirectionConfig", directionConfig);    }



    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magical_industry.solar_mana_collector");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(this.getBlockPos()); // ✅ 你 client 端的 menu 需要這個
        return new SolarManaCollectorMenu(id, playerInventory, buf);
    }




    public UpgradeInventory getUpgradeInventory() {
        return upgrades;
    }

    public EnumMap<Direction, Boolean> getDirectionConfig() {
        return directionConfig;
    }

    public ManaStorage getManaStorage() {
        return manaStorage;
    }


    @Override
    public void setDirectionConfig(Direction direction, boolean isOutput) {
        directionConfig.put(direction, isOutput);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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