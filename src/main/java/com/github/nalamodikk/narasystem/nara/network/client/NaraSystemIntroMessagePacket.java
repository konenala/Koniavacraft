package com.github.nalamodikk.narasystem.nara.network.client;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.narasystem.nara.message.NaraMessageRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record NaraSystemIntroMessagePacket() implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "nara_intro");
    public static final Type<NaraSystemIntroMessagePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "nara_intro"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NaraSystemIntroMessagePacket> STREAM_CODEC =
            StreamCodec.unit(new NaraSystemIntroMessagePacket());

    public static void handle(NaraSystemIntroMessagePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            NaraMessageRenderer.queue("message.magical_industry.nara.system_online");
            NaraMessageRenderer.queue("message.magical_industry.nara.stabilized");
            NaraMessageRenderer.queue("message.magical_industry.nara.welcome");
        });
    }
    // ✅ 正確範本
    public static void registerToServer(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, (packet, context) -> {});
    }


    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, NaraSystemIntroMessagePacket::handle);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
