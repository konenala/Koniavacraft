package com.github.nalamodikk.system.nara.network;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.register.ModCapabilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NaraSyncPacket(boolean isBound) implements CustomPacketPayload {
    public static final Type<NaraSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "nara_sync"));

    public static final StreamCodec<FriendlyByteBuf, NaraSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, NaraSyncPacket::isBound,
                    NaraSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
