package com.github.nalamodikk.common.API.block.mana;

import com.github.nalamodikk.common.block.mana_crafting.ManaCraftingTableRecipe;

import java.util.Optional;

public interface IManaCraftingMachine extends IManaUsingMachine{
    int getManaStored();
    boolean hasSufficientMana(int cost);
    void consumeMana(int cost);
    Optional<ManaCraftingTableRecipe> getCurrentRecipe();
    boolean hasRecipe();
}
