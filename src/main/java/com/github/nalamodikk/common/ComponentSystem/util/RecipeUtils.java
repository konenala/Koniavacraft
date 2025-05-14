package com.github.nalamodikk.common.ComponentSystem.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

/**
 * 舊版配方比對工具，僅作為相容用途保留
 */
public class RecipeUtils {

    public static boolean legacyMatchInputs(List<ItemStack> inputs, List<Ingredient> ingredients) {
        List<ItemStack> remaining = new ArrayList<>(inputs);

        for (Ingredient ing : ingredients) {
            boolean matched = false;
            for (ItemStack stack : remaining) {
                if (ing.test(stack)) {
                    remaining.remove(stack);
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }

        return true;
    }
}
