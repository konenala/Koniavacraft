package com.github.nalamodikk.common.ComponentSystem.API.machine.component;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IComponentBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.BaseGridComponent;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.capability.IHasMana;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentBehaviorRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ManaStorageComponent  extends BaseGridComponent implements IGridComponent, IHasMana {
    private final ManaStorage storage = new ManaStorage(1000); // 預設容量
    private CompoundTag behaviorData = new CompoundTag(); // ⭐ 來自物品的行為設定

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana_storage");
    }

    @Override
    public void saveToNBT(CompoundTag tag) {
        tag.put("mana", storage.serializeNBT());
        tag.put("behavior", behaviorData); // ⭐ 一併儲存行為參數
    }

    @Override
    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("mana")) {
            storage.deserializeNBT(tag.getCompound("mana"));
        }
        if (tag.contains("behavior")) {
            behaviorData = tag.getCompound("behavior"); // ⭐ 還原來自物品的資料
        }
    }

    @Override
    public CompoundTag getData() {
        // ❗ 只返回物品初始時的 NBT，而不是 storage 現況
        CompoundTag tag = new CompoundTag();
        tag.put("behavior", behaviorData.copy()); // ⭐ 一定要 copy，否則會交叉污染
        return tag;
    }


    @Override
    public List<IComponentBehavior> getBehaviors() {
        return List.of(ComponentBehaviorRegistry.create("mana_producer", behaviorData.copy()));
    }

    @Override
    public IUnifiedManaHandler getManaStorage() {
        return storage;
    }

    @Override
    public void onAdded(ComponentGrid grid, BlockPos pos) {}

    @Override
    public void onRemoved(ComponentGrid grid, BlockPos pos) {}

    /** ⭐ 由 ModuleItem 呼叫，提供初始值用 */
    public void setBehaviorData(CompoundTag behaviorData) {
        this.behaviorData = behaviorData;
    }
}
