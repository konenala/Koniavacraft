package com.github.nalamodikk.common.compat.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.BigInteger;

public class ModNeoNalaEnergyStorage implements IEnergyStorage {
    private static final int DECIMAL_DIGITS = 4;
    private BigDecimal energy;
    private final BigDecimal capacity;

    public ModNeoNalaEnergyStorage(BigInteger capacity) {
        this.energy = BigDecimal.ZERO.setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        this.capacity = new BigDecimal(capacity).setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        BigDecimal amount = BigDecimal.valueOf(maxReceive);
        BigDecimal accepted = amount.min(capacity.subtract(energy));
        if (!simulate) energy = energy.add(accepted);
        return accepted.intValue();
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        BigDecimal amount = BigDecimal.valueOf(maxExtract);
        BigDecimal extracted = energy.min(amount);
        if (!simulate) energy = energy.subtract(extracted);
        return extracted.intValue();
    }

    @Override public int getEnergyStored() { return energy.intValue(); }
    @Override public int getMaxEnergyStored() { return capacity.intValue(); }
    @Override public boolean canExtract() { return energy.compareTo(BigDecimal.ZERO) > 0; }
    @Override public boolean canReceive() { return energy.compareTo(capacity) < 0; }
}
