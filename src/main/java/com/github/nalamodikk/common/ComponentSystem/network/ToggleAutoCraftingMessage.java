package com.github.nalamodikk.common.ComponentSystem.network;

import com.github.nalamodikk.common.ComponentSystem.API.machine.component.AutoCrafterComponent;
import com.github.nalamodikk.common.ComponentSystem.block.blockentity.MachineBlock.ModularMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleAutoCraftingMessage {
    private final BlockPos pos;

    public ToggleAutoCraftingMessage(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ToggleAutoCraftingMessage msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static ToggleAutoCraftingMessage decode(FriendlyByteBuf buf) {
        return new ToggleAutoCraftingMessage(buf.readBlockPos());
    }

    public static void handle(ToggleAutoCraftingMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                Level level = player.level();
                BlockEntity be = level.getBlockEntity(msg.pos);
                if (be instanceof ModularMachineBlockEntity machine) {
                    machine.getComponentGrid().forEachComponent(AutoCrafterComponent.class, (pos, comp) -> {
                        boolean now = comp.isGuiToggle();
                        comp.setGuiToggle(!now, machine.getComponentGrid());
                    });
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
