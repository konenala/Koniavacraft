package com.github.nalamodikk.common.util.helpers;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public class ComponentNameHelper {

    /**
     * 根據 ItemStack 的 component_id NBT 轉換為可翻譯的名稱
     */
    public static Component getTranslatedComponentName(ItemStack stack, String translationRoot) {
        if (!stack.hasTag() || !stack.getTag().contains("component_id")) {
            return Component.translatable("item.magical_industry.module.unknown");
        }

        String idStr = stack.getTag().getString("component_id");
        ResourceLocation id = new ResourceLocation(idStr);

        // 你可以根據需求選擇是否保留命名空間
        String translationKey = translationRoot + "." + id.getNamespace() + "." + id.getPath();

        return Component.translatable(translationKey);
    }

    /**
     * 快捷方式，用於模組物品名稱顯示
     */
    public static Component getModuleDisplayName(ItemStack stack) {
        return getTranslatedComponentName(stack, "item.magical_industry.module");
    }
}
