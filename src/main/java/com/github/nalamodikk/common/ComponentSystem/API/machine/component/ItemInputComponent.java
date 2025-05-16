package com.github.nalamodikk.common.ComponentSystem.API.machine.component;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.BaseGridComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentContext;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 可拼裝的輸入模組，每放一個升級模組可增加輸入槽位。
 */
public class ItemInputComponent extends BaseGridComponent implements IGridComponent {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1);

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "item_input");
    }

    @Override
    public void onAdded(ComponentGrid grid, BlockPos pos) {
        int upgradeCount = (int) grid.getAllComponents().values().stream()
                .filter(c -> c.getId().toString().equals(MagicalIndustryMod.MOD_ID + ":input_upgrade"))
                .count();
        itemHandler.setSize(1 + upgradeCount);
    }

    @Override
    public void onRemoved(ComponentGrid grid, BlockPos pos) {

    }

    @Override
    public void saveToNBT(CompoundTag tag) {
        tag.put("items", itemHandler.serializeNBT());
    }

    @Override
    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("items")) {
            itemHandler.deserializeNBT(tag.getCompound("items"));
        }
    }

    @Override
    public CompoundTag getData() {
        return new CompoundTag();
    }


    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }
}
