package com.github.nalamodikk.common.API;

import com.github.nalamodikk.common.recipe.ManaCraftingTableRecipe;

import java.util.Optional;

public interface IManaCraftingMachine {
    int getManaStored();
    boolean hasSufficientMana(int cost);
    void consumeMana(int cost);
    Optional<ManaCraftingTableRecipe> getCurrentRecipe();
    boolean hasRecipe();
}
