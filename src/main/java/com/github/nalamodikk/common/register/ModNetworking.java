package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.packet.server.OpenUpgradeGuiPacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ToggleModePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ManaUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ModeChangePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.TechWandModePacket;
import com.github.nalamodikk.system.nara.network.NaraBindRequestPacket;
import com.github.nalamodikk.system.nara.network.NaraSyncPacket;
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
        ConfigDirectionUpdatePacket.registerTo(registrar);
        ToggleModePacket.registerTo(registrar);
        OpenUpgradeGuiPacket.registerTo(registrar);

        // 娜拉系統
        NaraBindRequestPacket.registerTo(registrar);
        NaraSyncPacket.registerTo(registrar);

    }

}
