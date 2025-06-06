package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.server.OpenUpgradeGuiPacket;
import com.github.nalamodikk.common.network.packet.server.manatool.*;
import com.github.nalamodikk.narasystem.nara.network.server.NaraBindRequestPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {
    public static final String VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        TechWandModePacket.registerTo(registrar);
        ModeChangePacket.registerTo(registrar);
        ConfigDirectionUpdatePacket.registerTo(registrar);
        ToggleModePacket.registerTo(registrar);
        OpenUpgradeGuiPacket.registerTo(registrar);

        // 娜拉系統
        NaraBindRequestPacket.registerTo(registrar);

    }

}
