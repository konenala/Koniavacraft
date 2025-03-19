package com.github.nalamodikk.common.block.entity;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.ModCapabilities;
import com.github.nalamodikk.common.compat.energy.ManaEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

public abstract class AbstractManaMachine extends BlockEntity
        implements GeoBlockEntity, GeoAnimatable, MenuProvider, IConfigurableBlock {

    protected final ManaEnergyStorage energyStorage;
    protected final ManaStorage manaStorage;
    protected final ItemStackHandler itemHandler;
    private final LazyOptional<ManaEnergyStorage> lazyEnergyStorage;
    private final LazyOptional<ManaStorage> lazyManaStorage;
    private final LazyOptional<ItemStackHandler> lazyItemHandler;

    protected boolean isWorking = false;

    public AbstractManaMachine(BlockEntityType<?> type, BlockPos pos, BlockState state,
                               @Nullable Integer maxMana, @Nullable Integer maxEnergy, int itemSlots) {
        super(type, pos, state);

        // 只有當 maxEnergy 不為 null 且大於 0 時，才初始化能量存儲
        this.energyStorage = (maxEnergy != null && maxEnergy > 0) ? new ManaEnergyStorage(maxEnergy) : null;

        // 只有當 maxMana 不為 null 且大於 0 時，才初始化魔力存儲
        this.manaStorage = (maxMana != null && maxMana > 0) ? new ManaStorage(maxMana) : null;

        this.itemHandler = new ItemStackHandler(itemSlots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };

        // 只有當 energyStorage 存在時才創建 LazyOptional
        this.lazyEnergyStorage = (this.energyStorage != null) ? LazyOptional.of(() -> energyStorage) : LazyOptional.empty();
        this.lazyManaStorage = (this.manaStorage != null) ? LazyOptional.of(() -> manaStorage) : LazyOptional.empty();
        this.lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }


    public abstract void tickMachine();

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractManaMachine machine) {
        if (!level.isClientSide) {
            machine.tickMachine();
        }
    }

    public void consumeMana(int amount) {
        if (manaStorage.getMana() >= amount) {
            manaStorage.consumeMana(amount);
            setChanged();
        }
    }

    public void generateMana(int amount) {
        if (manaStorage.getMana() + amount <= manaStorage.getMaxMana()) {
            manaStorage.addMana(amount);
            setChanged();
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);

        if (manaStorage != null) {
            manaStorage.setMana(tag.getInt("ManaStored"));
        }

        if (energyStorage != null) {
            energyStorage.receiveEnergy(tag.getInt("EnergyStored"), false);
        }

        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        isWorking = tag.getBoolean("IsWorking");
    }


    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);

        if (manaStorage != null) {
            tag.putInt("ManaStored", manaStorage.getMana());
        }

        if (energyStorage != null) {
            tag.putInt("EnergyStored", energyStorage.getEnergyStored());
        }

        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putBoolean("IsWorking", isWorking);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && energyStorage != null) {
            return lazyEnergyStorage.cast();
        } else if (cap == ModCapabilities.MANA && manaStorage != null) {
            return lazyManaStorage.cast();
        }
        return super.getCapability(cap, side);
    }


    public ItemStackHandler getInventory() {
        return itemHandler;
    }

    public int getManaStored() {
        return manaStorage.getMana();
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public boolean isWorking() {
        return isWorking;
    }

    @Nullable
    @Override
    public abstract AbstractContainerMenu createMenu(int id, Inventory inv, Player player);}
