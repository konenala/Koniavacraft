package com.github.nalamodikk.common.item;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.API.machine.IGridComponent;
import com.github.nalamodikk.common.register.ModItems;
import com.github.nalamodikk.common.register.component.ComponentRegistry;
import com.github.nalamodikk.common.util.helpers.ComponentNameHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ModuleItem extends Item {

    public static final String KEY_COMPONENT_ID = "component_id";
    public static final String KEY_COMPONENTS = "components";

    public ModuleItem(Properties properties) {
        super(properties);
    }

    @Nonnull
    public static List<IGridComponent> getComponents(ItemStack stack) {
        List<IGridComponent> components = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(KEY_COMPONENTS)) return components;

        ListTag list = tag.getList(KEY_COMPONENTS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                IGridComponent component = ComponentRegistry.createComponent(id);
                if (component != null) components.add(component);
            }
        }

        return components;
    }

    @Override
    public Component getName(ItemStack stack) {

        return ((MutableComponent) ComponentNameHelper.getModuleDisplayName(stack)).withStyle(ChatFormatting.WHITE);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(((MutableComponent) ComponentNameHelper.getModuleDisplayName(stack)).withStyle(ChatFormatting.WHITE));

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("mana_per_tick")) {
            tooltip.add(Component.translatable("tooltip.magical_industry.module.mana", tag.getInt("mana_per_tick")));
        }
    }



    @Nullable
    public static ResourceLocation getComponentId(ItemStack stack) {
            if (stack == null || !stack.hasTag() || !stack.getTag().contains(KEY_COMPONENT_ID)) {
                return null;
        }

        String idStr = stack.getTag().getString(KEY_COMPONENT_ID);
        try {
            return ResourceLocation.tryParse(idStr);
        } catch (Exception e) {
            MagicalIndustryMod.LOGGER.warn("❌ 無法解析 component_id: {}", idStr, e);
            return null;
        }
    }

    @Nullable
    public static IGridComponent getComponentFromItem(ItemStack stack) {
        ResourceLocation componentId = getComponentId(stack);
        if (componentId == null) return null;

        IGridComponent component = ComponentRegistry.createComponent(componentId);
        if (component == null) {
            MagicalIndustryMod.LOGGER.warn("⚠️ 無法從 ModuleItem 找到元件建構器: {}", componentId);
        }

        return component;
    }


    public static ItemStack createModuleItem(ResourceLocation... ids) {
        ItemStack stack = new ItemStack(ModItems.MODULE_ITEM.get());
        ListTag list = new ListTag();
        for (ResourceLocation id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        stack.getOrCreateTag().put(KEY_COMPONENTS, list);
        return stack;
    }

}
