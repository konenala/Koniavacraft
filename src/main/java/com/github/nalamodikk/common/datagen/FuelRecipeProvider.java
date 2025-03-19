package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.common.recipe.fuel.FuelRecipeBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

public class FuelRecipeProvider {


    public static void buildFuelRecipes(Consumer<FinishedRecipe> writer) {
        FuelRecipeBuilder.create("magical_industry", "corrupted_mana_dust", 200, 100, 300, "fuel_corrupted_mana_dust")
                .save(writer);

        FuelRecipeBuilder.create("magical_industry", "mana_dust", 400, 100, 500, "fuel_mana_dust")
                .save(writer);

        FuelRecipeBuilder.create("magical_industry", "mana_ingot", 800, 100, 1000, "fuel_mana_ingot")
                .save(writer);
    }

}
