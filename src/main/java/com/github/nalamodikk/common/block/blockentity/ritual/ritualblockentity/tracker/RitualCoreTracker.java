package com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.tracker;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 儀式核心註冊器：提供快速查詢世界中的儀式核心位置。
 */
public final class RitualCoreTracker {

    private static final Map<ResourceKey<Level>, Set<BlockPos>> CORE_POSITIONS = new ConcurrentHashMap<>();

    private RitualCoreTracker() {
    }

    /**
     * 記錄伺服端的儀式核心位置。
     */
    public static void register(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        CORE_POSITIONS
                .computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet())
                .add(pos.immutable());
    }

    /**
     * 移除已不存在的儀式核心位置。
     */
    public static void unregister(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        CORE_POSITIONS.computeIfPresent(level.dimension(), (key, set) -> {
            set.remove(pos);
            return set.isEmpty() ? null : set;
        });
    }

    /**
     * 取得指定世界中已註冊的核心座標集合。
     */
    public static Set<BlockPos> getCores(Level level) {
        return CORE_POSITIONS.getOrDefault(level.dimension(), Collections.emptySet());
    }
}
