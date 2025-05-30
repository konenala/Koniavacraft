package com.github.nalamodikk.common.API;

import net.minecraft.core.Direction;

import java.util.EnumMap;

public interface IConfigurableBlock {
    void setDirectionConfig(Direction direction, boolean isOutput);
    boolean isOutput(Direction direction);
    EnumMap<Direction, Boolean> getDirectionConfig(); // ✅ 新增這個

}
