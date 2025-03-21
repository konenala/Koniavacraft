package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.recipe.fuel.FuelRecipeBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

public class FuelRecipeProvider {


    public static void buildFuelRecipes(Consumer<FinishedRecipe> writer) {
        FuelRecipeBuilder.create(MagicalIndustryMod.MOD_ID, "corrupted_mana_dust", 200, 0, 300, "fuel_corrupted_mana_dust")
                .save(writer);

        FuelRecipeBuilder.create(MagicalIndustryMod.MOD_ID, "mana_dust", 400, 0, 500, "fuel_mana_dust")
                .save(writer);

        FuelRecipeBuilder.create(MagicalIndustryMod.MOD_ID, "mana_ingot", 800, 0, 1000, "fuel_mana_ingot")
                .save(writer);
    }

}
