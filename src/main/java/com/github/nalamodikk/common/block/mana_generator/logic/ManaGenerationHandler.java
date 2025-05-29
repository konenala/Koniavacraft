package com.github.nalamodikk.common.block.mana_generator.logic;


import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.block.mana_generator.recipe.loader.ManaGenFuelRateLoader;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ManaGenerationHandler {
    private final IUnifiedManaHandler manaStorage;
    private final Supplier<Optional<ManaGenFuelRateLoader.FuelRate>> fuelRateSupplier;
    private int tickCounter = 0;
    private final Consumer<Integer> onGenerate;

    public ManaGenerationHandler(IUnifiedManaHandler manaStorage, Supplier<Optional<ManaGenFuelRateLoader.FuelRate>> fuelRateSupplier, Consumer<Integer> onGenerate) {
        this.manaStorage = manaStorage;
        this.fuelRateSupplier = fuelRateSupplier;
        this.onGenerate = onGenerate;

    }

    public void resetTickCounter() {
        this.tickCounter = 0;
    }


    public boolean generate() {
        Optional<ManaGenFuelRateLoader.FuelRate> opt = fuelRateSupplier.get();
        if (opt.isEmpty() || manaStorage == null || !manaStorage.canReceive()) return false;

        ManaGenFuelRateLoader.FuelRate rate = opt.get();
        tickCounter++;

        if (tickCounter >= rate.getIntervalTick()) {
            tickCounter = 0;
            manaStorage.addMana(rate.getManaRate());
            return true;
        }

        return false;
    }


}

