package com.github.nalamodikk.common.network.packet.server;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.network.packet.client.ManaUpdatePacketClient;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;


public record ManaUpdatePacket(BlockPos pos, int mana) implements CustomPacketPayload {

    public static final Type<ManaUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "mana_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManaUpdatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    CodecsLibrary.BLOCK_POS, ManaUpdatePacket::pos,
                    ByteBufCodecs.VAR_INT, ManaUpdatePacket::mana,
                    ManaUpdatePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ✅ 封包處理器
    public static void handle(ManaUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;

            IUnifiedManaHandler handler = CapabilityUtils.getMana(level, packet.pos(), null);
            if (handler != null) {
                handler.setMana(packet.mana());
            }
        });
    }


    // ✅ 封包註冊：交由 ModNetworking 調用
    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playBidirectional(
                TYPE,
                STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ManaUpdatePacketClient::handleOnClient, // 👈 客戶端處理方法
                        (packet, context) -> {}                 // 👈 伺服器忽略處理
                )
        );
    }



    // ✅ 伺服器端封包發送工具
    public static void sendManaUpdate(ServerPlayer player, BlockPos pos, int mana) {
        PacketDistributor.sendToPlayer(player, new ManaUpdatePacket(pos, mana));
    }
}
