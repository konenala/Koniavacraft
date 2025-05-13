package com.github.nalamodikk.common.item;

import com.github.nalamodikk.client.renderer.item.DynamicModuleItemRenderer;
import com.github.nalamodikk.common.ComponentSystem.API.machine.component.ManaStorageComponent;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.register.ModItems;
import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentRegistry;
import com.github.nalamodikk.common.util.helpers.ComponentNameHelper;
import com.github.nalamodikk.common.util.helpers.ModuleItemHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
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
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ModuleItem extends Item {

    public static final String KEY_COMPONENT_ID = "component_id";
    public static final String KEY_COMPONENTS = "components";
    private static final Map<CompoundTag, List<IGridComponent>> COMPONENT_CACHE = new HashMap<>();

    public ModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new DynamicModuleItemRenderer();
            }
        });
    }

    @Nonnull
    public static List<IGridComponent> getComponents(ItemStack stack) {
        return ModuleItemHelper.getComponents(stack); // ✅ 呼叫真正快取邏輯
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
        List<IGridComponent> list = ModuleItemHelper.getComponents(stack);
        return list.isEmpty() ? null : list.get(0); // ✅ 使用快取並 clone
    }



    public static ItemStack createModuleItem(ResourceLocation... ids) {
        ItemStack stack = new ItemStack(ModItems.MODULE_ITEM.get());
        ListTag list = new ListTag();
        for (ResourceLocation id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }

        stack.getOrCreateTag().put(KEY_COMPONENTS, list);
        ModuleItemHelper.normalizeStackNBT(stack);

        return stack;
    }

}
