package com.github.nalamodikk.common.compat.energy;

import net.minecraftforge.energy.IEnergyStorage;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class ManaEnergyStorage implements IEnergyStorage {
    private BigDecimal energy; // 高精度能量存儲
    private final BigDecimal maxCapacity;
    private final int DECIMAL_DIGITS = 4;

    // 構造函數，支援 int, long, BigInteger
    public ManaEnergyStorage(int capacity) {
        this.energy = BigDecimal.ZERO.setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        this.maxCapacity = BigDecimal.valueOf(capacity);
    }

    public ManaEnergyStorage(long capacity) {
        this.energy = BigDecimal.ZERO.setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        this.maxCapacity = BigDecimal.valueOf(capacity);
    }

    public ManaEnergyStorage(BigInteger capacity) {
        this.energy = BigDecimal.ZERO.setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
        this.maxCapacity = new BigDecimal(capacity);
    }

    public void setEnergy(BigDecimal value) {
        this.energy = value.min(maxCapacity).max(BigDecimal.ZERO).setScale(DECIMAL_DIGITS, RoundingMode.DOWN);
    }

    // 能量接收（支援 int, long, BigInteger）
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return receiveEnergy(BigDecimal.valueOf(maxReceive), simulate).intValue();
    }

    public long receiveEnergy(long maxReceive, boolean simulate) {
        return receiveEnergy(BigDecimal.valueOf(maxReceive), simulate).longValue();
    }

    public BigInteger receiveEnergy(BigInteger maxReceive, boolean simulate) {
        return receiveEnergy(new BigDecimal(maxReceive), simulate).toBigInteger();
    }

    private BigDecimal receiveEnergy(BigDecimal amount, boolean simulate) {
        BigDecimal acceptedEnergy = amount.min(maxCapacity.subtract(energy));
        if (!simulate) {
            energy = energy.add(acceptedEnergy);
        }
        return acceptedEnergy;
    }

    // 能量提取（支援 int, long, BigInteger）
    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return extractEnergy(BigDecimal.valueOf(maxExtract), simulate).intValue();
    }

    public long extractEnergy(long maxExtract, boolean simulate) {
        return extractEnergy(BigDecimal.valueOf(maxExtract), simulate).longValue();
    }

    public BigInteger extractEnergy(BigInteger maxExtract, boolean simulate) {
        return extractEnergy(new BigDecimal(maxExtract), simulate).toBigInteger();
    }

    private BigDecimal extractEnergy(BigDecimal amount, boolean simulate) {
        BigDecimal extractedEnergy = energy.min(amount);
        if (!simulate) {
            energy = energy.subtract(extractedEnergy);
        }
        return extractedEnergy;
    }

    // 取得當前能量值
    @Override
    public int getEnergyStored() {
        return energy.intValue();
    }

    public long getEnergyStoredLong() {
        return energy.longValue();
    }

    public BigInteger getEnergyStoredBigInt() {
        return energy.toBigInteger();
    }

    @Override
    public int getMaxEnergyStored() {
        return maxCapacity.intValue();
    }

    public long getMaxEnergyStoredLong() {
        return maxCapacity.longValue();
    }

    public BigInteger getMaxEnergyStoredBigInt() {
        return maxCapacity.toBigInteger();
    }

    @Override
    public boolean canExtract() {
        return energy.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean canReceive() {
        return energy.compareTo(maxCapacity) < 0;
    }

    // 其他功能
    public boolean isFull() {
        return energy.compareTo(maxCapacity) >= 0;
    }

    public boolean isEmpty() {
        return energy.compareTo(BigDecimal.ZERO) == 0;
    }

    public void addEnergy(BigDecimal amount) {
        setEnergy(energy.add(amount));
    }

    public void subtractEnergy(BigDecimal amount) {
        setEnergy(energy.subtract(amount));
    }
}
