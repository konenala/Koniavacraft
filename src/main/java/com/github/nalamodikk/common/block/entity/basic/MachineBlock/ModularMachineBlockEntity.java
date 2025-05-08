package com.github.nalamodikk.common.block.entity.basic.MachineBlock;

import com.github.nalamodikk.common.API.machine.IComponentBehavior;
import com.github.nalamodikk.common.API.machine.IGridComponent;
import com.github.nalamodikk.common.API.machine.grid.ComponentContext;
import com.github.nalamodikk.common.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.capability.IHasMana;
import com.github.nalamodikk.common.item.ModuleItem;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;
import java.util.Map;

public class ModularMachineBlockEntity extends BlockEntity {
    private ComponentGrid componentGrid;
    private final ItemStackHandler itemHandler = new ItemStackHandler(1); // 玩家放模組物品

    public ModularMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MODULAR_MACHINE_BE.get(), pos, state);
        this.componentGrid = new ComponentGrid(this.getLevel()); // 傳入世界物件
    }
    private ItemStack lastStack = ItemStack.EMPTY; // 加在 class 裡面做快取

    public void tick() {
        if (level == null || level.isClientSide) return;

        ItemStack stack = itemHandler.getStackInSlot(0);

        // ✅ 只有在物品變更時才重新載入 Grid 結構
        if (!ItemStack.isSameItemSameTags(stack, lastStack)) {
            lastStack = stack.copy(); // 更新快取
            if (!stack.isEmpty()) {
                CompoundTag gridTag = stack.getOrCreateTag().getCompound("grid");
                componentGrid.loadFromNBT(gridTag); // 差異更新
            }
        }

        // ✅ 每 tick 執行 Grid 的行為邏輯
        componentGrid.tick();
    }

    public ComponentGrid getGrid() {
        return this.componentGrid;
    }

}
