package com.github.nalamodikk.common.util.helpers;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentRegistry;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleItemHelper {
    private static final Map<String, List<IGridComponent>> COMPONENT_CACHE = new HashMap<>();

    public static final String KEY_COMPONENT_ID = "component_id";
    public static final String KEY_COMPONENTS = "components";

    /**
     * 將老格式模組（僅含 component_id）轉換為 components[] + data 結構。
     * 可重複呼叫，若已是新格式則不會重複轉換。
     */
    public static void normalizeStackNBT(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();

        // 已有 components[] 就略過
        // 無 component_id → 轉換自 components[] 第 0 個
        if (!tag.contains(KEY_COMPONENT_ID) && tag.contains(KEY_COMPONENTS)) {
            ListTag list = tag.getList(KEY_COMPONENTS, Tag.TAG_STRING);
            if (!list.isEmpty()) {
                String firstId = list.getString(0);
                tag.putString(KEY_COMPONENT_ID, firstId); // ✅ 自動補上主視覺用 component_id
            }
        }

        // 無 component_id → 無法處理
        if (!tag.contains(KEY_COMPONENT_ID)) return;

        String idStr = tag.getString(KEY_COMPONENT_ID);
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        if (id == null) return;

        IGridComponent component = ComponentRegistry.createComponent(id);
        if (component == null) return;

        // 寫入 components[]（單一元素）
        ListTag list = new ListTag();
        list.add(StringTag.valueOf(id.toString()));
        tag.put(KEY_COMPONENTS, list);

        // 嘗試取得元件行為資料存成 data（僅限支援 getData 的元件）
        CompoundTag data = component.getData(); // 包含 mana / behavior
        if (!data.isEmpty()) {
            tag.put("data", data.copy());
        }
    }

    public static String hashNBT(CompoundTag tag) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = tag.toString().getBytes(StandardCharsets.UTF_8);
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString(); // 回傳一個安全的 64 字元 key
        } catch (Exception e) {
            return String.valueOf(tag.toString().hashCode()); // fallback
        }
    }

    public static List<IGridComponent> getComponents(ItemStack stack) {
        return cloneComponentList(getComponentsRaw(stack));
    }

    public static List<IGridComponent> getComponentsRaw(ItemStack stack) {
        if (!stack.hasTag()) return List.of();
        CompoundTag tag = stack.getTag();
        if (!tag.contains("components")) return List.of();

        StringBuilder sb = new StringBuilder();
        ListTag list = tag.getList("components", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.getString(i)).append("|");
        }
        if (tag.contains("data")) {
            sb.append(hashNBT(tag.getCompound("data")));
        }

        String cacheKey = sb.toString();

        if (COMPONENT_CACHE.containsKey(cacheKey)) {
            return COMPONENT_CACHE.get(cacheKey);
        }

        List<IGridComponent> result = new ArrayList<>();
        CompoundTag data = tag.getCompound("data");

        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id == null) continue;

            IGridComponent component = ComponentRegistry.createComponent(id);
            if (component == null) continue;

            component.loadFromNBT(data);
            result.add(component);
        }

        COMPONENT_CACHE.put(cacheKey, result);
        return result;
    }


    private static List<IGridComponent> cloneComponentList(List<IGridComponent> original) {
        List<IGridComponent> copy = new ArrayList<>();
        for (IGridComponent c : original) {
            IGridComponent newOne = ComponentRegistry.createComponent(c.getId());
            if (newOne != null) {
                newOne.loadFromNBT(c.getData().copy());
                copy.add(newOne);
            }
        }
        return copy;
    }



}
