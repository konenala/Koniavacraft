package com.github.nalamodikk.common.ComponentSystem.register.component;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IComponentBehavior;
import com.github.nalamodikk.common.ComponentSystem.API.machine.behavior.ManaProducerBehavior;
import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ComponentBehaviorRegistry {

    // 原始版本是儲存行為實例，我們改為儲存 "tag → 行為" 的工廠函式
    private static final Map<String, Function<CompoundTag, IComponentBehavior>> FACTORIES = new HashMap<>();

    public static void register(String id, Function<CompoundTag, IComponentBehavior> factory) {
        if (FACTORIES.containsKey(id)) {
            MagicalIndustryMod.LOGGER.warn("⚠️ 行為 ID '{}' 已被註冊，將覆蓋原本行為", id);
        }
        FACTORIES.put(id, factory);
    }

    public static IComponentBehavior create(String id, CompoundTag data) {
        Function<CompoundTag, IComponentBehavior> factory = FACTORIES.get(id);
        if (factory == null) throw new IllegalArgumentException("❌ 找不到行為工廠: " + id);
        return factory.apply(data);
    }







    private ComponentBehaviorRegistry() {}
}
