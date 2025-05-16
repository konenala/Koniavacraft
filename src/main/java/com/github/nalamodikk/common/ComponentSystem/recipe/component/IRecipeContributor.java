package com.github.nalamodikk.common.ComponentSystem.recipe.component;

import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.ComponentSystem.recipe.component.AssemblyRecipe;

import java.util.List;

public interface IRecipeContributor {
    List<AssemblyRecipe> getLocalRecipes(ComponentGrid grid);
}
