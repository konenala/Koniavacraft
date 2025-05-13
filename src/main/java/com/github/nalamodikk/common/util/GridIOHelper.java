package com.github.nalamodikk.common.util;

import com.github.nalamodikk.common.ComponentSystem.API.machine.component.ItemInputComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.component.ItemOutputComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentGrid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class GridIOHelper {
    public static void readOrInitFromNBT(ComponentGrid grid, CompoundTag tag) {
        if (tag.contains("grid", Tag.TAG_COMPOUND)) {
            grid.loadFromNBT(tag.getCompound("grid"));
        } else {
            grid.clear(); // ✅ 若你有清空方法，這樣可以重設拼裝狀態
        }
    }

    public static void writeToNBTIfPresent(ComponentGrid grid, CompoundTag tag) {
        CompoundTag gridTag = new CompoundTag();
        grid.saveToNBT(gridTag);
        if (!gridTag.isEmpty()) {
            tag.put("grid", gridTag);
        }
    }

    /**
     * 取得 Grid 中所有 ItemInputComponent（可選條件過濾）
     */
    public static List<ItemInputComponent> getAllInputs(ComponentGrid grid, Predicate<ItemInputComponent> filter) {
        List<ItemInputComponent> result = new ArrayList<>();
        for (var component : grid.getAllComponents().values()) {
            if (component instanceof ItemInputComponent input && filter.test(input)) {
                result.add(input);
            }
        }
        return result;
    }

    /**
     * 無條件版本（全部回傳）
     */
    public static List<ItemInputComponent> getAllInputs(ComponentGrid grid) {
        return getAllInputs(grid, c -> true);
    }

    /**
     * 取得 Grid 中所有 ItemOutputComponent（可選條件過濾）
     */
    public static List<ItemOutputComponent> getAllOutputs(ComponentGrid grid, Predicate<ItemOutputComponent> filter) {
        List<ItemOutputComponent> result = new ArrayList<>();
        for (var component : grid.getAllComponents().values()) {
            if (component instanceof ItemOutputComponent output && filter.test(output)) {
                result.add(output);
            }
        }
        return result;
    }

    /**
     * 無條件版本（全部回傳）
     */
    public static List<ItemOutputComponent> getAllOutputs(ComponentGrid grid) {
        return getAllOutputs(grid, c -> true);
    }

    public static boolean insertIntoAnyOutputSlot(ComponentGrid grid, ItemStack stack) {
        for (var output : getAllOutputs(grid)) {
            var handler = output.getItemHandler();
            for (int i = 0; i < handler.getSlots(); i++) {
                if (handler.insertItem(i, stack.copy(), true).isEmpty()) {
                    handler.insertItem(i, stack.copy(), false);
                    return true;
                }
            }
        }
        return false;
    }

}
