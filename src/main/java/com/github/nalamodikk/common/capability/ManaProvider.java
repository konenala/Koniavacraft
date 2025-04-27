package com.github.nalamodikk.common.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

public class ManaProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final IUnifiedManaHandler manaStorage;
    private final LazyOptional<IUnifiedManaHandler> lazyOptional;

    public ManaProvider(IUnifiedManaHandler manaStorage) {
        this.manaStorage = manaStorage;
        this.lazyOptional = LazyOptional.of(() -> this.manaStorage);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return cap == ManaCapability.MANA ? lazyOptional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        if (manaStorage instanceof INBTSerializable<?>) {
            @SuppressWarnings("unchecked")
            INBTSerializable<CompoundTag> serializable = (INBTSerializable<CompoundTag>) manaStorage;
            return serializable.serializeNBT();
        }
        return new CompoundTag(); // 預防萬一
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (manaStorage instanceof INBTSerializable<?>) {
            @SuppressWarnings("unchecked")
            INBTSerializable<CompoundTag> serializable = (INBTSerializable<CompoundTag>) manaStorage;
            serializable.deserializeNBT(nbt);
        }
    }
}
