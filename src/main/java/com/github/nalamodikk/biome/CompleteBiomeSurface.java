package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.utils.world.BlockReplacementUtils;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class CompleteBiomeSurface {

    // 🎯 性能控制參數
    private static final int MAX_PROCESSING_TIME_MS = 2;  // 每tick最多2ms
    private static final int MAX_CHUNKS_PER_TICK = 2;     // 每tick最多處理2個chunk
    private static final int CHUNK_QUEUE_LIMIT = 50;      // 隊列大小限制

    // 🚀 任務隊列系統
    private static final Queue<BiomeProcessingTask> PROCESSING_QUEUE = new ArrayDeque<>();
    private static final Set<ChunkPos> PROCESSED_CHUNKS = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceKey<Biome>, BiomeProcessor> BIOME_PROCESSORS = new HashMap<>();

    // 📊 性能統計
    private static long totalProcessingTime = 0;
    private static int totalChunksProcessed = 0;
    private static int skippedChunks = 0;

    static {
        // 🌱 註冊生物群系處理器
        registerBiomeProcessor(ModBiomes.MANA_PLAINS, new ManaPlainsBiomeProcessor());
        // 🚀 未來添加新生物群系時在這裡註冊
        // registerBiomeProcessor(ModBiomes.CRYSTAL_FOREST, new CrystalForestBiomeProcessor());
    }

    /**
     * 🎯 生物群系處理任務
     */
    public static class BiomeProcessingTask {
        public final ServerLevel level;
        public final ChunkAccess chunk;
        public final long submittedTime;
        public final ResourceKey<Biome> detectedBiome;

        public BiomeProcessingTask(ServerLevel level, ChunkAccess chunk, ResourceKey<Biome> detectedBiome) {
            this.level = level;
            this.chunk = chunk;
            this.detectedBiome = detectedBiome;
            this.submittedTime = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - submittedTime > 10000; // 10秒過期
        }

        public ChunkPos getChunkPos() {
            return chunk.getPos();
        }
    }

    /**
     * 🎯 生物群系處理器接口
     */
    public interface BiomeProcessor {
        int processChunk(ServerLevel level, ChunkAccess chunk);
        boolean canProcess(ServerLevel level, ChunkAccess chunk);
    }

    /**
     * 🌱 魔力草原處理器
     */
    public static class ManaPlainsBiomeProcessor implements BiomeProcessor {
        @Override
        public boolean canProcess(ServerLevel level, ChunkAccess chunk) {
            return BlockReplacementUtils.isBiomeFast(level, chunk, ModBiomes.MANA_PLAINS);
        }

        @Override
        public int processChunk(ServerLevel level, ChunkAccess chunk) {
            return BlockReplacementUtils.replace(level, chunk, ModBiomes.MANA_PLAINS,
                    BlockReplacementUtils.rules(
                            Blocks.GRASS_BLOCK, ModBlocks.MANA_GRASS_BLOCK.get(),
                            Blocks.DIRT, ModBlocks.MANA_SOIL.get()
                    ),
                    BlockReplacementUtils.conditions(
                            Blocks.DIRT, BlockReplacementUtils.DEEP_UNDERGROUND.and(
                                    (chunk1, pos) -> ModBlocks.DEEP_MANA_SOIL.get() != null)
                    )
            );
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ChunkAccess chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();

        // 🚀 快速跳過：已處理的chunk
        if (PROCESSED_CHUNKS.contains(chunkPos)) {
            skippedChunks++;
            return;
        }

        // 🚀 快速跳過：隊列已滿
        if (PROCESSING_QUEUE.size() >= CHUNK_QUEUE_LIMIT) {
            skippedChunks++;
            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.warn("⚠️ 生物群系處理隊列已滿，跳過chunk ({}, {})",
                        chunkPos.x, chunkPos.z);
            }
            return;
        }

        // 🎯 檢測生物群系類型
        ResourceKey<Biome> detectedBiome = detectBiomeType(level, chunk);
        if (detectedBiome != null) {
            PROCESSING_QUEUE.offer(new BiomeProcessingTask(level, chunk, detectedBiome));

            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.debug("📋 添加生物群系處理任務: {} at ({}, {}), 隊列大小: {}",
                        detectedBiome.location().getPath(), chunkPos.x, chunkPos.z, PROCESSING_QUEUE.size());
            }
        } else {
            // 不是目標生物群系，標記為已處理避免重複檢查
            PROCESSED_CHUNKS.add(chunkPos);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PROCESSING_QUEUE.isEmpty()) return;

        long tickStartTime = System.currentTimeMillis();
        int processedThisTick = 0;

        // 🚀 時間和數量雙重限制
        while (!PROCESSING_QUEUE.isEmpty() &&
                processedThisTick < MAX_CHUNKS_PER_TICK &&
                (System.currentTimeMillis() - tickStartTime) < MAX_PROCESSING_TIME_MS) {

            BiomeProcessingTask task = PROCESSING_QUEUE.poll();
            if (task == null) break;

            // 跳過過期任務
            if (task.isExpired()) {
                if (KoniavacraftMod.IS_DEV) {
                    KoniavacraftMod.LOGGER.debug("⏰ 跳過過期任務: ({}, {})",
                            task.getChunkPos().x, task.getChunkPos().z);
                }
                continue;
            }

            // 執行處理
            long taskStartTime = System.currentTimeMillis();
            int replacedBlocks = processTask(task);
            long taskDuration = System.currentTimeMillis() - taskStartTime;

            // 標記為已處理
            PROCESSED_CHUNKS.add(task.getChunkPos());
            if (replacedBlocks > 0) {
                task.chunk.setUnsaved(true);
            }

            // 統計
            totalProcessingTime += taskDuration;
            totalChunksProcessed++;
            processedThisTick++;

            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.debug("✅ 處理完成: {} at ({}, {}), 替換{}個方塊, 用時{}ms",
                        task.detectedBiome.location().getPath(),
                        task.getChunkPos().x, task.getChunkPos().z,
                        replacedBlocks, taskDuration);
            }
        }

        // 🎯 定期打印統計信息
        if (KoniavacraftMod.IS_DEV && totalChunksProcessed > 0 && totalChunksProcessed % 20 == 0) {
            printPerformanceStats();
        }
    }

    /**
     * 🔍 檢測chunk的生物群系類型
     */
    private static ResourceKey<Biome> detectBiomeType(ServerLevel level, ChunkAccess chunk) {
        // 🚀 優化：使用緩存的快速檢測
        for (ResourceKey<Biome> biomeKey : BIOME_PROCESSORS.keySet()) {
            BiomeProcessor processor = BIOME_PROCESSORS.get(biomeKey);
            if (processor.canProcess(level, chunk)) {
                return biomeKey;
            }
        }
        return null;
    }

    /**
     * 🎯 處理單個任務
     */
    private static int processTask(BiomeProcessingTask task) {
        BiomeProcessor processor = BIOME_PROCESSORS.get(task.detectedBiome);
        if (processor != null) {
            return processor.processChunk(task.level, task.chunk);
        }
        return 0;
    }

    /**
     * 📊 打印性能統計
     */
    private static void printPerformanceStats() {
        double avgTime = totalChunksProcessed > 0 ? (double) totalProcessingTime / totalChunksProcessed : 0;

        KoniavacraftMod.LOGGER.info("📊 生物群系系統統計:");
        KoniavacraftMod.LOGGER.info("   處理chunk: {}, 跳過chunk: {}", totalChunksProcessed, skippedChunks);
        KoniavacraftMod.LOGGER.info("   平均處理時間: {:.2f}ms", avgTime);
        KoniavacraftMod.LOGGER.info("   當前隊列大小: {}", PROCESSING_QUEUE.size());
        KoniavacraftMod.LOGGER.info("   已處理chunk數: {}", PROCESSED_CHUNKS.size());
    }

    /**
     * 🎯 註冊生物群系處理器
     */
    public static void registerBiomeProcessor(ResourceKey<Biome> biome, BiomeProcessor processor) {
        BIOME_PROCESSORS.put(biome, processor);
        KoniavacraftMod.LOGGER.info("📝 註冊生物群系處理器: {}", biome.location());
    }

    /**
     * 🧹 清理系統
     */
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            PROCESSING_QUEUE.clear();
            PROCESSED_CHUNKS.clear();
            BlockReplacementUtils.clearCache();

            // 重置統計
            totalProcessingTime = 0;
            totalChunksProcessed = 0;
            skippedChunks = 0;

            KoniavacraftMod.LOGGER.info("🧹 生物群系系統已清理");
        }
    }

    /**
     * 📋 獲取系統狀態
     */
    public static SystemStatus getSystemStatus() {
        return new SystemStatus(
                PROCESSING_QUEUE.size(),
                PROCESSED_CHUNKS.size(),
                totalChunksProcessed,
                skippedChunks,
                totalProcessingTime
        );
    }

    public record SystemStatus(int queueSize, int processedChunks, int totalProcessed,
                               int skippedChunks, long totalProcessingTime) {
        public double getAverageProcessingTime() {
            return totalProcessed > 0 ? (double) totalProcessingTime / totalProcessed : 0;
        }
    }
}