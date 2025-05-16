package com.github.nalamodikk.common.ComponentSystem.register.component;

import com.github.nalamodikk.common.ComponentSystem.API.machine.behavior.CraftingBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.behavior.ManaProducerBehavior;
import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentBehaviorRegistry;

public class ModBehaviors {
        public static void registerAll() {
            ComponentBehaviorRegistry.register("mana_producer", tag -> {
                ManaProducerBehavior behavior = new ManaProducerBehavior();
                behavior.init(tag);
                return behavior;
            });

            ComponentBehaviorRegistry.register("crafting", tag -> {
                CraftingBehavior behavior = new CraftingBehavior();
                behavior.init(tag);
                return behavior;
            });

        }

        // 未來你可以註冊更多行為在這裡
    }

