package com.github.nalamodikk.common.register.component;

import com.github.nalamodikk.common.API.machine.IGridComponent;
import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Supplier;

public class ComponentRegistry {

    private static final Map<ResourceLocation, Supplier<IGridComponent>> JAVA_REGISTRY = new HashMap<>();

    public static void register(ResourceLocation id, Supplier<IGridComponent> constructor) {
        if (JAVA_REGISTRY.containsKey(id)) {
            MagicalIndustryMod.LOGGER.warn("⚠️ 重複註冊 Java 元件 ID：{}，將覆蓋", id);
        }
        JAVA_REGISTRY.put(id, constructor);
    }

    public static IGridComponent createComponent(ResourceLocation id) {
        Supplier<IGridComponent> supplier = JAVA_REGISTRY.get(id);
        if (supplier != null) {
            MagicalIndustryMod.LOGGER.debug("🔧 從 Java 建立元件：{}", id);
            return supplier.get();
        }

        MagicalIndustryMod.LOGGER.warn("❌ 找不到對應元件建構器：{}", id);
        return null;
    }

    public static Set<ResourceLocation> getAllComponentIds() {
        return Collections.unmodifiableSet(JAVA_REGISTRY.keySet());
    }

    public static boolean has(ResourceLocation id) {
        return JAVA_REGISTRY.containsKey(id);
    }

    /**
     * ⚠️ 不建議使用：用字串建立元件 ID，僅供少數需要字串處理的地方。
     */
    @Deprecated
    public static IGridComponent get(String idStr) {
        Supplier<IGridComponent> supplier = JAVA_REGISTRY.get(new ResourceLocation(idStr));
        return supplier != null ? supplier.get() : null;
    }

    private ComponentRegistry() {}
}
