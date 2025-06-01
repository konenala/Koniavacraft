package com.github.nalamodikk.common.network.packet.client;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.network.packet.server.manatool.ManaUpdatePacket;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// ManaUpdatePacketClient.java
public class ManaUpdatePacketClient {
    private static ManaUpdatePacket packet;
    private static IPayloadContext context;

    public static void handle(ManaUpdatePacket packet, IPayloadContext context) {
        ManaUpdatePacketClient.packet = packet;
        ManaUpdatePacketClient.context = context;
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;

            IUnifiedManaHandler handler = CapabilityUtils.getMana(level, packet.pos(), null);
            if (handler != null) {
                handler.setMana(packet.mana());
            }
        });
    }
}
