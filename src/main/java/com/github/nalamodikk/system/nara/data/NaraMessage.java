package com.github.nalamodikk.system.nara.data;

import net.minecraft.network.chat.Component;

public record NaraMessage(String key, int ticksVisible) {
    public Component asComponent() {
        return Component.translatable(key);
    }
}

