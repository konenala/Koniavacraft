package com.github.nalamodikk.test;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import net.minecraft.nbt.CompoundTag;

public class DummyManaStorage implements IUnifiedManaHandler {
    private int mana = 0;
    private final int capacity = 1000;

    @Override public int getMana() { return mana; }

    @Override public void setMana(int amount) { this.mana = amount; }

    @Override public void addMana(int amount) {
        this.mana = Math.min(mana + amount, capacity);
    }

    @Override public void consumeMana(int amount) {
        this.mana = Math.max(mana - amount, 0);
    }

    @Override public void onChanged() {}

    @Override public int getMaxMana() { return capacity; }

    @Override public boolean canExtract() { return false; }

    @Override
    public int insertMana(int amount, ManaAction action) {
        int toInsert = Math.min(capacity - mana, amount);
        if (action.execute()) {
            mana += toInsert;
            System.out.println("[DEBUG] 接收到 mana：" + toInsert);
        }
        return toInsert;
    }

    @Override public int extractMana(int amount, ManaAction action) { return 0; }

    @Override public int getManaContainerCount() { return 1; }

    @Override public int getMana(int container) { return mana; }

    @Override public void setMana(int container, int mana) { this.mana = mana; }

    @Override public int getMaxMana(int container) { return capacity; }

    @Override public int getNeededMana(int container) { return capacity - mana; }

    @Override public int insertMana(int container, int amount, ManaAction action) {
        return insertMana(amount, action);
    }

    @Override public int extractMana(int container, int amount, ManaAction action) {
        return 0;
    }

    public CompoundTag serializeNBT() { return new CompoundTag(); }
    public void deserializeNBT(CompoundTag tag) {}
}
