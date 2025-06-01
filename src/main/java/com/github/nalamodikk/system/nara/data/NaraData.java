package com.github.nalamodikk.system.nara.data;

import com.github.nalamodikk.system.nara.api.INaraData;
import net.minecraft.nbt.CompoundTag;

public class NaraData implements INaraData {
    private boolean isBound = false;
    private boolean bound = false;

    @Override
    public boolean isBound() {
        return bound;
    }

    @Override
    public void setBound(boolean value) {
        this.bound = value;
    }

    // 用於儲存和載入資料（NeoForge 要手動序列化）
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Bound", bound);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.bound = tag.getBoolean("Bound");
    }
}
