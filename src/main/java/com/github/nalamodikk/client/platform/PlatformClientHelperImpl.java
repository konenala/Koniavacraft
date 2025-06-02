package com.github.nalamodikk.client.platform;

import com.github.nalamodikk.common.network.packet.server.manatool.ManaUpdatePacket;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlatformClientHelperImpl implements PlatformClientHelper {
    @Override
    public void handleManaPacket(ManaUpdatePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        BlockPos pos = packet.pos();
        IUnifiedManaHandler handler = CapabilityUtils.getMana(level, pos, null);
        if (handler != null) {
            handler.setMana(packet.mana());
        }
    }
}
