package com.github.nalamodikk.common.API;

public interface IManaUsingMachine {
    int getManaStored();
    boolean hasSufficientMana(int cost);
    void consumeMana(int cost);
}
