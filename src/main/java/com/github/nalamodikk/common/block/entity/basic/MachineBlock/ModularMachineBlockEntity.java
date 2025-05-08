package com.github.nalamodikk.common.block.entity.basic.MachineBlock;

import com.github.nalamodikk.common.API.machine.IComponentBehavior;
import com.github.nalamodikk.common.API.machine.IGridComponent;
import com.github.nalamodikk.common.API.machine.grid.ComponentContext;
import com.github.nalamodikk.common.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.capability.IHasMana;
import com.github.nalamodikk.common.item.ModuleItem;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
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
    public void tick() {
        if (level == null || level.isClientSide) return;

        ItemStack stack = itemHandler.getStackInSlot(0);
        componentGrid.getAllComponents().clear(); // 清空重建

        if (!stack.isEmpty()) {
            List<IGridComponent> components = ModuleItem.getComponents(stack); // 從物品 NBT 讀元件
            int index = 0;
            for (IGridComponent component : components) {
                int x = index % 3 - 1; // -1, 0, 1 (中間是0)
                int y = index / 3 - 1;
                componentGrid.setComponent(x, y, component);
                index++;
            }
        }

        // Tick 所有模組內的行為
        for (Map.Entry<BlockPos, IGridComponent> entry : componentGrid.getAllComponents().entrySet()) {
            BlockPos pos = entry.getKey();
            IGridComponent component = entry.getValue();
            ComponentContext context = new ComponentContext(componentGrid, pos, component);

            for (IComponentBehavior behavior : component.getBehaviors()) {
                behavior.onTick(context);
            }
        }
    }

}
