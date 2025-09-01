package com.github.nalamodikk.common.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 🌤️ 天空檢測工具類 - 使用高度圖的極速穩定方案
 *
 * 💡 設計理念：
 * - 🚀 利用 Minecraft 內建的 Heightmap.Types.MOTION_BLOCKING
 * - ⚡ 比逐格掃描快 100+ 倍
 * - 🎯 對光照更新不敏感，極其穩定
 * - 📊 高度圖是整柱抽象，實務上完全夠用
 */
public final class SkyUtils {

    /**
     * 🔍 使用高度圖檢測方塊是否能見天空
     *
     * @param level 伺服器世界
     * @param pos 要檢測的方塊位置
     * @return true 如果該位置能見天空
     */
    public static boolean isOpenToSkyByHeightmap(ServerLevel level, BlockPos pos) {
        // MOTION_BLOCKING: 第一個會阻擋運動的方塊高度（包括阻擋天空光的方塊）
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());

        // 如果我們的方塊 Y >= 阻擋高度 - 1，就算見天
        return pos.getY() >= h - 1;
    }
}
