package com.github.nalamodikk.common.network;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.entity.ManaGenerator.ManaGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleModePacket {
    private final BlockPos pos;

    public ToggleModePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ToggleModePacket msg, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(msg.pos);
    }

    public static ToggleModePacket decode(FriendlyByteBuf buffer) {
        return new ToggleModePacket(buffer.readBlockPos());
    }

    public static void handle(ToggleModePacket msg, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                ServerLevel level = (ServerLevel) player.getCommandSenderWorld();

                BlockEntity blockEntity = level.getBlockEntity(msg.pos);
                if (blockEntity instanceof ManaGeneratorBlockEntity manaGenerator) {
                    // 🔥 防止繞過 GUI，伺服器端也檢查機器是否正在運作
                    if (manaGenerator.getBurnTime() > 0) {
                        MagicalIndustryMod.LOGGER.info("⚠ 無法切換模式，發電機正在運行中！（來自伺服器檢查）");
                        return;
                    }

                    manaGenerator.toggleMode(); // 切換模式
                    level.sendBlockUpdated(msg.pos, manaGenerator.getBlockState(), manaGenerator.getBlockState(), 3);
                }
            }
        });
        ctx.setPacketHandled(true);
    }







}
