package com.github.nalamodikk.common.platform;


import com.github.nalamodikk.common.network.packet.server.manatool.ManaUpdatePacket;

public interface PlatformClientHelper {
        void handleManaPacket(ManaUpdatePacket packet);
}
