package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.item.ManaDebugToolItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModDataComponents {


    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.DATA_COMPONENT_TYPE, helper -> {
            helper.register(ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "mode_index"), ManaDebugToolItem.MODE_INDEX);
        });
    }
}