package com.github.nalamodikk.common.network;

import com.github.nalamodikk.common.screen.UpgradeMenu;
import com.github.nalamodikk.common.upgrade.api.IUpgradeableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class OpenUpgradeGuiPacket {
    private final BlockPos pos;

    public OpenUpgradeGuiPacket(BlockPos pos) {
        this.pos = pos;
    }

    public OpenUpgradeGuiPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Level level = player.level();
            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof IUpgradeableMachine machine) {
                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (id, inv, p) -> new UpgradeMenu(id, inv, machine.getUpgradeInventory(), machine),
                        Component.translatable("screen.magical_industry.upgrade.title")
                ), buf -> buf.writeBlockPos(pos));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
