package com.github.nalamodikk.system.nara.network;

import com.github.nalamodikk.common.register.ModCapabilities;
import com.github.nalamodikk.system.nara.api.INaraData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NaraSyncPacketclient {
    public static void handleNaraSync(NaraSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            INaraData data = player.getCapability(ModCapabilities.NARA_DATA, null);
            if (data != null) {
                data.setBound(packet.isBound());
            }

        }).exceptionally(e -> {
            context.disconnect(Component.translatable("message.magical_industry.nara.sync_error", e.getMessage()));
            return null;
        });
    }
}
