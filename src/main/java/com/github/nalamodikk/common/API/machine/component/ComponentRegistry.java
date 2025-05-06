package com.github.nalamodikk.common.API.machine.component;


import com.github.nalamodikk.common.API.IGridComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 模組零件註冊中心，負責根據模組 ID 字串還原對應的 IGridComponent 實例
 */
public class ComponentRegistry {

    // 🔸 用來儲存：模組ID → 建構器（new 的方法）
    private static final Map<String, Supplier<IGridComponent>> REGISTRY = new HashMap<>();

    /**
     * ✨ 將一個模組註冊到對應的 ID 上
     * @param id 這個模組的 ResourceLocation ID
     * @param supplier 一個建構器，用來 new 出這個模組
     */
    public static void register(ResourceLocation id, Supplier<IGridComponent> supplier) {
        REGISTRY.put(id.toString(), supplier);
    }

    /**
     * 🔍 根據模組 ID 還原出一個模組物件（讀 NBT 時用）
     * @param idStr 儲存在 NBT 裡的模組 ID 字串
     * @return 對應的模組（每次都 new 一個新的）
     */
    public static IGridComponent get(String idStr) {
        Supplier<IGridComponent> supplier = REGISTRY.get(idStr);
        if (supplier == null) return null;
        return supplier.get(); // 每次都 new 出新物件（避免共用狀態）
    }

    /**
     * 🔒 禁止建立這個類別的實例（因為這是工具類）
     */
    private ComponentRegistry() {}
}
