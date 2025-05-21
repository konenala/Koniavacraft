package com.github.nalamodikk.net;

import com.github.nalamodikk.group.ParticleGroup;
import com.github.nalamodikk.group.ParticleGroupProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketSpawnGroupPosRot {
    private final UUID groupId;
    private final Vec3 position;
    private final float rotation;

    public PacketSpawnGroupPosRot(UUID groupId, Vec3 position, float rotation) {
        this.groupId = groupId;
        this.position = position;
        this.rotation = rotation;
    }

    public static void encode(PacketSpawnGroupPosRot msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.groupId);
        buf.writeDouble(msg.position.x);
        buf.writeDouble(msg.position.y);
        buf.writeDouble(msg.position.z);
        buf.writeFloat(msg.rotation);
    }

    public static PacketSpawnGroupPosRot decode(FriendlyByteBuf buf) {
        UUID groupId = buf.readUUID();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        float rot = buf.readFloat();
        return new PacketSpawnGroupPosRot(groupId, pos, rot);
    }

    public static void handle(PacketSpawnGroupPosRot msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                ParticleGroup group = ParticleGroupProvider.create(msg.groupId);
                if (group != null) {
                    group.setCenter(msg.position);
                    group.setRotation(msg.rotation);
                    group.spawn(level);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // Server 端幫你呼叫的廣播方法
    public static void sendToNearby(ServerPlayer player, UUID groupId, Vec3 pos, float rot) {
        PacketSpawnGroupPosRot packet = new PacketSpawnGroupPosRot(groupId, pos, rot);
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), packet);
    }
}
