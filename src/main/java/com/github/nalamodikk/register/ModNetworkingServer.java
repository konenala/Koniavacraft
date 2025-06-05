package com.github.nalamodikk.register;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.common.network.packet.server.manatool.*;
import com.github.nalamodikk.narasystem.nara.network.client.NaraSystemIntroMessagePacket;
import com.github.nalamodikk.narasystem.nara.network.client.OpenNaraInitScreenPacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraSyncPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID ,value = Dist.DEDICATED_SERVER, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworkingServer {
    public static final String VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        ManaUpdatePacket.registerToServer(registrar);
        NaraSystemIntroMessagePacket.registerToServer(registrar);
        NaraSyncPacket.registerToServer(registrar);
        OpenNaraInitScreenPacket.registerToServer(registrar);

    }

}
