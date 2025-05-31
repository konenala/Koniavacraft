package com.github.nalamodikk.client.network.client;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.network.packet.server.manatool.ManaUpdatePacket;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class ManaUpdatePacketClient {
    public static void handle(ManaUpdatePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        var handler = CapabilityUtils.getMana(level, packet.pos(), null);
        if (handler != null) {
            handler.setMana(packet.mana());
        }
    }
}
