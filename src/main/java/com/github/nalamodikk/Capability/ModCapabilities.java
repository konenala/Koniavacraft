package com.github.nalamodikk.Capability;

import com.github.nalamodikk.MagicalIndustryMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCapabilities {
    // 声明 Capability 实例
    public static final Capability<IMana> MANA = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        // 注册 IMana 能力
        event.register(IMana.class);
    }
}
