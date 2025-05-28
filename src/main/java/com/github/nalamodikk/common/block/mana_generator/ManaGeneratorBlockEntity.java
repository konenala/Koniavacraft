// ⚠ 自動產生：結合 NeoForge 能量與魔力產出邏輯
package com.github.nalamodikk.common.block.mana_generator;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.manabase.AbstractManaMachineEntityBlock;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaCapability;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.recipe.mana_fuel.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.register.ModBlockEntities;
import com.github.nalamodikk.common.register.ModCapability;
import com.github.nalamodikk.common.sync.UnifiedSyncManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ManaGeneratorBlockEntity extends AbstractManaMachineEntityBlock implements IConfigurableBlock {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManaGeneratorBlockEntity.class);

    public enum Mode {
        MANA,
        ENERGY
    }

    private static final int MAX_MANA = 200000;
    private static final int MAX_ENERGY = 200000;
    private static final int TICK_INTERVAL = 1;
    private static final int MANA_PER_CYCLE = 10;
    private static final int SYNC_DATA_COUNT = 5;
    private static final int FUEL_SLOT_COUNT = 1;
    private static final int MANA_STORED_INDEX = 0;
    private static final int ENERGY_STORED_INDEX = 1;
    private static final int MODE_INDEX = 2;
    private static final int BURN_TIME_INDEX = 3;
    private static final int CURRENT_BURN_TIME_INDEX = 4;


    private final ItemStackHandler fuelHandler = new ItemStackHandler(FUEL_SLOT_COUNT);
    private final EnumMap<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);
    private ContainerLevelAccess access;
    private final UnifiedSyncManager syncManager = new UnifiedSyncManager(SYNC_DATA_COUNT);
    private int burnTime = 0;
    private int currentBurnTime = 0;
    private boolean isWorking = false;
    private double manaAccumulated = 0;
    private ResourceLocation currentFuelId = null;
    private int failedFuelCooldown = 0;
    private Mode currentMode = Mode.MANA;

    public ManaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_GENERATOR_BE.get(), pos, state, true, MAX_MANA, MAX_ENERGY, TICK_INTERVAL, MANA_PER_CYCLE);
        if (this.level != null) {
            this.access = ContainerLevelAccess.create(this.level, this.worldPosition);
        }
    }

    public static int getManaStoredIndex() {return MANA_STORED_INDEX;}
    public static int getEnergyStoredIndex() {return ENERGY_STORED_INDEX;}
    public static int getModeIndex() {return MODE_INDEX;}
    public static int getBurnTimeIndex() {return BURN_TIME_INDEX;}
    public static int getCurrentBurnTimeIndex() {return CURRENT_BURN_TIME_INDEX;}
    public static int getDataCount() {return SYNC_DATA_COUNT;}
    public static int getMaxMana() {return MAX_MANA;}
    public static int getMaxEnergy() {return MAX_ENERGY;}



    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        this.access = ContainerLevelAccess.create(level, this.worldPosition);
    }

    public static Map<Item, Integer> getAllFuelItems(Level level) {
        Map<Item, Integer> fuelMap = new HashMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            int burnTime = net.minecraft.world.level.block.entity.FurnaceBlockEntity.getFuel().getOrDefault(item, 0);

            if (burnTime > 0) {
                fuelMap.put(item, burnTime);
            }
        }

        return fuelMap;
    }




    public void markUpdated() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public IItemHandler getInventory() {
        return fuelHandler;
    }


    @Override
    public void tickMachine() {
        if (failedFuelCooldown > 0) {
            failedFuelCooldown--;
            return;
        }

        if (burnTime <= 0) {
            if (!handleNewFuel()) {
                isWorking = false;
                failedFuelCooldown = 20;
                return;
            }
        }

        burnTime--;
        if (getCurrentMode() == 0) {
            handleManaGeneration();
        } else {
            handleEnergyGeneration();
        }

        outputEnergyAndMana();
        this.sync();

    }

    @Override
    protected boolean canGenerate() {
        return false;
    }

    private void handleManaGeneration() {
        if (manaStorage == null || manaStorage.getManaStored() >= manaStorage.getMaxManaStored()) {
            isWorking = false;
            return;
        }

        if (currentFuelId == null) {
            isWorking = false;
            return;
        }

        ManaGenFuelRateLoader.FuelRate rate = ManaGenFuelRateLoader.getFuelRateForItem(currentFuelId);
        if (rate == null) {
            isWorking = false;
            return;
        }

        manaAccumulated += rate.getManaRate();
        while (manaAccumulated >= 1.0) {
            int toStore = (int) manaAccumulated;
            manaStorage.addMana(toStore);
            manaAccumulated -= toStore;
        }

        isWorking = true;
    }

    private void handleEnergyGeneration() {
        if (energyStorage == null || energyStorage.getEnergyStored() >= energyStorage.getMaxEnergyStored()) {
            isWorking = false;
            return;
        }

        double energyToGenerate = 40;
        int accepted = energyStorage.receiveEnergy((int) energyToGenerate, false);
        isWorking = accepted > 0;
    }

    private boolean handleNewFuel() {
        ItemStack fuel = fuelHandler.getStackInSlot(0);
        if (fuel.isEmpty()) return false;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(fuel.getItem());
        currentFuelId = id;

        ManaGenFuelRateLoader.FuelRate rate = ManaGenFuelRateLoader.getFuelRateForItem(id);
        if (rate == null || rate.getBurnTime() <= 0 || rate.getManaRate() <= 0) return false;

        currentBurnTime = rate.getBurnTime();
        burnTime = currentBurnTime;
        fuelHandler.extractItem(0, 1, false);
        return true;
    }

    private void outputEnergyAndMana() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        for (Direction dir : Direction.values()) {
            if (!isOutput(dir)) continue;

            BlockPos targetPos = worldPosition.relative(dir);

            IEnergyStorage energyTarget = BlockCapabilityCache.create(
                    Capabilities.EnergyStorage.BLOCK, serverLevel, targetPos, dir.getOpposite()
            ).getCapability();

            if (energyTarget != null && energyStorage != null && energyTarget.canReceive()) {
                int toSend = Math.min(energyStorage.getEnergyStored(), 40);
                int accepted = energyTarget.receiveEnergy(toSend, false);
                energyStorage.extractEnergy(accepted, false);
            }

            IUnifiedManaHandler manaTarget = BlockCapabilityCache.create(
                    ModCapability.MANA, serverLevel, targetPos, dir.getOpposite()
            ).getCapability();

            if (manaTarget != null && manaStorage != null && manaTarget.canReceive()) {
                int toSend = Math.min(manaStorage.getManaStored(), 40);
                int accepted = manaTarget.receiveMana(toSend, ManaAction.get(false));
                manaStorage.extractMana(accepted, ManaAction.get(false));
            }
        }
    }


    private void sync() {
        syncManager.set(0, manaStorage != null ? manaStorage.getManaStored() : 0);
        syncManager.set(1, energyStorage != null ? energyStorage.getEnergyStored() : 0);
        syncManager.set(2, currentMode == Mode.MANA ? 0 : 1);
        syncManager.set(3, burnTime);
        syncManager.set(4, currentBurnTime);
    }

    public ContainerData getContainerData() {
        return syncManager.getContainerData();
    }

    public void toggleMode() {
        if (isWorking || burnTime > 0) {
            MagicalIndustryMod.LOGGER.info("⚠ 無法切換模式，發電機正在運行中！");
            return;
        }
        currentMode = (currentMode == Mode.MANA) ? Mode.ENERGY : Mode.MANA;
    }

    public int getCurrentMode() {
        return currentMode == Mode.MANA ? 0 : 1;
    }

    @Override
    public boolean isOutput(Direction direction) {
        return directionConfig.getOrDefault(direction, false);
    }

    @Override
    public void setDirectionConfig(Direction direction, boolean isOutput) {
        directionConfig.put(direction, isOutput);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block." + MagicalIndustryMod.MOD_ID + ".mana_generator");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return  new ManaGeneratorMenu(id, inv, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));

    }

    public ItemStackHandler getFuelHandler() {
        return fuelHandler;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getCurrentBurnTime() {
        return currentBurnTime;
    }

    public boolean isWorking() {
        return isWorking;
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockEntityType<T> type) {
        return (lvl, pos, state, blockEntity) -> {
            if (blockEntity instanceof ManaGeneratorBlockEntity entity) {
                entity.tickMachine();
            }
        };
    }
}
