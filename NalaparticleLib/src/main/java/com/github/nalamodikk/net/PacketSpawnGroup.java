package com.github.nalamodikk.net;

import com.github.nalamodikk.group.ParticleGroup;
import com.github.nalamodikk.group.ParticleGroupProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketSpawnGroup {
    private final UUID groupId;

    public PacketSpawnGroup(UUID groupId) {
        this.groupId = groupId;
    }

    public static void encode(PacketSpawnGroup msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.groupId);
    }

    public static PacketSpawnGroup decode(FriendlyByteBuf buf) {
        return new PacketSpawnGroup(buf.readUUID());
    }

    public static void handle(PacketSpawnGroup msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                ParticleGroup group = ParticleGroupProvider.create(msg.groupId);
                if (group != null) {
                    group.spawn(mc.level);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
