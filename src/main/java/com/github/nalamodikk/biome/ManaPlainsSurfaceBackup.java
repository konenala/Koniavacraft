package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * 🔄 魔力草原地表備用處理器
 *
 * 這個處理器作為 Mixin 的備用方案：
 * - 如果 Mixin 因為初始化問題失敗
 * - 這個事件處理器會在區塊載入時替換方塊
 * - 確保魔力草原最終有正確的地表方塊
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ManaPlainsSurfaceBackup {

    private static boolean hasLoggedBackupActivation = false;

    /**
     * 🎯 區塊載入事件 - 備用地表方塊替換
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // 只處理伺服器端的區塊
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkAccess chunk = event.getChunk();

        try {
            // 🔍 檢查區塊是否在魔力草原生物群系中
            if (isInManaPlains(serverLevel, chunk)) {

                // 🔍 檢查是否需要執行備用處理
                if (needsBackupProcessing(chunk)) {

                    // 記錄備用處理啟動（只記錄一次）
                    if (!hasLoggedBackupActivation) {
                        KoniavacraftMod.LOGGER.info("🔄 啟動魔力草原地表備用處理系統");
                        hasLoggedBackupActivation = true;
                    }

                    // 🔄 執行地表方塊替換
                    replaceSurfaceBlocks(serverLevel, chunk);

                    KoniavacraftMod.LOGGER.debug("✅ 備用處理：魔力草原區塊 ({}, {}) 地表方塊替換完成",
                            chunk.getPos().x, chunk.getPos().z);
                }
            }
        } catch (Exception e) {
            // 記錄錯誤但不中斷遊戲
            KoniavacraftMod.LOGGER.debug("⚠️ 備用地表處理失敗 ({}, {}): {}",
                    chunk.getPos().x, chunk.getPos().z, e.getMessage());
        }
    }

    /**
     * 🔍 檢查區塊是否在魔力草原生物群系中
     */
    private static boolean isInManaPlains(ServerLevel level, ChunkAccess chunk) {
        try {
            // 檢查區塊中心位置的生物群系
            BlockPos centerPos = new BlockPos(
                    chunk.getPos().x * 16 + 8,
                    level.getSeaLevel(),
                    chunk.getPos().z * 16 + 8
            );

            var biome = level.getBiome(centerPos);
            return biome.is(ModBiomes.MANA_PLAINS);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 🔍 檢查是否需要備用處理
     * （例如：如果還有原版草方塊，說明 Mixin 沒有生效）
     */
    private static boolean needsBackupProcessing(ChunkAccess chunk) {
        try {
            // 快速檢查區塊中是否還有原版草方塊
            for (int x = 0; x < 16; x += 4) {  // 每隔4格檢查一次，提高效率
                for (int z = 0; z < 16; z += 4) {
                    for (int y = chunk.getMaxBuildHeight() - 1; y >= chunk.getMinBuildHeight(); y--) {
                        BlockPos pos = new BlockPos(
                                chunk.getPos().x * 16 + x,
                                y,
                                chunk.getPos().z * 16 + z
                        );

                        BlockState state = chunk.getBlockState(pos);

                        // 如果找到原版草方塊，說明需要備用處理
                        if (state.is(Blocks.GRASS_BLOCK)) {
                            return true;
                        }

                        // 如果已經是魔力草方塊，說明不需要處理
                        if (state.is(ModBlocks.MANA_GRASS_BLOCK.get())) {
                            return false;
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            // 如果檢查出錯，保守地執行備用處理
            return true;
        }
    }

    /**
     * 🔄 執行地表方塊替換
     */
    private static void replaceSurfaceBlocks(ServerLevel level, ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int replacedCount = 0;

        // 遍歷區塊中的所有位置
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // 從世界頂部開始向下檢查（只檢查地表附近）
                for (int y = level.getMaxBuildHeight() - 1; y >= level.getSeaLevel() - 10; y--) {
                    pos.set(
                            chunk.getPos().x * 16 + x,
                            y,
                            chunk.getPos().z * 16 + z
                    );

                    try {
                        BlockState currentState = chunk.getBlockState(pos);

                        // 🌱 替換草方塊為魔力草方塊
                        if (currentState.is(Blocks.GRASS_BLOCK)) {
                            chunk.setBlockState(pos, ModBlocks.MANA_GRASS_BLOCK.get().defaultBlockState(), false);
                            replacedCount++;
                        }
                        // 🌱 替換土方塊為魔力土壤
                        else if (currentState.is(Blocks.DIRT)) {
                            // 檢查深度來決定使用哪種魔力土壤
                            if (isDeepUnderground(chunk, pos)) {
                                chunk.setBlockState(pos, ModBlocks.DEEP_MANA_SOIL.get().defaultBlockState(), false);
                            } else {
                                chunk.setBlockState(pos, ModBlocks.MANA_SOIL.get().defaultBlockState(), false);
                            }
                            replacedCount++;
                        }
                    } catch (Exception e) {
                        // 忽略個別方塊的錯誤
                    }
                }
            }
        }

        // 如果有替換方塊，標記區塊為已修改
        if (replacedCount > 0) {
            chunk.setUnsaved(true);
            KoniavacraftMod.LOGGER.debug("🔄 備用處理替換了 {} 個方塊", replacedCount);
        }
    }

    /**
     * 🔍 檢查位置是否在深層地下
     */
    private static boolean isDeepUnderground(ChunkAccess chunk, BlockPos pos) {
        try {
            int solidBlocksAbove = 0;

            for (int y = pos.getY() + 1; y <= pos.getY() + 6; y++) {
                BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
                BlockState state = chunk.getBlockState(checkPos);

                if (state.is(Blocks.DIRT) ||
                        state.is(Blocks.STONE) ||
                        state.is(Blocks.GRASS_BLOCK) ||
                        state.is(ModBlocks.MANA_SOIL.get()) ||
                        state.is(ModBlocks.MANA_GRASS_BLOCK.get())) {
                    solidBlocksAbove++;
                }
            }

            return solidBlocksAbove >= 4;
        } catch (Exception e) {
            return false;
        }
    }
}