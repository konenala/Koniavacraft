
/**
 * 📦 ConfigDirectionUpdatePacket
 *
 * 用於同步 UniversalConfigScreen 中每個方向的 I/O 設定到伺服器端。
 * 搭配 PacketDistributor.sendToServer(...) 使用。
 * 記得：這個封包會自動透過 type() 推導註冊資訊，無需額外傳 TYPE。
 */

package com.github.nalamodikk.common.network.packet.server;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.register.ModDataComponents;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import com.github.nalamodikk.common.utils.item.ItemStackUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.EnumMap;

public record ConfigDirectionUpdatePacket(BlockPos pos, Direction direction, IOHandlerUtils.IOType ioType)
        implements CustomPacketPayload {

    public static final Type<ConfigDirectionUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "config_direction_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigDirectionUpdatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    CodecsLibrary.BLOCK_POS, ConfigDirectionUpdatePacket::pos,
                    CodecsLibrary.DIRECTION, ConfigDirectionUpdatePacket::direction,
                    CodecsLibrary.STREAM_CODEC, ConfigDirectionUpdatePacket::ioType,
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
                    // ✅ 若內容沒變，就直接略過處理（防止多次封包）
                    if (configurable.getIOConfig(packet.direction()) == packet.ioType()) {
                        if (MagicalIndustryMod.IS_DEV) {
                            MagicalIndustryMod.LOGGER.info("[Server] ⏩ Skipped duplicate config for {} → {}", packet.direction(),packet.ioType());
                        }
                        return;
                    }

                    configurable.setIOConfig(packet.direction(), packet.ioType());
                    be.setChanged();
                    level.sendBlockUpdated(packet.pos(), be.getBlockState(), be.getBlockState(), 3); // 🟢 客戶端同步畫面

                    // ✅ 同步更新玩家手上的魔杖儲存設定（DataComponent）
                    ItemStack wand = ItemStackUtils.findHeldWand(player);
                    if (!wand.isEmpty()) {
                        EnumMap<Direction, Boolean> updatedConfig = new EnumMap<>(Direction.class);
                        for (Direction dir : Direction.values()) {
                            updatedConfig.put(dir, configurable.isOutput(dir));
                        }

                        // ✅ 若 wand 上的值已經相同，也不需要重複寫入
                        EnumMap<Direction, Boolean> existing = wand.get(ModDataComponents.CONFIGURED_DIRECTIONS);
                        if (!updatedConfig.equals(existing)) {
                            wand.set(ModDataComponents.CONFIGURED_DIRECTIONS, updatedConfig);

                            if (MagicalIndustryMod.IS_DEV) {
                                MagicalIndustryMod.LOGGER.info("[Server] 🔁 Updated wand config: {}", updatedConfig);
                            }
                        } else {
                            if (MagicalIndustryMod.IS_DEV) {
                                MagicalIndustryMod.LOGGER.info("[Server] ⏩ Skipped wand update (already up-to-date)");
                            }
                        }
                    }
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
