package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.register.component.ComponentBehaviorRegistry;
import com.github.nalamodikk.common.register.component.ModComponents;

public class ModRegistries {
    public static void registerAll() {
        ModComponents.registerAll();
        ComponentBehaviorRegistry.registerAll();
        // 將來還可以加新的 Registry 類
    }
}
