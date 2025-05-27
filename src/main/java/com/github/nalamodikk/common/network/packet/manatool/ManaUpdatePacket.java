package com.github.nalamodikk.common.network.packet.manatool;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.utils.CodecUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record ManaUpdatePacket(BlockPos pos, int mana) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "mana_update");

    public static final Type<ManaUpdatePacket> TYPE = new Type<>(ID);


    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MagicalIndustryMod.MOD_ID);

        // ✅ 伺服器 → 客戶端：同步 mana 狀態
        registrar.playToClient(
                ManaUpdatePacket.TYPE,
                ManaUpdatePacket.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        BlockPos pos = packet.pos();
                        int mana = packet.mana();

                        // 可擴充為更新 UI、粒子、client side BE 等
                        ClientLevel level = Minecraft.getInstance().level;
                        if (level != null) {
                            BlockEntity be = level.getBlockEntity(pos);
                            System.out.println("[Client] 收到 Mana 封包: " + mana + " at " + pos);
                            // 可以改為：((IManaBlockEntity) be).getManaStorage().setMana(mana)
                        }
                    });
                }
        );

        // ⛔ 如果你有要接收客戶端→伺服器，也可以寫 playToServer(...)
        // registrar.playToServer(...)
    }

    public static void sendManaUpdate(ServerPlayer player, BlockPos pos, int mana) {
        PacketDistributor.sendToPlayer(player, new ManaUpdatePacket(pos, mana));
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ManaUpdatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    CodecUtils.BLOCK_POS, ManaUpdatePacket::pos,
                    ByteBufCodecs.VAR_INT, ManaUpdatePacket::mana,
                    ManaUpdatePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
