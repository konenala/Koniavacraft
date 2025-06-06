package com.github.nalamodikk.common.API.block.mana;

public interface IManaUsingMachine {
    int getManaStored();
    boolean hasSufficientMana(int cost);
    void consumeMana(int cost);
}
