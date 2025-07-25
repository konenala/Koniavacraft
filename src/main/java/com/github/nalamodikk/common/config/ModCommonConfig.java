package com.github.nalamodikk.common.config;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.biome.CompleteBiomeSurface;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ModCommonConfig {

    // 持有實例與規格
    public static final ModCommonConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        // 使用官方推薦的 configure 方法建立
        Pair<ModCommonConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ModCommonConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    // ===============================
    // 🎯 原有設定值
    // ===============================
    public final ModConfigSpec.IntValue manaRecipeRefreshInterval;
    public final ModConfigSpec.BooleanValue showIntroAnimation;

    // ===============================
    // 🌍 生物群系處理設定
    // ===============================

    // 🧵 多線程設定
    public final ModConfigSpec.BooleanValue biomeMultithreading;
    public final ModConfigSpec.IntValue biomeThreadCount;

    // 📊 性能控制
    public final ModConfigSpec.IntValue biomeMaxProcessingTimeMs;
    public final ModConfigSpec.IntValue biomeMaxChunksPerTick;
    public final ModConfigSpec.IntValue biomeQueueLimit;

    // 🛡️ 內存管理
    public final ModConfigSpec.IntValue biomeMaxMemoryMB;
    public final ModConfigSpec.IntValue biomeSnapshotDepth;

    // 🎯 智能功能
    public final ModConfigSpec.BooleanValue biomeAdaptivePerformance;
    public final ModConfigSpec.BooleanValue biomeAutoSkipMinor;

    private ModCommonConfig(ModConfigSpec.Builder builder) {
        // ===============================
        // 🎯 原有配置項目
        // ===============================
        manaRecipeRefreshInterval = builder
                .comment("每幾 tick 更新一次魔力合成配方結果（建議值：2～10）")
                .comment("How many ticks to refresh the mana crafting recipe result (Recommended value: 2-10)")
                .translation("koniava.config.manaRecipeRefreshInterval")
                .defineInRange("manaRecipeRefreshInterval", 2, 1, 40);

        showIntroAnimation = builder
                .comment("是否啟用登入動畫（預設開啟）")
                .comment("Enable intro animation on player login (default: true)")
                .translation("koniava.config.showIntroAnimation")
                .define("showIntroAnimation", true);

        // ===============================
        // 🌍 生物群系處理配置區段
        // ===============================
        builder.push("biome_processing");

        // 🧵 多線程配置
        builder.push("multithreading");

        biomeMultithreading = builder
                .comment("啟用生物群系處理多線程加速")
                .comment("Enable multithreading for biome processing acceleration")
                .comment("建議：有4核心以上CPU且內存充足時啟用")
                .comment("Recommended: Enable when you have 4+ CPU cores and sufficient memory")
                .translation("koniava.config.biome.multithreading")
                .worldRestart()
                .define("enableMultithreading", getDefaultMultithreading());

        biomeThreadCount = builder
                .comment("多線程處理時使用的線程數")
                .comment("Number of threads to use for multithreaded processing")
                .comment("建議值：CPU核心數 - 2（最少1個，最多8個）")
                .comment("Recommended: CPU cores - 2 (minimum 1, maximum 8)")
                .translation("koniava.config.biome.threadCount")
                .worldRestart()
                .defineInRange("threadCount", getDefaultThreadCount(), 1, 8);

        builder.pop(); // multithreading

        // 📊 性能控制
        builder.push("performance");

        biomeMaxProcessingTimeMs = builder
                .comment("每個 tick 最多花費多少毫秒處理生物群系")
                .comment("Maximum milliseconds per tick for biome processing")
                .comment("較高值 = 更快處理但可能影響 TPS")
                .comment("Higher values = faster processing but may affect TPS")
                .translation("koniava.config.biome.maxProcessingTime")
                .defineInRange("maxProcessingTimeMs", 2, 1, 10);

        biomeMaxChunksPerTick = builder
                .comment("每個 tick 最多處理多少個區塊")
                .comment("Maximum chunks to process per tick")
                .comment("較高值在高負載時處理更快")
                .comment("Higher values process faster under high load")
                .translation("koniava.config.biome.maxChunksPerTick")
                .defineInRange("maxChunksPerTick", 2, 1, 16);

        biomeQueueLimit = builder
                .comment("處理隊列的最大大小")
                .comment("Maximum size of the processing queue")
                .comment("超過此值將跳過新的區塊以防止記憶體溢出")
                .comment("Exceeding this will skip new chunks to prevent memory overflow")
                .translation("koniava.config.biome.queueLimit")
                .defineInRange("queueLimit", getDefaultQueueLimit(), 10, 500);

        builder.pop(); // performance

        // 🛡️ 內存管理
        builder.push("memory");

        biomeMaxMemoryMB = builder
                .comment("生物群系處理最大內存使用量（MB）")
                .comment("Maximum memory usage for biome processing (MB)")
                .comment("根據服務器記憶體自動調整：<2GB=50MB, 2-4GB=100MB, >4GB=200MB")
                .comment("Auto-adjusted based on server memory: <2GB=50MB, 2-4GB=100MB, >4GB=200MB")
                .translation("koniava.config.biome.maxMemory")
                .defineInRange("maxMemoryMB", getDefaultMaxMemory(), 20, 1000);

        biomeSnapshotDepth = builder
                .comment("地表快照深度（方塊層數）")
                .comment("Surface snapshot depth (block layers)")
                .comment("較低值使用較少記憶體但可能遺漏深層替換")
                .comment("Lower values use less memory but may miss deep replacements")
                .translation("koniava.config.biome.snapshotDepth")
                .defineInRange("snapshotDepth", 10, 3, 20);

        builder.pop(); // memory

        // 🎯 智能功能
        builder.push("adaptive");

        biomeAdaptivePerformance = builder
                .comment("啟用自適應性能調整")
                .comment("Enable adaptive performance adjustment")
                .comment("系統會根據負載自動調整處理參數")
                .comment("System will automatically adjust processing parameters based on load")
                .translation("koniava.config.biome.adaptivePerformance")
                .define("enableAdaptivePerformance", true);

        biomeAutoSkipMinor = builder
                .comment("高負載時自動跳過次要生物群系")
                .comment("Automatically skip minor biomes under high load")
                .comment("緊急模式下會跳過優先級較低的生物群系處理")
                .comment("Skip lower priority biomes during emergency mode")
                .translation("koniava.config.biome.autoSkipMinor")
                .define("autoSkipMinorBiomes", true);

        builder.pop(); // adaptive
        builder.pop(); // biome_processing
    }

    // ===============================
    // 🤖 智能默認值計算
    // ===============================

    /**
     * 🧮 根據系統規格計算是否啟用多線程
     */
    private static boolean getDefaultMultithreading() {
        int cores = Runtime.getRuntime().availableProcessors();
        long memoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);

        // 只有在4核心以上且內存充足時才啟用多線程
        return cores >= 4 && memoryMB >= 2048;
    }

    /**
     * 🧮 根據CPU核心數計算最佳線程數
     */
    private static int getDefaultThreadCount() {
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores <= 2) return 1;
        if (cores <= 4) return 2;
        if (cores <= 8) return Math.max(2, cores - 2);
        return 6; // 最多6個線程
    }

    /**
     * 🧮 根據內存大小計算隊列限制
     */
    private static int getDefaultQueueLimit() {
        long memoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        if (memoryMB < 2048) return 20;      // <2GB
        if (memoryMB < 4096) return 50;      // 2-4GB
        if (memoryMB < 8192) return 100;     // 4-8GB
        return 200;                          // >8GB
    }

    /**
     * 🧮 根據內存大小計算最大內存使用
     */
    private static int getDefaultMaxMemory() {
        long memoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        if (memoryMB < 2048) return 50;      // <2GB：50MB
        if (memoryMB < 4096) return 100;     // 2-4GB：100MB
        if (memoryMB < 8192) return 200;     // 4-8GB：200MB
        return 400;                          // >8GB：400MB
    }

    // ===============================
    // 🎧 配置事件處理器
    // ===============================

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            // 原有配置日誌
            KoniavacraftMod.LOGGER.info("載入魔力設定: manaRecipeRefreshInterval = {}",
                    INSTANCE.manaRecipeRefreshInterval.get());
            KoniavacraftMod.LOGGER.info("載入動畫設定: showIntroAnimation = {}",
                    INSTANCE.showIntroAnimation.get());

            // 生物群系配置日誌
            KoniavacraftMod.LOGGER.info("🌍 載入生物群系處理設定:");
            KoniavacraftMod.LOGGER.info("   多線程: {} ({}個線程)",
                    INSTANCE.biomeMultithreading.get(), INSTANCE.biomeThreadCount.get());
            KoniavacraftMod.LOGGER.info("   性能限制: {}ms/tick, {}chunks/tick",
                    INSTANCE.biomeMaxProcessingTimeMs.get(), INSTANCE.biomeMaxChunksPerTick.get());
            KoniavacraftMod.LOGGER.info("   內存限制: {}MB, 深度{}層",
                    INSTANCE.biomeMaxMemoryMB.get(), INSTANCE.biomeSnapshotDepth.get());
            KoniavacraftMod.LOGGER.info("   隊列限制: {}, 自適應: {}, 自動跳過: {}",
                    INSTANCE.biomeQueueLimit.get(), INSTANCE.biomeAdaptivePerformance.get(),
                    INSTANCE.biomeAutoSkipMinor.get());

            // 通知生物群系系統重新載入
            CompleteBiomeSurface.onConfigReload();
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            // 原有配置日誌
            KoniavacraftMod.LOGGER.info("重新載入魔力設定: manaRecipeRefreshInterval = {}",
                    INSTANCE.manaRecipeRefreshInterval.get());
            KoniavacraftMod.LOGGER.info("重新載入動畫設定: showIntroAnimation = {}",
                    INSTANCE.showIntroAnimation.get());

            // 重新載入生物群系配置
            KoniavacraftMod.LOGGER.info("🔄 重新載入生物群系處理設定");
            CompleteBiomeSurface.onConfigReload();
        }
    }

    // ===============================
    // 🛠️ 配置工具方法
    // ===============================

    /**
     * 📋 獲取當前配置摘要
     */
    public static String getConfigSummary() {
        return String.format("""
            🎛️ Koniavacraft 配置摘要:
            
            📜 基本設定:
              魔力配方刷新間隔: %d ticks
              登入動畫: %s
            
            🌍 生物群系處理:
              多線程: %s (%d個線程)
              性能: %dms/tick, %d chunks/tick
              內存: %dMB 限制, %d層深度
              隊列: %d 個上限
              智能功能: 自適應=%s, 自動跳過=%s
            
            💡 建議: 如遇到性能問題，可調整上述參數或關閉多線程
            """,
                INSTANCE.manaRecipeRefreshInterval.get(),
                INSTANCE.showIntroAnimation.get() ? "啟用" : "停用",

                INSTANCE.biomeMultithreading.get() ? "啟用" : "停用",
                INSTANCE.biomeThreadCount.get(),
                INSTANCE.biomeMaxProcessingTimeMs.get(),
                INSTANCE.biomeMaxChunksPerTick.get(),
                INSTANCE.biomeMaxMemoryMB.get(),
                INSTANCE.biomeSnapshotDepth.get(),
                INSTANCE.biomeQueueLimit.get(),
                INSTANCE.biomeAdaptivePerformance.get() ? "是" : "否",
                INSTANCE.biomeAutoSkipMinor.get() ? "是" : "否"
        );
    }

    /**
     * 🚀 一鍵性能模式設定
     */
    public static class PerformancePresets {

        public static void applyLowEndServer() {
            KoniavacraftMod.LOGGER.info("🔧 應用低配置服務器預設...");
            // 這個功能需要配合配置重載機制，這裡只是示例
        }

        public static void applyHighEndServer() {
            KoniavacraftMod.LOGGER.info("🚀 應用高配置服務器預設...");
            // 這個功能需要配合配置重載機制，這裡只是示例
        }

        public static void applyBalanced() {
            KoniavacraftMod.LOGGER.info("⚖️ 應用平衡設定預設...");
            // 這個功能需要配合配置重載機制，這裡只是示例
        }
    }
}