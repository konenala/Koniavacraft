package com.github.nalamodikk.common.network.packet.server.manatool;

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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
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


    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ManaUpdatePacketClient.handle(packet, context))
        );
    }


    // ✅ 封包處理器


    // ✅ 封包註冊：交由 ModNetworking 調用


    public static void registerTo(PayloadRegistrar registrar) {
    }

    // ✅ 伺服器端封包發送工具
    public static void sendManaUpdate(ServerPlayer player, BlockPos pos, int mana) {
        PacketDistributor.sendToPlayer(player, new ManaUpdatePacket(pos, mana));
    }
}
