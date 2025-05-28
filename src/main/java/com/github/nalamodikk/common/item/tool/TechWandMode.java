package com.github.nalamodikk.common.item.tool;


import net.minecraft.util.StringRepresentable;

public enum TechWandMode implements StringRepresentable {
    CONFIGURE_IO,
    DIRECTION_CONFIG,
    ROTATE;

    public TechWandMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public TechWandMode previous() {
        return values()[(this.ordinal() - 1 + values().length) % values().length];
    }

    public TechWandMode cycle(boolean forward) {
        TechWandMode[] values = values();
        int index = this.ordinal();
        int nextIndex = (index + (forward ? 1 : -1) + values.length) % values.length;
        return values[nextIndex];
    }

    @Override
    public String getSerializedName() {
        return this.name(); // 或 .toLowerCase() 也可以配合本地化
    }
}
