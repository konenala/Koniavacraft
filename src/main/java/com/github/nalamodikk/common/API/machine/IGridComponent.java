package com.github.nalamodikk.common.API.machine;

import com.github.nalamodikk.common.API.machine.grid.ComponentGrid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

import java.util.List;

public interface IGridComponent {
    // 唯一 ID，例如 magicalindustry:mana_core
    ResourceLocation getId();

    // 放進格子時觸發
    void onAdded(ComponentGrid grid, BlockPos pos);

    // 拿出格子時觸發
    void onRemoved(ComponentGrid grid, BlockPos pos);

    // 存模組自己的狀態
    void saveToNBT(CompoundTag tag);

    // 載入模組自己的狀態
    void loadFromNBT(CompoundTag tag);

    CompoundTag getData(); // ✅ 你自己加的
    default List<IComponentBehavior> getBehaviors() {
        return List.of(); // 沒有預設行為
    }


}