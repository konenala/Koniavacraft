package com.github.nalamodikk.common.ComponentSystem.util.helpers;

import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class ModuleItemHelper {
    private static final Map<String, List<IGridComponent>> COMPONENT_CACHE = new HashMap<>();
    private static final Map<String, List<IGridComponent>> CLONED_COMPONENT_CACHE = new HashMap<>();
    private static final Map<ItemStack, String> STACK_SIGNATURE_CACHE = new WeakHashMap<>();
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, IGridComponent> CLONE_POOL = new HashMap<>();
    private static final Set<String> PRINTED_CLONE_KEYS = new HashSet<>();

    public static final String KEY_COMPONENT_ID = "component_id";
    public static final String KEY_COMPONENTS = "components";

    public static IGridComponent fastClone(IGridComponent original) {
        String key = original.getId().toString() + "#" + hashNBT(original.getData());
        IGridComponent cached = CLONE_POOL.get(key);

        if (cached == null) {
            IGridComponent newOne = ComponentRegistry.createComponent(original.getId());
            newOne.loadFromNBT(original.getData().copy());
            CLONE_POOL.put(key, newOne);
            return newOne;
        }
        if (PRINTED_CLONE_KEYS.add(key)) {
            LOGGER.debug("♻️ [ClonePool] hit: {} (source = {})", key, original.getClass().getSimpleName());
        }


        IGridComponent newClone = ComponentRegistry.createComponent(original.getId());
        newClone.loadFromNBT(cached.getData().copy());
        return newClone;
    }
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
        if (!stack.hasTag()) return List.of();

        CompoundTag tag = stack.getTag();
        if (!tag.contains("components")) return List.of();

        // [1] 取得原始 components list 與 data
        ListTag list = tag.getList("components", Tag.TAG_STRING);
        CompoundTag data = tag.contains("data") ? tag.getCompound("data") : new CompoundTag();

        // [2] 計算 signature key（更穩定）
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            String idStr = list.getString(i);
            IGridComponent c = ComponentRegistry.createComponent(new ResourceLocation(idStr));
            if (c != null) {
                c.loadFromNBT(data);
                sb.append(idStr).append("#").append(ModuleItemHelper.hashNBT(c.getData()));
            } else {
                sb.append(idStr).append("#null");
            }
            sb.append("|");
        }
        String cacheKey = sb.toString();

        LOGGER.debug("🧪 cacheKey = {}", cacheKey);
        if (COMPONENT_CACHE.containsKey(cacheKey)) {
            LOGGER.debug("✅ 命中 COMPONENT_CACHE");
        } else {
            LOGGER.warn("❌ 未命中 COMPONENT_CACHE");
        }

        // [3] 如果快取命中：記錄並 clone 回傳
        if (COMPONENT_CACHE.containsKey(cacheKey)) {
            STACK_SIGNATURE_CACHE.put(stack, cacheKey); // ✨ 對應記住這個 stack 對應的 key
            return cloneComponentList(COMPONENT_CACHE.get(cacheKey));
        }

        // [4] 否則：建立新的元件
        List<IGridComponent> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id == null) continue;

            IGridComponent component = ComponentRegistry.createComponent(id);
            if (component == null) continue;

            component.loadFromNBT(data);
            result.add(component);
        }

        // [5] 存入快取
        COMPONENT_CACHE.put(cacheKey, result);
        STACK_SIGNATURE_CACHE.put(stack, cacheKey);
        return cloneComponentList(result);
    }





    public static List<IGridComponent> getComponentsRaw(ItemStack stack) {
        if (!stack.hasTag()) return List.of();
        CompoundTag tag = stack.getTag();
        if (!tag.contains("components")) return List.of();

        ListTag list = tag.getList("components", Tag.TAG_STRING);
        CompoundTag data = tag.contains("data") ? tag.getCompound("data") : new CompoundTag();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            String idStr = list.getString(i);
            sb.append(idStr).append("@");

            IGridComponent c = ComponentRegistry.createComponent(new ResourceLocation(idStr));
            if (c != null) {
                c.loadFromNBT(data);
                sb.append(c.getData().toString().hashCode()); // ✅ 加強穩定性
            } else {
                sb.append("null");
            }

            sb.append("|");
        }




        List<IGridComponent> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id == null) continue;

            IGridComponent component = ComponentRegistry.createComponent(id);
            if (component == null) continue;

            component.loadFromNBT(data);
            result.add(component);
        }
        COMPONENT_CACHE.put(sb.toString(), result);

        return result;
    }



    private static List<IGridComponent> cloneComponentList(List<IGridComponent> original) {
        List<IGridComponent> copy = new ArrayList<>();
        for (IGridComponent c : original) {
            IGridComponent newOne = ModuleItemHelper.fastClone(c);
            if (newOne != null) {
                copy.add(newOne); // ✅ 加進回傳列表
            }
        }
        return copy;
    }




}
