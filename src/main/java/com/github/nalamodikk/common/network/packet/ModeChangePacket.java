// ✅ NeoForge 1.21+ 官方推薦封包系統：CustomPacketPayload + RegisterPayloadHandlerEvent + StreamCodec
// 專用於 modeIndex 模式切換封包

package com.github.nalamodikk.common.network.packet;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.item.ManaDebugToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// ModeChangePacket.java
public record ModeChangePacket(boolean forward) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ModeChangePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "mode_change"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModeChangePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    ModeChangePacket::forward,
                    ModeChangePacket::new
            );



    public static void handle(ModeChangePacket msg, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var stack = serverPlayer.getMainHandItem();
            if (stack.getItem() instanceof ManaDebugToolItem tool) {
                tool.cycleMode(stack, msg.forward());
            }
        }
    }


    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(MagicalIndustryMod.MOD_ID)
                .playToServer(TYPE, STREAM_CODEC, (msg, context) -> handle(msg, context.player()));
    }

    public static void sendToServer(boolean forward) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ModeChangePacket(forward));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE; // 由 ModPackets 提供 ID
    }

}

