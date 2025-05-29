package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.common.block.mana_generator.recipe.ManaGenFuelRecipeBuilder;
import com.github.nalamodikk.common.register.ModItems;
import net.minecraft.world.item.Items;
import net.minecraft.data.recipes.RecipeOutput;

public class ManaFuelRecipeProvider {

    public static void generate(RecipeOutput output) {
        // 🪵 Minecraft 基礎燃料
        ManaGenFuelRecipeBuilder.create(Items.COAL, 10, 5, 200).save(output);
        ManaGenFuelRecipeBuilder.create(Items.BLAZE_ROD, 30, 0, 1600).save(output);

        // 🧪 你模組內的物品
        ManaGenFuelRecipeBuilder.create(ModItems.CORRUPTED_MANA_DUST.get(), 20, 15, 400).save(output);
        ManaGenFuelRecipeBuilder.create(ModItems.MANA_DUST.get(), 50, 25, 800).save(output);
        ManaGenFuelRecipeBuilder.create(ModItems.MANA_INGOT.get(), 450, 25, 4000).save(output);
    }
}
