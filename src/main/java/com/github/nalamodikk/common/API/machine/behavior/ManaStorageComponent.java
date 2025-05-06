package com.github.nalamodikk.common.API.machine.behavior;

import com.github.nalamodikk.common.API.IGridComponent;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.capability.IHasMana;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.API.machine.grid.ComponentGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class ManaStorageComponent implements IGridComponent, IHasMana {
    private final ManaStorage storage = new ManaStorage(1000); // 預設容量：1000 mana

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana_storage");
    }

    @Override
    public void saveToNBT(CompoundTag tag) {
        tag.put("mana", storage.serializeNBT());
    }

    @Override
    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("mana")) {
            storage.deserializeNBT(tag.getCompound("mana"));
        }
    }

    @Override
    public CompoundTag getData() {
        return null;
    }

    @Override
    public IUnifiedManaHandler getManaStorage() {
        return storage;
    }

    @Override
    public void onAdded(ComponentGrid grid, BlockPos pos) {}

    @Override
    public void onRemoved(ComponentGrid grid, BlockPos pos) {}
}
