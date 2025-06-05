package com.github.nalamodikk.register.client;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.client.keyinput.ModKeyMappings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientInit {
    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        ModKeyMappings.registerKeyMappings(event);
    }

    // 如果還沒有這段就加進來
    @EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientRuntime {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            ModKeyMappings.onClientTick(event);
        }
    }
}
