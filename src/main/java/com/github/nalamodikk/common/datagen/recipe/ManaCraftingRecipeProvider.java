package com.github.nalamodikk.common.datagen.recipe;

import com.github.nalamodikk.common.block.mana_crafting.ManaCraftingTableRecipe;
import com.github.nalamodikk.common.block.mana_crafting.recipe.ManaCraftingRecipeBuilder;
import com.github.nalamodikk.common.register.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

public class ManaCraftingRecipeProvider {

    public static ManaCraftingTableRecipe shaped(ResourceLocation id, List<String> pattern, Map<String, Ingredient> key, ItemStack result, int manaCost) {
        return ManaCraftingTableRecipe.fromCodec(true, manaCost, pattern, key, List.of(), result).withId(id);
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
