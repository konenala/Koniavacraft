package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.packet.manatool.ManaUpdatePacket;
import com.github.nalamodikk.common.network.packet.manatool.ModeChangePacket;
import com.github.nalamodikk.common.network.packet.manatool.TechWandModePacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {
    public static final String VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        TechWandModePacket.registerTo(registrar);
        ModeChangePacket.registerTo(registrar);
        ManaUpdatePacket.registerTo(registrar);
    }

}
