package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.config.ModCommonConfig;
import com.github.nalamodikk.common.utils.world.BlockReplacementUtils;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.*;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class CompleteBiomeSurface {

    // 🎯 任務隊列系統
    private static final Queue<BiomeProcessingTask> PROCESSING_QUEUE = new ArrayDeque<>();
    private static final Queue<ComputedReplacementTask> COMPUTED_QUEUE = new ArrayDeque<>();
    private static final Set<ChunkPos> PROCESSED_CHUNKS = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceKey<Biome>, BiomeProcessor> BIOME_PROCESSORS = new HashMap<>();
    private static final Map<ResourceKey<Biome>, Integer> BIOME_PRIORITIES = new HashMap<>();

    // 🧵 多線程執行器
    private static ExecutorService COMPUTATION_EXECUTOR;
    private static boolean isMultithreadingActive = false;

    // 📊 性能統計
    private static long totalProcessingTime = 0;
    private static int totalChunksProcessed = 0;
    private static int skippedChunks = 0;
    private static long currentMemoryUsage = 0;
    private static int consecutiveHighLoadTicks = 0;
    private static boolean emergencyMode = false;

    static {
        // 🌱 註冊生物群系處理器
        registerBiomeProcessor(ModBiomes.MANA_PLAINS, new ManaPlainsBiomeProcessor(), 10);

    }

    /**
     * 🧵 初始化線程池（基於配置）
     */
    private static synchronized void initializeThreadPool() {
        if (isMultithreadingActive || COMPUTATION_EXECUTOR != null) {
            return; // 已經初始化過了
        }

        try {
            boolean enableMultithreading = ModCommonConfig.INSTANCE.biomeMultithreading.get();
            int threadCount = ModCommonConfig.INSTANCE.biomeThreadCount.get();

            if (enableMultithreading) {
                COMPUTATION_EXECUTOR = Executors.newFixedThreadPool(threadCount, r -> {
                    Thread t = new Thread(r, "BiomeComputation-" + System.currentTimeMillis());
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    return t;
                });
                isMultithreadingActive = true;
                KoniavacraftMod.LOGGER.info("🧵 啟用多線程生物群系處理 - {}個線程", threadCount);
            } else {
                isMultithreadingActive = false;
                KoniavacraftMod.LOGGER.info("🔄 使用單線程生物群系處理");
            }
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("❌ 初始化線程池失敗: {}", e.getMessage());
            isMultithreadingActive = false;
        }
    }
    /**
     * 🎯 生物群系處理任務
     */
    public static class BiomeProcessingTask {
        public final ServerLevel level;
        public final ChunkAccess chunk;
        public final ChunkPos chunkPos;
        public final long submittedTime;
        public final ResourceKey<Biome> detectedBiome;
        public final int priority;
        public final Map<BlockPos, BlockState> chunkSnapshot;

        public BiomeProcessingTask(ServerLevel level, ChunkAccess chunk, ResourceKey<Biome> detectedBiome, int priority) {
            this.level = level;
            this.chunk = chunk;
            this.chunkPos = chunk.getPos();
            this.detectedBiome = detectedBiome;
            this.priority = priority;
            this.submittedTime = System.currentTimeMillis();
            this.chunkSnapshot = createChunkSnapshot(chunk);
            estimateMemoryUsage();
        }

        private Map<BlockPos, BlockState> createChunkSnapshot(ChunkAccess chunk) {
            Map<BlockPos, BlockState> snapshot = new HashMap<>();
            ChunkPos pos = chunk.getPos();
            // 安全讀取配置，提供預設值

            int depthLimit;
            if (emergencyMode) {
                depthLimit = 5;
            } else {
                try {
                    depthLimit = ModCommonConfig.INSTANCE.biomeSnapshotDepth.get();
                } catch (Exception e) {
                    depthLimit = 10; // 使用預設值
                }
            }

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = chunk.getMaxBuildHeight() - 1; y >= chunk.getMinBuildHeight(); y--) {
                        BlockPos blockPos = new BlockPos(pos.x * 16 + x, y, pos.z * 16 + z);
                        BlockState state = chunk.getBlockState(blockPos);

                        if (!state.isAir()) {
                            snapshot.put(blockPos.immutable(), state);

                            // 記錄地表往下幾層
                            int actualDepthLimit = Math.max(y - depthLimit, chunk.getMinBuildHeight());
                            for (int dy = y - 1; dy >= actualDepthLimit; dy--) {
                                BlockPos deepPos = new BlockPos(pos.x * 16 + x, dy, pos.z * 16 + z);
                                BlockState deepState = chunk.getBlockState(deepPos);
                                snapshot.put(deepPos.immutable(), deepState);
                            }
                            break;
                        }
                    }
                }
            }
            return snapshot;
        }

        private void estimateMemoryUsage() {
            long chunkMemory = chunkSnapshot.size() * 80L;
            currentMemoryUsage += chunkMemory;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - submittedTime > 10000;
        }

        public boolean shouldSkipInEmergency() {
            return emergencyMode && ModCommonConfig.INSTANCE.biomeAutoSkipMinor.get() && priority < 5;
        }
    }

    /**
     * 🎯 已計算完成的替換任務
     */
    public static class ComputedReplacementTask {
        public final ChunkPos chunkPos;
        public final ServerLevel level;
        public final Map<BlockPos, BlockState> replacements;
        public final long computedTime;

        public ComputedReplacementTask(ChunkPos chunkPos, ServerLevel level, Map<BlockPos, BlockState> replacements) {
            this.chunkPos = chunkPos;
            this.level = level;
            this.replacements = replacements;
            this.computedTime = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - computedTime > 5000;
        }
    }

    /**
     * 🎯 生物群系處理器接口
     */
    public interface BiomeProcessor {
        Map<BlockPos, BlockState> analyzeReplacements(ChunkPos chunkPos, Map<BlockPos, BlockState> chunkSnapshot);
        boolean canProcess(ServerLevel level, ChunkAccess chunk);
        default int processChunk(ServerLevel level, ChunkAccess chunk) { return 0; }
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

        @Override
        public Map<BlockPos, BlockState> analyzeReplacements(ChunkPos chunkPos, Map<BlockPos, BlockState> chunkSnapshot) {
            Map<BlockPos, BlockState> replacements = new HashMap<>();

            for (Map.Entry<BlockPos, BlockState> entry : chunkSnapshot.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState currentState = entry.getValue();

                if (currentState.is(Blocks.GRASS_BLOCK)) {
                    replacements.put(pos, ModBlocks.MANA_GRASS_BLOCK.get().defaultBlockState());
                } else if (currentState.is(Blocks.DIRT)) {
                    if (isDeepUnderground(pos, chunkSnapshot)) {
                        replacements.put(pos, ModBlocks.DEEP_MANA_SOIL.get().defaultBlockState());
                    } else {
                        replacements.put(pos, ModBlocks.MANA_SOIL.get().defaultBlockState());
                    }
                }
            }

            return replacements;
        }

        private boolean isDeepUnderground(BlockPos pos, Map<BlockPos, BlockState> snapshot) {
            int solidBlocksAbove = 0;
            for (int y = pos.getY() + 1; y <= pos.getY() + 6; y++) {
                BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
                BlockState state = snapshot.get(checkPos);
                if (state != null && isSolidBlock(state)) {
                    solidBlocksAbove++;
                }
            }
            return solidBlocksAbove >= 4;
        }

        private boolean isSolidBlock(BlockState state) {
            return state.is(Blocks.DIRT) || state.is(Blocks.STONE) ||
                    state.is(Blocks.GRASS_BLOCK) || state.is(ModBlocks.MANA_SOIL.get()) ||
                    state.is(ModBlocks.MANA_GRASS_BLOCK.get());
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!isMultithreadingActive && COMPUTATION_EXECUTOR == null) {
            initializeThreadPool();
        }
        ChunkAccess chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();

        // 快速跳過檢查
        if (PROCESSED_CHUNKS.contains(chunkPos)) {
            skippedChunks++;
            return;
        }

        // 隊列限制檢查
        int queueLimit = ModCommonConfig.INSTANCE.biomeQueueLimit.get();
        if (PROCESSING_QUEUE.size() >= queueLimit * 2) {
            enterEmergencyMode();
            skippedChunks++;
            return;
        }

        // 內存使用檢查
        long maxMemoryBytes = ModCommonConfig.INSTANCE.biomeMaxMemoryMB.get() * 1024L * 1024L;
        if (currentMemoryUsage > maxMemoryBytes) {
            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.warn("⚠️ 內存使用超限，跳過chunk處理");
            }
            skippedChunks++;
            return;
        }

        // 檢測生物群系類型
        ResourceKey<Biome> detectedBiome = detectBiomeType(level, chunk);
        if (detectedBiome != null) {
            int priority = BIOME_PRIORITIES.getOrDefault(detectedBiome, 5);
            BiomeProcessingTask task = new BiomeProcessingTask(level, chunk, detectedBiome, priority);

            if (task.shouldSkipInEmergency()) {
                skippedChunks++;
                return;
            }

            PROCESSING_QUEUE.offer(task);

            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.debug("📋 添加生物群系處理任務: {} at ({}, {}), 優先級: {}, 隊列大小: {}",
                        detectedBiome.location().getPath(), chunkPos.x, chunkPos.z, priority, PROCESSING_QUEUE.size());
            }
        } else {
            PROCESSED_CHUNKS.add(chunkPos);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long tickStartTime = System.currentTimeMillis();
        // 延遲初始化
        if (!isMultithreadingActive && COMPUTATION_EXECUTOR == null) {
            initializeThreadPool();
        }
        // 自適應性能調整
        if (ModCommonConfig.INSTANCE.biomeAdaptivePerformance.get()) {
            adaptivePerformanceAdjustment();
        }

        // 根據配置選擇處理模式
        if (isMultithreadingActive && ModCommonConfig.INSTANCE.biomeMultithreading.get()) {
            submitComputationTasks();
            applyComputedReplacements(tickStartTime);
        } else {
            processSingleThreaded(tickStartTime);
        }

        // 性能統計
        if (KoniavacraftMod.IS_DEV && totalChunksProcessed > 0 && totalChunksProcessed % 50 == 0) {
            printDetailedStats();
        }
    }

    /**
     * 🛡️ 自適應性能調整
     */
    private static void adaptivePerformanceAdjustment() {
        int queueSize = PROCESSING_QUEUE.size() + COMPUTED_QUEUE.size();
        int queueLimit = ModCommonConfig.INSTANCE.biomeQueueLimit.get();

        if (queueSize > queueLimit) {
            consecutiveHighLoadTicks++;
        } else {
            consecutiveHighLoadTicks = 0;
            if (emergencyMode) {
                exitEmergencyMode();
            }
        }

        if (consecutiveHighLoadTicks > 20) {
            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.info("🔧 檢測到持續高負載，建議調整配置");
            }
        }
    }

    /**
     * 🚨 進入緊急模式
     */
    private static void enterEmergencyMode() {
        if (!emergencyMode) {
            emergencyMode = true;
            KoniavacraftMod.LOGGER.warn("🚨 生物群系處理進入緊急模式：隊列過載");
            PROCESSING_QUEUE.removeIf(BiomeProcessingTask::isExpired);
            COMPUTED_QUEUE.removeIf(ComputedReplacementTask::isExpired);
        }
    }

    /**
     * ✅ 退出緊急模式
     */
    private static void exitEmergencyMode() {
        emergencyMode = false;
        KoniavacraftMod.LOGGER.info("✅ 生物群系處理退出緊急模式");
    }

    /**
     * 🧵 提交計算任務到多線程
     */
    private static void submitComputationTasks() {
        int maxChunksPerTick = ModCommonConfig.INSTANCE.biomeMaxChunksPerTick.get();
        int submitted = 0;

        while (!PROCESSING_QUEUE.isEmpty() && submitted < maxChunksPerTick) {
            BiomeProcessingTask task = PROCESSING_QUEUE.poll();
            if (task == null || task.isExpired()) continue;

            CompletableFuture.runAsync(() -> {
                try {
                    BiomeProcessor processor = BIOME_PROCESSORS.get(task.detectedBiome);
                    if (processor != null) {
                        Map<BlockPos, BlockState> replacements =
                                processor.analyzeReplacements(task.chunkPos, task.chunkSnapshot);

                        if (!replacements.isEmpty()) {
                            COMPUTED_QUEUE.offer(new ComputedReplacementTask(
                                    task.chunkPos, task.level, replacements));
                        }
                    }

                    currentMemoryUsage -= task.chunkSnapshot.size() * 80L;
                } catch (Exception e) {
                    if (KoniavacraftMod.IS_DEV) {
                        KoniavacraftMod.LOGGER.error("多線程計算出錯: {}", e.getMessage());
                    }
                }
            }, COMPUTATION_EXECUTOR);

            submitted++;
        }
    }

    /**
     * 🎯 在主線程應用已計算的替換
     */
    private static void applyComputedReplacements(long tickStartTime) {
        int maxChunksPerTick = ModCommonConfig.INSTANCE.biomeMaxChunksPerTick.get();
        int maxProcessingTimeMs = ModCommonConfig.INSTANCE.biomeMaxProcessingTimeMs.get();
        int appliedThisTick = 0;

        while (!COMPUTED_QUEUE.isEmpty() &&
                appliedThisTick < maxChunksPerTick &&
                (System.currentTimeMillis() - tickStartTime) < maxProcessingTimeMs) {

            ComputedReplacementTask task = COMPUTED_QUEUE.poll();
            if (task == null || task.isExpired()) continue;

            long taskStartTime = System.currentTimeMillis();
            int replacedBlocks = applyReplacements(task);
            long taskDuration = System.currentTimeMillis() - taskStartTime;

            PROCESSED_CHUNKS.add(task.chunkPos);
            totalProcessingTime += taskDuration;
            totalChunksProcessed++;
            appliedThisTick++;

            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.debug("✅ 多線程應用完成: ({}, {}), 替換{}個方塊, 用時{}ms",
                        task.chunkPos.x, task.chunkPos.z, replacedBlocks, taskDuration);
            }
        }
    }

    /**
     * 🔄 單線程模式處理
     */
    private static void processSingleThreaded(long tickStartTime) {
        int maxChunksPerTick = ModCommonConfig.INSTANCE.biomeMaxChunksPerTick.get();
        int maxProcessingTimeMs = ModCommonConfig.INSTANCE.biomeMaxProcessingTimeMs.get();
        int processedThisTick = 0;

        while (!PROCESSING_QUEUE.isEmpty() &&
                processedThisTick < maxChunksPerTick &&
                (System.currentTimeMillis() - tickStartTime) < maxProcessingTimeMs) {

            BiomeProcessingTask task = PROCESSING_QUEUE.poll();
            if (task == null || task.isExpired()) continue;

            BiomeProcessor processor = BIOME_PROCESSORS.get(task.detectedBiome);
            if (processor != null) {
                long taskStartTime = System.currentTimeMillis();
                int replacedBlocks = processor.processChunk(task.level, task.chunk);
                long taskDuration = System.currentTimeMillis() - taskStartTime;

                if (replacedBlocks > 0) {
                    task.chunk.setUnsaved(true);
                }

                PROCESSED_CHUNKS.add(task.chunkPos);
                totalProcessingTime += taskDuration;
                totalChunksProcessed++;
                processedThisTick++;

                if (KoniavacraftMod.IS_DEV) {
                    KoniavacraftMod.LOGGER.debug("✅ 單線程處理完成: {} at ({}, {}), 替換{}個方塊, 用時{}ms",
                            task.detectedBiome.location().getPath(),
                            task.chunkPos.x, task.chunkPos.z,
                            replacedBlocks, taskDuration);
                }
            }

            currentMemoryUsage -= task.chunkSnapshot.size() * 80L;
        }
    }

    /**
     * 🎯 在主線程中安全地應用方塊替換
     */
    private static int applyReplacements(ComputedReplacementTask task) {
        try {
            ChunkAccess chunk = task.level.getChunk(task.chunkPos.x, task.chunkPos.z);

            for (Map.Entry<BlockPos, BlockState> entry : task.replacements.entrySet()) {
                try {
                    chunk.setBlockState(entry.getKey(), entry.getValue(), false);
                } catch (Exception e) {
                    // 忽略個別方塊錯誤
                }
            }

            if (!task.replacements.isEmpty()) {
                chunk.setUnsaved(true);
            }

            return task.replacements.size();
        } catch (Exception e) {
            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.error("應用替換時出錯: {}", e.getMessage());
            }
            return 0;
        }
    }

    /**
     * 🔍 檢測chunk的生物群系類型
     */
    private static ResourceKey<Biome> detectBiomeType(ServerLevel level, ChunkAccess chunk) {
        return BIOME_PROCESSORS.entrySet().stream()
                .sorted((a, b) -> Integer.compare(
                        BIOME_PRIORITIES.getOrDefault(b.getKey(), 5),
                        BIOME_PRIORITIES.getOrDefault(a.getKey(), 5)))
                .filter(entry -> entry.getValue().canProcess(level, chunk))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * 📊 詳細統計信息
     */
    private static void printDetailedStats() {
        double avgTime = totalChunksProcessed > 0 ? (double) totalProcessingTime / totalChunksProcessed : 0;
        long memoryMB = currentMemoryUsage / (1024 * 1024);

        KoniavacraftMod.LOGGER.info("📊 生物群系系統詳細統計:");
        KoniavacraftMod.LOGGER.info("   模式: {}", isMultithreadingActive ? "多線程" : "單線程");
        KoniavacraftMod.LOGGER.info("   處理chunk: {}, 跳過chunk: {}", totalChunksProcessed, skippedChunks);
        KoniavacraftMod.LOGGER.info("   平均處理時間: {:.2f}ms", avgTime);
        KoniavacraftMod.LOGGER.info("   當前隊列: 分析{}個, 應用{}個", PROCESSING_QUEUE.size(), COMPUTED_QUEUE.size());
        KoniavacraftMod.LOGGER.info("   內存使用: {}MB / {}MB", memoryMB, ModCommonConfig.INSTANCE.biomeMaxMemoryMB.get());
        KoniavacraftMod.LOGGER.info("   緊急模式: {}", emergencyMode ? "啟用" : "關閉");
    }

    /**
     * 🎯 註冊生物群系處理器
     */
    public static void registerBiomeProcessor(ResourceKey<Biome> biome, BiomeProcessor processor, int priority) {
        BIOME_PROCESSORS.put(biome, processor);
        BIOME_PRIORITIES.put(biome, priority);
        KoniavacraftMod.LOGGER.info("📝 註冊生物群系處理器: {} (優先級: {})", biome.location(), priority);
    }

    /**
     * ⚙️ 配置更新時重新初始化線程池
     */
    public static void onConfigReload() {
        shutdownThreadPool();
        initializeThreadPool();
        KoniavacraftMod.LOGGER.info("⚙️ 生物群系配置已重新載入");
    }

    /**
     * 🧹 關閉線程池
     */
    private static void shutdownThreadPool() {
        if (COMPUTATION_EXECUTOR != null) {
            COMPUTATION_EXECUTOR.shutdown();
            try {
                if (!COMPUTATION_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    COMPUTATION_EXECUTOR.shutdownNow();
                    if (!COMPUTATION_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                        KoniavacraftMod.LOGGER.warn("線程池無法正常關閉");
                    }
                }
            } catch (InterruptedException e) {
                COMPUTATION_EXECUTOR.shutdownNow();
                Thread.currentThread().interrupt();
            }
            COMPUTATION_EXECUTOR = null;
            isMultithreadingActive = false;
        }
    }

    /**
     * 🧹 清理系統
     */
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            PROCESSING_QUEUE.clear();
            COMPUTED_QUEUE.clear();
            PROCESSED_CHUNKS.clear();

            shutdownThreadPool();

            // 重置統計和狀態
            totalProcessingTime = 0;
            totalChunksProcessed = 0;
            skippedChunks = 0;
            currentMemoryUsage = 0;
            consecutiveHighLoadTicks = 0;
            emergencyMode = false;

            KoniavacraftMod.LOGGER.info("🧹 生物群系系統已清理完成");
        }
    }

    /**
     * 📋 獲取系統狀態
     */
    public static SystemStatus getSystemStatus() {
        return new SystemStatus(
                PROCESSING_QUEUE.size(),
                COMPUTED_QUEUE.size(),
                PROCESSED_CHUNKS.size(),
                totalChunksProcessed,
                skippedChunks,
                totalProcessingTime,
                currentMemoryUsage,
                isMultithreadingActive,
                emergencyMode,
                ModCommonConfig.INSTANCE.biomeThreadCount.get()
        );
    }

    /**
     * 📊 系統狀態記錄
     */
    public record SystemStatus(int analysisQueueSize, int applicationQueueSize,
                               int processedChunks, int totalProcessed,
                               int skippedChunks, long totalProcessingTime,
                               long memoryUsage, boolean multithreadingActive,
                               boolean emergencyMode, int threadPoolSize) {

        public double getAverageProcessingTime() {
            return totalProcessed > 0 ? (double) totalProcessingTime / totalProcessed : 0;
        }

        public double getMemoryUsageMB() {
            return memoryUsage / (1024.0 * 1024.0);
        }

        public String getStatusSummary() {
            return String.format("模式:%s, 隊列:%d+%d, 處理:%d, 內存:%.1fMB, 緊急:%s",
                    multithreadingActive ? "多線程" : "單線程",
                    analysisQueueSize, applicationQueueSize,
                    totalProcessed, getMemoryUsageMB(),
                    emergencyMode ? "是" : "否");
        }
    }
}