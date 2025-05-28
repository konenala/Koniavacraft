package com.github.nalamodikk.common.network.packet.manatool;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.item.tool.TechWandMode;
import com.github.nalamodikk.common.register.ModDataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record TechWandModePacket(TechWandMode mode) implements CustomPacketPayload {

    public static final Type<TechWandModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "tech_wand_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TechWandModePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(255)
                            .mapStream(buf -> buf)
                            .map(s -> Enum.valueOf(TechWandMode.class, s), TechWandMode::name),
                    TechWandModePacket::mode,
                    TechWandModePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TechWandModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack stack = player.getMainHandItem();
                if (!stack.isEmpty()) {
                    stack.set(ModDataComponents.TECH_WAND_MODE, packet.mode());
                }
            }
        });
    }


    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, TechWandModePacket::handle);
    }

    public static void sendToServer(TechWandMode mode) {
        PacketDistributor.sendToServer(new TechWandModePacket(mode));
    }
}
