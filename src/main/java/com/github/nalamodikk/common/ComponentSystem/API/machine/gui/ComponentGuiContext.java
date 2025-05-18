package com.github.nalamodikk.common.ComponentSystem.API.machine.gui;

import com.github.nalamodikk.client.screenAPI.gui.api.IGuiDataContext;
import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.IItemHandler;

public class ComponentGuiContext implements IGuiDataContext {
    private final ComponentGrid grid;
    private final BlockPos pos;
    private final IGridComponent component;
    private final CompoundTag data;

    public ComponentGuiContext(ComponentGrid grid, BlockPos pos, IGridComponent component, CompoundTag data) {
        this.grid = grid;
        this.pos = pos;
        this.component = component;
        this.data = data;

    }

    public CompoundTag getData() {
        return data;
    }


    @Override
    public <T> T query(String key) {
        return switch (key) {
            case "grid" -> (T) grid;
            case "blockPos" -> (T) pos;
            case "component" -> (T) component;
            default -> null;
        };
    }

    @Override
    public Object host() {
        return grid;
    }

    @Override
    public IItemHandler getItemHandler() {
        return null;
    }
}
