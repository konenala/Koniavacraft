package com.github.nalamodikk.common.ComponentSystem.util;

import com.github.nalamodikk.common.ComponentSystem.recipe.component.AssemblyRecipe;
import com.github.nalamodikk.common.ComponentSystem.recipe.component.CountedIngredient;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AssemblyRecipeUtil {

    public static AssemblyRecipe findMatchingRecipe(List<AssemblyRecipe> recipes, List<ItemStack> inputs) {
        outer:
        for (AssemblyRecipe recipe : recipes) {
            List<CountedIngredient> required = recipe.getInputItems();
            if (required.size() > inputs.size()) continue;

            List<ItemStack> tempInputs = new ArrayList<>();
            for (ItemStack original : inputs) {
                tempInputs.add(original.copy());
            }

            for (CountedIngredient counted : required) {
                boolean matched = false;
                for (int i = 0; i < tempInputs.size(); i++) {
                    ItemStack stack = tempInputs.get(i);
                    if (counted.getIngredient().test(stack) && stack.getCount() >= counted.getCount()) {
                        stack.shrink(counted.getCount());
                        if (stack.isEmpty()) tempInputs.remove(i);
                        matched = true;
                        break;
                    }
                }
                if (!matched) continue outer;
            }

            return recipe;
        }

        return null;
    }
}
