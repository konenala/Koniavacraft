// 🔧 通用方塊替換工具類
package com.github.nalamodikk.common.utils.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * 🔧 通用方塊替換工具
 *
 * 用法超簡單：
 * BlockReplacementUtils.replace(level, chunk, ModBiomes.MANA_PLAINS, rules);
 */
public class BlockReplacementUtils {

    /**
     * 🎯 核心方法：替換指定生物群系的方塊
     *
     * @param level 世界
     * @param chunk 區塊
     * @param targetBiome 目標生物群系
     * @param replacements 替換規則
     * @return 替換的方塊數量
     */
    public static int replace(ServerLevel level, ChunkAccess chunk,
                              ResourceKey<Biome> targetBiome, Map<Block, Block> replacements) {
        return replace(level, chunk, targetBiome, replacements, Map.of());
    }

    /**
     * 🎯 核心方法：替換指定生物群系的方塊（帶條件）
     */
    public static int replace(ServerLevel level, ChunkAccess chunk,
                              ResourceKey<Biome> targetBiome,
                              Map<Block, Block> replacements,
                              Map<Block, BiPredicate<ChunkAccess, BlockPos>> conditions) {

        // 🔍 檢查是否為目標生物群系
        if (!isBiome(level, chunk, targetBiome)) {
            return 0;
        }

        // 🔄 執行替換
        return performReplacement(level, chunk, replacements, conditions);
    }

    /**
     * 🌟 便捷方法：快速創建替換規則
     */
    public static Map<Block, Block> rules(Block source, Block target) {
        return Map.of(source, target);
    }

    public static Map<Block, Block> rules(Block s1, Block t1, Block s2, Block t2) {
        return Map.of(s1, t1, s2, t2);
    }

    public static Map<Block, Block> rules(Block s1, Block t1, Block s2, Block t2, Block s3, Block t3) {
        return Map.of(s1, t1, s2, t2, s3, t3);
    }

    /**
     * 🌟 便捷方法：創建條件規則
     */
    public static Map<Block, BiPredicate<ChunkAccess, BlockPos>> conditions(
            Block source, BiPredicate<ChunkAccess, BlockPos> condition) {
        return Map.of(source, condition);
    }

    /**
     * 🏗️ 建造者模式（可選，提供更複雜的配置）
     */
    public static ReplacementBuilder builder() {
        return new ReplacementBuilder();
    }

    // === 內部實現 ===

    /**
     * 🔍 檢查區塊是否為指定生物群系
     */
    private static boolean isBiome(ServerLevel level, ChunkAccess chunk, ResourceKey<Biome> targetBiome) {
        BlockPos centerPos = new BlockPos(
                chunk.getPos().x * 16 + 8,
                level.getSeaLevel(),
                chunk.getPos().z * 16 + 8
        );

        return level.getBiome(centerPos).is(targetBiome);
    }

    /**
     * 🔄 執行方塊替換
     */
    private static int performReplacement(ServerLevel level, ChunkAccess chunk,
                                          Map<Block, Block> replacements,
                                          Map<Block, BiPredicate<ChunkAccess, BlockPos>> conditions) {

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int replacedCount = 0;

        // 掃描範圍：海平面上下10格
        int minY = level.getSeaLevel() - 10;
        int maxY = level.getSeaLevel() + 10;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = maxY; y >= minY; y--) {
                    pos.set(
                            chunk.getPos().x * 16 + x,
                            y,
                            chunk.getPos().z * 16 + z
                    );

                    BlockState currentState = chunk.getBlockState(pos);
                    Block currentBlock = currentState.getBlock();

                    // 🎯 檢查條件替換
                    if (conditions.containsKey(currentBlock)) {
                        BiPredicate<ChunkAccess, BlockPos> condition = conditions.get(currentBlock);
                        if (condition.test(chunk, pos)) {
                            Block targetBlock = replacements.get(currentBlock);
                            if (targetBlock != null) {
                                chunk.setBlockState(pos, targetBlock.defaultBlockState(), false);
                                replacedCount++;
                                continue;
                            }
                        }
                    }

                    // 🔄 檢查簡單替換
                    if (replacements.containsKey(currentBlock)) {
                        Block targetBlock = replacements.get(currentBlock);
                        chunk.setBlockState(pos, targetBlock.defaultBlockState(), false);
                        replacedCount++;
                    }
                }
            }
        }

        return replacedCount;
    }

    // === 建造者類 ===

    public static class ReplacementBuilder {
        private final Map<Block, Block> replacements = new HashMap<>();
        private final Map<Block, BiPredicate<ChunkAccess, BlockPos>> conditions = new HashMap<>();

        public ReplacementBuilder replace(Block source, Block target) {
            replacements.put(source, target);
            return this;
        }

        public ReplacementBuilder replaceIf(Block source, Block target,
                                            BiPredicate<ChunkAccess, BlockPos> condition) {
            replacements.put(source, target);
            conditions.put(source, condition);
            return this;
        }

        public int apply(ServerLevel level, ChunkAccess chunk, ResourceKey<Biome> targetBiome) {
            return BlockReplacementUtils.replace(level, chunk, targetBiome, replacements, conditions);
        }
    }

    // === 常用條件 ===

    /**
     * 🌊 常用條件：深層地下
     */
    public static BiPredicate<ChunkAccess, BlockPos> DEEP_UNDERGROUND = (chunk, pos) -> {
        int solidBlocks = 0;
        for (int y = pos.getY() + 1; y <= pos.getY() + 6; y++) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (!chunk.getBlockState(checkPos).isAir()) {
                solidBlocks++;
            }
        }
        return solidBlocks >= 4;
    };

    /**
     * 🌱 常用條件：地表
     */
    public static BiPredicate<ChunkAccess, BlockPos> SURFACE = DEEP_UNDERGROUND.negate();

    /**
     * 🎲 常用條件：隨機機率
     */
    public static BiPredicate<ChunkAccess, BlockPos> chance(float probability) {
        return (chunk, pos) -> Math.random() < probability;
    }

    /**
     * 🌊 常用條件：靠近水源
     */
    public static BiPredicate<ChunkAccess, BlockPos> nearWater(int radius) {
        return (chunk, pos) -> {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos checkPos = pos.offset(dx, 0, dz);
                    if (chunk.getBlockState(checkPos).is(net.minecraft.world.level.block.Blocks.WATER)) {
                        return true;
                    }
                }
            }
            return false;
        };
    }
}
