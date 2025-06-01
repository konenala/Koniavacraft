package com.github.nalamodikk.system.data;

import com.github.nalamodikk.system.api.INaraData;
import net.minecraft.nbt.CompoundTag;

public class NaraData implements INaraData {
    private boolean isBound = false;

    @Override
    public boolean isBound() {
        return isBound;
    }

    @Override
    public void setBound(boolean bound) {
        this.isBound = bound;
    }

    public void saveNBT(CompoundTag tag) {
        tag.putBoolean("IsBound", isBound);
    }

    public void loadNBT(CompoundTag tag) {
        this.isBound = tag.getBoolean("IsBound");
    }
}
