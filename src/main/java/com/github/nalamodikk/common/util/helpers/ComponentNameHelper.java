package com.github.nalamodikk.common.util.helpers;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public class ComponentNameHelper {

    /**
     * 根據 ItemStack 的 component_id NBT 轉換為可翻譯的名稱
     */
    public static Component getTranslatedComponentName(ItemStack stack, String translationRoot) {
        if (!stack.hasTag()) {
            return Component.translatable("item.magical_industry.module.unknown");
        }

        String idStr = null;
        CompoundTag tag = stack.getTag();

        // ✅ 優先抓 component_id
        if (tag.contains("component_id")) {
            idStr = tag.getString("component_id");
        }
        // ✅ fallback: 抓 components[0]
        else if (tag.contains("components")) {
            ListTag list = tag.getList("components", Tag.TAG_STRING);
            if (!list.isEmpty()) {
                idStr = list.getString(0);
            }
        }

        if (idStr == null) {
            return Component.translatable("item.magical_industry.module.unknown");
        }

        ResourceLocation id = ResourceLocation.tryParse(idStr);
        if (id == null) {
            return Component.translatable("item.magical_industry.module.unknown");
        }

        return Component.translatable(translationRoot + "." + id.getNamespace() + "." + id.getPath());
    }


    /**
     * 快捷方式，用於模組物品名稱顯示
     */
    public static Component getModuleDisplayName(ItemStack stack) {
        return getTranslatedComponentName(stack, "item.magical_industry.module");
    }
}
