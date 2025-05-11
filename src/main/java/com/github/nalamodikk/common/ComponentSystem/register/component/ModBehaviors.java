package com.github.nalamodikk.common.ComponentSystem.register.component;

import com.github.nalamodikk.common.ComponentSystem.API.machine.behavior.ManaProducerBehavior;
import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentBehaviorRegistry;

public class ModBehaviors {
    public static void registerAll() {
        ComponentBehaviorRegistry.register("mana_producer", new ManaProducerBehavior());
        // 未來你可以註冊更多行為在這裡
        // ComponentBehaviorRegistry.register("cooling", new CoolingBehavior());
    }
}
