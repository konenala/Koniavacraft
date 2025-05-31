package com.github.nalamodikk.common.datagen.recipe;

import com.github.nalamodikk.common.block.mana_crafting.ManaCraftingTableRecipe;
import com.github.nalamodikk.common.block.mana_crafting.recipe.ManaCraftingRecipeBuilder;
import com.github.nalamodikk.common.register.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManaCraftingRecipeProvider {
    public static ManaCraftingTableRecipe shaped(ResourceLocation id, List<String> pattern, Map<String, Ingredient> stringKey, ItemStack result, int manaCost) {
        // 把 String 轉成 Character
        Map<Character, Ingredient> charKey = new HashMap<>();
        for (Map.Entry<String, Ingredient> entry : stringKey.entrySet()) {
            String keyStr = entry.getKey();
            if (keyStr.length() != 1) {
                throw new IllegalArgumentException("Key must be single character, got: '" + keyStr + "'");
            }
            charKey.put(keyStr.charAt(0), entry.getValue());
        }

        return ManaCraftingTableRecipe.createShaped(pattern, charKey, result, manaCost).withId(id);
    }


    public static void generate(RecipeOutput output) {
        // ✅ 無序合成配方
        ManaCraftingRecipeBuilder.create(ModItems.MANA_DUST.get(), 1)
                .addIngredient(Ingredient.of(Items.DIAMOND))
                .manaCost(5000)
                .save(output);

        // ✅ 有序合成配方
//        ManaCraftingRecipeBuilder.create(ModItems.MANA_STAFF.get(), 1)
//                .pattern(" A ")
//                .pattern(" B ")
//                .pattern(" C ")
//                .define('A', Items.IRON_INGOT)
//                .define('B', Items.STICK)
//                .define('C', Items.DIAMOND)
//                .manaCost(2000)
//                .save(output);

    }

}
