package com.github.nalamodikk.system.nara.util;

import com.github.nalamodikk.common.register.ModDataAttachments;
import com.github.nalamodikk.common.register.ModDataComponents;
import net.minecraft.world.entity.player.Player;


public class NaraHelper {
    public static boolean isBound(Player player) {
        return player.getData(ModDataAttachments.NARA_BOUND); // AttachmentType<T>
    }

    public static void setBound(Player player, boolean value) {
        player.setData(ModDataAttachments.NARA_BOUND, value);
    }

    public static void clearBound(Player player) {
        player.removeData(ModDataAttachments.NARA_BOUND);
    }
}


