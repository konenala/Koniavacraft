package com.github.nalamodikk.common.block.entity;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.compat.energy.ManaEnergyStorage;
import com.github.nalamodikk.common.register.ModBlockEntities;
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

    public AbstractManaMachine(BlockEntityType<?> type, BlockPos pos, BlockState state, int maxMana, int maxEnergy, int itemSlots) {
        super(type, pos, state);

        this.energyStorage = new ManaEnergyStorage(maxEnergy);
        this.manaStorage = new ManaStorage(maxMana);
        this.itemHandler = new ItemStackHandler(itemSlots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };

        this.lazyEnergyStorage = LazyOptional.of(() -> energyStorage);
        this.lazyManaStorage = LazyOptional.of(() -> manaStorage);
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
        manaStorage.setMana(tag.getInt("ManaStored"));
        energyStorage.receiveEnergy(tag.getInt("EnergyStored"), false);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        isWorking = tag.getBoolean("IsWorking");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ManaStored", manaStorage.getMana());
        tag.putInt("EnergyStored", energyStorage.getEnergyStored());
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putBoolean("IsWorking", isWorking);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergyStorage.cast();
        if (cap == ModCapabilities.MANA) return lazyManaStorage.cast();
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
