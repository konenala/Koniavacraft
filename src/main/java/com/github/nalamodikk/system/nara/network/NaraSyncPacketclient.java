package com.github.nalamodikk.system.nara.network;

import com.github.nalamodikk.common.register.ModCapabilities;
import com.github.nalamodikk.system.nara.util.NaraHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NaraSyncPacketclient {
    public static void handleNaraSync(NaraSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            NaraHelper.setBound(player, packet.isBound());

        }).exceptionally(e -> {
            context.disconnect(Component.translatable("message.magical_industry.nara.sync_error", e.getMessage()));
            return null;
        });
    }

}
