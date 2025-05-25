package com.github.nalamodikk.common.registry;

import com.github.nalamodikk.common.ComponentSystem.register.component.ModBehaviors;
import com.github.nalamodikk.common.ComponentSystem.register.component.ModComponents;

public class ModRegistries {
    public static void registerAll() {
        ModComponents.registerAll();
        ModBehaviors.registerAll();
        // 將來還可以加新的 Registry 類
    }
}
