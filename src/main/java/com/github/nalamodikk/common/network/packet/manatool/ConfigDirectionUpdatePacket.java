
/**
 * 📦 ConfigDirectionUpdatePacket
 *
 * 用於同步 UniversalConfigScreen 中每個方向的 I/O 設定到伺服器端。
 * 搭配 PacketDistributor.sendToServer(...) 使用。
 * 記得：這個封包會自動透過 type() 推導註冊資訊，無需額外傳 TYPE。
 */

package com.github.nalamodikk.common.network.packet.manatool;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.utils.CodecsLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record ConfigDirectionUpdatePacket(BlockPos pos, Direction direction, boolean isOutput)
        implements CustomPacketPayload {

    public static final Type<ConfigDirectionUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "config_direction_update"));



    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigDirectionUpdatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    CodecsLibrary.BLOCK_POS, ConfigDirectionUpdatePacket::pos,
                    CodecsLibrary.DIRECTION, ConfigDirectionUpdatePacket::direction,
                    ByteBufCodecs.BOOL, ConfigDirectionUpdatePacket::isOutput,
                    ConfigDirectionUpdatePacket::new
            );



    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigDirectionUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Level level = player.level();
                BlockEntity be = level.getBlockEntity(packet.pos());
                if (be instanceof IConfigurableBlock configurable) {
                    configurable.setDirectionConfig(packet.direction(), packet.isOutput());
                    be.setChanged();
                }
            }
        });
    }

    public static void registerTo(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, ConfigDirectionUpdatePacket::handle);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(MagicalIndustryMod.MOD_ID)
                .playToServer(TYPE, STREAM_CODEC, ConfigDirectionUpdatePacket::handle);
    }
}
