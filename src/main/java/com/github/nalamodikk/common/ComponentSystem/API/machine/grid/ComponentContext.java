package com.github.nalamodikk.common.ComponentSystem.API.machine.grid;


import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * 組件執行上下文：提供模組所在位置、自己、grid 與鄰近模組資訊
 */
public class ComponentContext {
    private final ComponentGrid grid;
    private final BlockPos pos;
    private final IGridComponent self;

    public ComponentContext(ComponentGrid grid, BlockPos pos, IGridComponent self) {
        this.grid = grid;
        this.pos = pos;
        this.self = self;
    }

    public ComponentGrid grid() {
        return grid;
    }

    public BlockPos pos() {
        return pos;
    }

    public IGridComponent self() {
        return self;
    }

    /**
     * 對每個鄰近模組執行操作（上下左右）
     */
    public void forEachNeighbor(Consumer<IGridComponent> action) {
        for (BlockPos offset : new BlockPos[]{
                pos.north(), pos.south(), pos.east(), pos.west(), pos.above(), pos.below()
        }) {
            IGridComponent neighbor = grid.getComponent(offset);
            if (neighbor != null) {
                action.accept(neighbor);
            }
        }
    }


    public Level getLevel() {
        return this.grid.getLevel();
    }

    public BlockPos getCenterPos() {
        return this.pos;
    }

}
