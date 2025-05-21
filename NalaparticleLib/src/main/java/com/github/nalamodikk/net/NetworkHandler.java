package com.github.nalamodikk.net;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class NetworkHandler {
    private static final String PROTOCOL = "1.0";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("nalaparticlelib", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++,
                PacketSpawnGroup.class,
                PacketSpawnGroup::encode,
                PacketSpawnGroup::decode,
                PacketSpawnGroup::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }
}
