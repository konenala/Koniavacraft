package com.github.nalamodikk.common.capability;

import com.github.nalamodikk.common.capability.mana.ManaAction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.INBTSerializable;

public class ManaStorage implements IUnifiedManaHandler , INBTSerializable<CompoundTag> {
    public static final Capability<ManaStorage> MANA = CapabilityManager.get(new CapabilityToken<>() {});

    private int mana;
    private final int capacity;

    public ManaStorage(int capacity) {
        this.capacity = capacity;
        this.mana = 0;
    }

    public boolean isFull() {
        return this.getManaStored() >= this.getMaxManaStored();
    }

    @Override
    public void addMana(int amount) {
        this.mana = Math.min(this.mana + amount, capacity);
        onChanged(); // 添加魔力時通知變化
    }

    @Override
    public void consumeMana(int amount) {
        this.mana = Math.max(this.mana - amount, 0);
        onChanged(); // 消耗魔力時通知變化
    }

    @Override
    public int getManaStored() {
        return mana;
    }

    @Override
    public void setMana(int amount) {
        this.mana = Math.min(amount, capacity);
        onChanged(); // 設置魔力時通知變化
    }

    @Override
    public void onChanged() {
        // 這裡可以加入狀態同步的邏輯（如果需要的話）
    }

    @Override
    public int getMaxManaStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return this.mana > 0; // 只要魔力大於 0 就允許提取
    }

    /** 這裡修正多槽位的問題，因為這個 class 只有一個 Mana 容器 */
    @Override
    public int getManaContainerCount() {
        return 1; // 這是一個單獨的 Mana 儲存容器
    }

    @Override
    public int getManaStored(int container) {
        return container == 0 ? getManaStored() : 0; // 只支援 container 0
    }

    @Override
    public void setMana(int container, int mana) {
        if (container == 0) {
            setMana(mana);
        }
    }

    @Override
    public int getMaxManaStored(int container) {
        return container == 0 ? getMaxManaStored() : 0;
    }

    @Override
    public int getNeededMana(int container) {
        return container == 0 ? getMaxManaStored() - getManaStored() : 0;
    }

    @Override
    public int insertMana(int container, int amount, ManaAction action) {
        if (container != 0) {
            return amount; // 只允許 container 0 存魔力
        }
        return insertMana(amount, action);
    }

    @Override
    public int extractMana(int container, int amount, ManaAction action) {
        if (container != 0) {
            return 0; // 只允許 container 0 提取魔力
        }
        return extractMana(amount, action);
    }

    @Override
    public int extractMana(int amount, ManaAction action) {
        if (amount <= 0 || mana == 0) {
            return 0;
        }

        int manaExtracted = Math.min(amount, mana);

        if (action.execute() && manaExtracted > 0) {
            mana -= manaExtracted;
            onChanged(); // 確保變更被通知
        }

        return manaExtracted;
    }

    @Override
    public int receiveMana(int amount, ManaAction action) {
        if (amount <= 0) {
            return 0;
        }
        int toReceive = Math.min(amount, getMaxManaStored() - getManaStored());
        if (action.execute() && toReceive > 0) {
            addMana(toReceive);
            onChanged(); // 通知數據變更
        }
        return toReceive;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Mana", this.mana);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.mana = nbt.getInt("Mana");
    }

}
