package com.github.nalamodikk.common.ComponentSystem.register.component;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IComponentBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.behavior.ManaProducerBehavior;
import com.github.nalamodikk.common.MagicalIndustryMod;

import java.util.HashMap;
import java.util.Map;

public class ComponentBehaviorRegistry {

    private static final Map<String, IComponentBehavior> BEHAVIORS = new HashMap<>();

    public static void register(String id, IComponentBehavior behavior) {
        if (BEHAVIORS.containsKey(id)) {
            MagicalIndustryMod.LOGGER.warn("⚠️ 行為 ID '{}' 已被註冊，將覆蓋原本行為", id);
        }
        BEHAVIORS.put(id, behavior);
    }

    public static IComponentBehavior get(String id) {
        IComponentBehavior behavior = BEHAVIORS.get(id);
        if (behavior == null) {
            throw new IllegalArgumentException("❌ 找不到行為: " + id);
        }
        return behavior;
    }

    public static IComponentBehavior getOrNull(String id) {
        return BEHAVIORS.get(id);
    }

    public static boolean has(String id) {
        return BEHAVIORS.containsKey(id);
    }



    private ComponentBehaviorRegistry() {}
}
