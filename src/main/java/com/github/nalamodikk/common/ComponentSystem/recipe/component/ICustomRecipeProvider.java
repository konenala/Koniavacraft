package com.github.nalamodikk.common.ComponentSystem.recipe.component;

import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentGrid;

import java.util.List;

public interface ICustomRecipeProvider {
    List<AssemblyRecipe> generateRecipes(ComponentGrid grid);
}
