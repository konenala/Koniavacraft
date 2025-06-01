package com.github.nalamodikk.system.api.util;

import com.github.nalamodikk.system.api.INaraData;
import com.github.nalamodikk.system.data.NaraDataProvider;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public class NaraHelper {
    @Nullable
    public static INaraData get(Player player) {
        return player.getCapability(NaraDataProvider.NARA_DATA, null);
    }

    public static boolean isBound(Player player) {
        var data = get(player);
        return data != null && data.isBound();
    }

    public static void setBound(Player player, boolean bound) {
        var data = get(player);
        if (data != null) {
            data.setBound(bound);
        }
    }
}
