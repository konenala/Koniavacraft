package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;


@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ManaCapability {
    public static final BlockCapability<IUnifiedManaHandler, Void> MANA =
            BlockCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath("magical_industry", "mana"),
                    IUnifiedManaHandler.class
            );
}

