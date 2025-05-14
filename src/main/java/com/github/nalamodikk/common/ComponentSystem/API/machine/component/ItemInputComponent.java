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
    private final ItemStackHandler itemHandler = new ItemStackHandler(1); // 預設一格
    private CompoundTag behaviorData = new CompoundTag(); // ⬅️ 來自物品的設定

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "item_input");
    }

    @Override
    public void onAdded(ComponentGrid grid, BlockPos pos) {
        ComponentContext context = new ComponentContext(grid, pos, this);
        // 計算拼裝內有幾個升級模組
        int upgradeCount = (int) grid.getAllComponents().values().stream()
                .filter(c -> c.getId().toString().equals(MagicalIndustryMod.MOD_ID + ":input_upgrade"))
                .count();

        // 擴充格數 = 1 + 升級模組數
        int newSlots = 1 + upgradeCount;
        MagicalIndustryMod.LOGGER.debug("🔧 ItemInputComponent: 偵測到 {} 個 input_upgrade，總槽位數設為 {}", upgradeCount, newSlots);
        itemHandler.setSize(newSlots);
    }

    @Override
    public void onRemoved(ComponentGrid grid, BlockPos pos) {
        // 目前不需要釋放什麼東西，保留擴充點
    }
    public void setBehaviorData(CompoundTag behaviorData) {
        this.behaviorData = behaviorData;
    }

    @Override
    public void saveToNBT(CompoundTag tag) {
        tag.put("items", itemHandler.serializeNBT());
        tag.put("behavior", behaviorData);
    }

    @Override
    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("items")) {
            itemHandler.deserializeNBT(tag.getCompound("items"));
        }
        if (tag.contains("behavior")) {
            behaviorData = tag.getCompound("behavior");
        }
    }

    @Override
    public CompoundTag getData() {
        CompoundTag tag = new CompoundTag();
        tag.put("behavior", behaviorData.copy()); // ⭐ 記得要 copy，避免交叉污染
        return tag;
    }

    /**
     * 提供行為存取此輸入模組的項目儲存槽
     */
    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }
}
