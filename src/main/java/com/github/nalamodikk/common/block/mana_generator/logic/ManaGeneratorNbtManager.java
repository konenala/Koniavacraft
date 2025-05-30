package com.github.nalamodikk.common.block.mana_generator.logic;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.utils.nbt.NbtUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class ManaGeneratorNbtManager {
    private final ManaGeneratorBlockEntity entity;

    public ManaGeneratorNbtManager(ManaGeneratorBlockEntity entity) {
        this.entity = entity;
    }

    public void save(CompoundTag tag, HolderLookup.Provider provider) {
//        MagicalIndustryMod.LOGGER.info("[Client] saveAdditional - isWorking = {}", entity.isWorking());

        tag.putInt("Mode", entity.getStateManager().getCurrentModeIndex());
        tag.putInt("BurnTime", entity.getBurnTime());
        tag.putInt("CurrentBurnTime", entity.getCurrentBurnTime());
        tag.putBoolean("IsWorking", entity.isWorking());
        tag.putBoolean("IsPaused", entity.getFuelLogic().isPaused());

        if (entity.getFuelLogic().getCurrentFuelId() != null) {
            tag.putString("CurrentFuelId", entity.getFuelLogic().getCurrentFuelId().toString());
        }

        NbtUtils.write(tag, "Mana", entity.getManaStorage(), provider);
        NbtUtils.write(tag, "Energy", entity.getEnergyStorage(), provider);
        NbtUtils.write(tag, "FuelItems", entity.getFuelHandler(), provider);
        NbtUtils.writeEnumBooleanMap(tag, "DirectionConfig", entity.getDirectionConfig());
    }

    public void load(CompoundTag tag, HolderLookup.Provider provider) {
//        MagicalIndustryMod.LOGGER.info("[Client] loaded IsWorking = {}", tag.getBoolean("IsWorking"));

        entity.getStateManager().setModeIndex(tag.getInt("Mode"));
        entity.setBurnTimeFromNbt(tag.getInt("BurnTime"));
        entity.setCurrentBurnTimeFromNbt(tag.getInt("CurrentBurnTime"));
        entity.getStateManager().setWorking(tag.getBoolean("IsWorking"));
        entity.getFuelLogic().setPaused(tag.getBoolean("IsPaused"));

        if (tag.contains("CurrentFuelId")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("CurrentFuelId"));
            entity.getFuelLogic().setCurrentFuelId(id);
        }

        NbtUtils.read(tag, "Mana", entity.getManaStorage(), provider);
        NbtUtils.read(tag, "Energy", entity.getEnergyStorage(), provider);
        NbtUtils.read(tag, "FuelItems", entity.getFuelHandler(), provider);
        NbtUtils.readEnumBooleanMap(tag, "DirectionConfig", entity.getDirectionConfig());

        entity.forceRefreshAnimationFromNbt();
    }
}
