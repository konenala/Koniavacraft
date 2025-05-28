package com.github.nalamodikk.common.register.handler;
// 📡 封包註冊與發送工具 - 集中管理所有 CustomPacketPayload

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.packet.manatool.ModeChangePacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;


@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class RegisterNetworkHandler {

}
