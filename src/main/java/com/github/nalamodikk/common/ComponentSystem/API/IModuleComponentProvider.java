package com.github.nalamodikk.common.ComponentSystem.API;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import net.minecraft.world.item.ItemStack;

public interface IModuleComponentProvider {
    IGridComponent getComponent(ItemStack stack);
}
