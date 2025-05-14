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
 * 可拼裝的輸出模組，每放一個升級模組可增加輸出槽位。
 */
public class ItemOutputComponent  extends BaseGridComponent implements IGridComponent {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1); // 預設 1 格輸出槽

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "item_output");
    }

    @Override
    public void onAdded(ComponentGrid grid, BlockPos pos) {
        ComponentContext context = new ComponentContext(grid, pos, this);
        int upgradeCount = (int) grid.getAllComponents().values().stream()
                .filter(c -> c.getId().toString().equals(MagicalIndustryMod.MOD_ID + ":output_upgrade"))
                .count();

        int newSlots = 1 + upgradeCount;
        MagicalIndustryMod.LOGGER.debug("🔧 ItemOutputComponent: 偵測到 {} 個 output_upgrade，總槽位數設為 {}", upgradeCount, newSlots);
        itemHandler.setSize(newSlots);
    }

    @Override
    public void onRemoved(ComponentGrid grid, BlockPos pos) {
        // 尚無特殊釋放需求
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
        return new CompoundTag(); // ❗ 沒有特殊參數，回傳空即可
    }


    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }
}
