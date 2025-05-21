package com.github.nalamodikk.group;

import com.github.nalamodikk.net.NetworkHandler;
import com.github.nalamodikk.net.PacketSpawnGroup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;

import java.util.UUID;

public class ServerParticleGroup {
    private final UUID groupId = UUID.randomUUID();
    private final ParticleGroup clientGroup;
    private final ServerLevel level;
    private final Player owner;

    private Vec3 center;
    private float rotation = 0;

    public ServerParticleGroup(ServerLevel level, Player owner, ParticleGroup clientGroup) {
        this.level = level;
        this.owner = owner;
        this.clientGroup = clientGroup;
        this.center = owner.position().add(0, 2, 0); // 例如：顯示在玩家頭頂
    }

    public void tick() {
        this.rotation += 5f; // 每 tick 旋轉

        clientGroup.setCenter(center);
        clientGroup.setRotation(rotation);

        // 傳給 owner（或所有 nearby player）
        NetworkHandler.CHANNEL.sendTo(
                new PacketSpawnGroup(groupId),
                ((ServerPlayer) owner).connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );


    }
}
