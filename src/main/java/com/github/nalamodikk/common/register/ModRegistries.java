package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentBehaviorRegistry;
import com.github.nalamodikk.common.ComponentSystem.register.component.ModBehaviors;
import com.github.nalamodikk.common.ComponentSystem.register.component.ModComponents;

public class ModRegistries {
    public static void registerAll() {
        ModComponents.registerAll();
        ModBehaviors.registerAll();
        // 將來還可以加新的 Registry 類
    }
}
