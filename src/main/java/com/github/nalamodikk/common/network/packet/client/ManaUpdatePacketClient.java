package com.github.nalamodikk.common.network.packet.client;


import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.network.packet.server.ManaUpdatePacket;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public class ManaUpdatePacketClient {
    public static void handleOnClient(ManaUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null) return;

            IUnifiedManaHandler handler = CapabilityUtils.getMana(level, packet.pos(), null);
            if (handler != null) {
                handler.setMana(packet.mana());
            }
        });
    }
}
