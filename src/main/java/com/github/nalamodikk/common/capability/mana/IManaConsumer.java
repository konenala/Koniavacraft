package com.github.nalamodikk.common.capability.mana;

public interface IManaConsumer {
    int getRequestedMana();   // 當前需要多少魔力
    int receiveMana(int amount); // 實際收到多少魔力
}
