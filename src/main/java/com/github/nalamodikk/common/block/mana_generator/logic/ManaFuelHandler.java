package com.github.nalamodikk.common.block.mana_generator.logic;

import com.github.nalamodikk.common.block.mana_generator.recipe.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.block.mana_generator.recipe.loader.ManaGenFuelRateLoader.FuelRate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ManaFuelHandler {

    private final ItemStackHandler fuelHandler;
    private ResourceLocation currentFuelId;
    private int burnTime;
    private int currentBurnTime;
    private int failedFuelCooldown;

    public ManaFuelHandler(ItemStackHandler fuelHandler) {
        this.fuelHandler = fuelHandler;
    }

    public boolean tryConsumeFuel() {
        ItemStack fuel = fuelHandler.getStackInSlot(0);
        if (fuel.isEmpty()) return false;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(fuel.getItem());
        FuelRate rate = ManaGenFuelRateLoader.getFuelRateForItem(id);

        if (rate == null || rate.getBurnTime() <= 0 || rate.getManaRate() <= 0) return false;

        currentFuelId = id;
        currentBurnTime = rate.getBurnTime();
        burnTime = currentBurnTime;
        fuelHandler.extractItem(0, 1, false);

        return true;
    }

    public static Map<Item, Integer> getAllFuelItems() {
        Map<Item, Integer> fuelMap = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            int burnTime = net.minecraft.world.level.block.entity.FurnaceBlockEntity.getFuel().getOrDefault(item, 0);
            if (burnTime > 0) fuelMap.put(item, burnTime);
        }
        return fuelMap;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getCurrentBurnTime() {
        return currentBurnTime;
    }

    public ResourceLocation getCurrentFuelId() {
        return currentFuelId;
    }

    public boolean isCoolingDown() {
        return failedFuelCooldown > 0;
    }

    public void tickCooldown() {
        if (failedFuelCooldown > 0) failedFuelCooldown--;
    }

    public void setCooldown() {
        this.failedFuelCooldown = 20;
    }

    public void resetBurnTime() {
        this.burnTime = 0;
        this.currentBurnTime = 0;
    }

    public void tickBurn() {
        if (burnTime > 0) burnTime--;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    public Optional<ManaGenFuelRateLoader.FuelRate> getCurrentFuelRate() {
        if (currentFuelId == null) return Optional.empty();

        ManaGenFuelRateLoader.FuelRate rate = ManaGenFuelRateLoader.getFuelRateForItem(currentFuelId);
        return Optional.ofNullable(rate);
    }

}
