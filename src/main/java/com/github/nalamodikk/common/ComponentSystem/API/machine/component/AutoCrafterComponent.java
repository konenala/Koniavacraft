package com.github.nalamodikk.common.ComponentSystem.API.machine.component;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IComponentBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.IControllableBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.behavior.CraftingBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.BaseGridComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentContext;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentBehaviorRegistry;
import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class AutoCrafterComponent extends BaseGridComponent implements IGridComponent {
    private CompoundTag behaviorData = new CompoundTag(); // ⬅️ 設定是否啟用自動合成

    private boolean guiToggle = true; // 預設啟用，可由 GUI 改變

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "auto_crafter");
    }

    @Override
    public void onAdded(ComponentGrid grid, BlockPos pos) {
        // 這裡可以同步 toggle 狀態給行為
        syncToggleToBehavior(grid);
    }

    @Override
    public void onRemoved(ComponentGrid grid, BlockPos pos) {}

    @Override
    public void saveToNBT(CompoundTag tag) {
        tag.put("behavior", behaviorData);
        tag.putBoolean("enabled", guiToggle); // ✅ 儲存 GUI 狀態

    }

    @Override
    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("behavior")) {
            behaviorData = tag.getCompound("behavior");
        }
        if (tag.contains("enabled")) {
            guiToggle = tag.getBoolean("enabled"); // ✅ 還原 GUI 狀態
        }
    }

    @Override
    public CompoundTag getData() {
        CompoundTag tag = new CompoundTag();
        tag.put("behavior", behaviorData.copy());
        return tag;
    }
    @Override
    public List<IComponentBehavior> getBehaviors() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("enabled", guiToggle); // 將啟用狀態存入傳給行為
        CraftingBehavior behavior = (CraftingBehavior) ComponentBehaviorRegistry.create("crafting", tag);
        return List.of(behavior);
    }

    public void setBehaviorData(CompoundTag behaviorData) {
        this.behaviorData = behaviorData;
    }
    public void toggle() {
        this.guiToggle = !guiToggle;
    }

    public boolean isGuiToggle() {
        return guiToggle;
    }

    public void setGuiToggle(boolean toggle, ComponentGrid grid) {
        this.guiToggle = toggle;
        syncToggleToBehavior(grid);
    }

    private void syncToggleToBehavior(ComponentGrid grid) {
        CraftingBehavior behavior = grid.findBehavior(CraftingBehavior.class);
        if (behavior != null) {
            behavior.setEnabled(guiToggle);
        }
    }

}
