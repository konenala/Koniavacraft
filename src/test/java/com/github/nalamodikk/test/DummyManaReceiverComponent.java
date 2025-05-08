package com.github.nalamodikk.test;

import com.github.nalamodikk.common.API.machine.IGridComponent;
import com.github.nalamodikk.common.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.capability.IHasMana;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class DummyManaReceiverComponent implements IGridComponent, IHasMana {
    private final IUnifiedManaHandler mana = new DummyManaStorage();

    @Override public IUnifiedManaHandler getManaStorage() { return mana; }
    @Override public ResourceLocation getId() { return new ResourceLocation("magical_industry", "dummy_storage"); }
    @Override public void saveToNBT(CompoundTag tag) {}
    @Override public void loadFromNBT(CompoundTag tag) {}
    @Override public CompoundTag getData() { return new CompoundTag(); }
    @Override public void onAdded(ComponentGrid grid, BlockPos pos) {}
    @Override public void onRemoved(ComponentGrid grid, BlockPos pos) {}
}
