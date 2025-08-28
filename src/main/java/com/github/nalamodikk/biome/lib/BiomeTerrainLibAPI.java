// 🌍 Biome Terrain Library - Simple API
package com.github.nalamodikk.biome.lib;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.function.Supplier;

/**
 * 🚀 Biome Terrain Library - 簡單API
 *
 * 使用範例：
 * ```java
 * // 註冊魔力草原
 * BiomeTerrainLibAPI.addBiome(ModBiomes.MANA_PLAINS)
 *     .surface(() -> ModBlocks.MANA_GRASS.get())
 *     .soil(() -> ModBlocks.MANA_SOIL.get())
 *     .deepSoil(() -> ModBlocks.DEEP_MANA_SOIL.get(), 20)
 *     .avoidWater()
 *     .priority(10)
 *     .register();
 *
 * // 初始化系統
 * BiomeTerrainLibAPI.initialize();
 *
 * // 獲取所有規則
 * SurfaceRules.RuleSource rules = BiomeTerrainLibAPI.getAllRules();
 * ```
 */
public class BiomeTerrainLibAPI {

    /**
     * 🌱 開始配置生物群系
     */
    public static BiomeBuilder addBiome(ResourceKey<Biome> biome) {
        return new BiomeBuilder(biome);
    }

    /**
     * 🚀 初始化庫系統
     */
    public static void initialize() {
        BiomeIntegration.initialize();
    }

    /**
     * 🎯 獲取所有 Surface Rules
     */
    public static SurfaceRules.RuleSource getAllRules() {
        return UniversalTerrainEcosystemLibrary.createAllEcosystemRules();
    }

    /**
     * 🧹 清理系統
     */
    public static void cleanup() {
        BiomeIntegration.cleanup();
    }

    /**
     * 📊 獲取統計信息
     */
    public static String getStats() {
        return String.format("已註冊 %d 個生態系統",
                UniversalTerrainEcosystemLibrary.getRegisteredEcosystemCount());
    }

    // ================================
    // 🏗️ 流暢的建構器API
    // ================================

    public static class BiomeBuilder {
        private final ResourceKey<Biome> biome;
        private final EcosystemConfig.Builder configBuilder;

        BiomeBuilder(ResourceKey<Biome> biome) {
            this.biome = biome;
            this.configBuilder = EcosystemConfig.builder();
        }

        public BiomeBuilder surface(Supplier<Block> block) {
            configBuilder.surfaceBlock(block);
            return this;
        }

        public BiomeBuilder soil(Supplier<Block> block) {
            configBuilder.soilBlock(block);
            return this;
        }

        public BiomeBuilder deepSoil(Supplier<Block> block, int threshold) {
            configBuilder.deepSoilBlock(block, threshold);
            return this;
        }

        public BiomeBuilder deepSoil(Supplier<Block> block) {
            configBuilder.deepSoilBlock(block);
            return this;
        }

        public BiomeBuilder stone(Supplier<Block> block, int threshold) {
            configBuilder.stoneBlock(block, threshold);
            return this;
        }

        public BiomeBuilder stone(Supplier<Block> block) {
            configBuilder.stoneBlock(block);
            return this;
        }

        public BiomeBuilder avoidWater() {
            configBuilder.waterRules(WaterRules.AVOID_WATER);
            return this;
        }

        public BiomeBuilder allowUnderwater() {
            configBuilder.waterRules(WaterRules.ALLOW_UNDERWATER);
            return this;
        }

        public BiomeBuilder nearWater() {
            configBuilder.waterRules(WaterRules.ONLY_NEAR_WATER);
            return this;
        }

        public BiomeBuilder replaceWaterWithLava() {
            configBuilder.waterRules(WaterRules.REPLACE_WITH_LAVA);
            return this;
        }

        public BiomeBuilder priority(int priority) {
            configBuilder.priority(priority);
            return this;
        }

        /**
         * 🎯 註冊到系統
         */
        public void register() {
            EcosystemConfig config = configBuilder.build();
            UniversalTerrainEcosystemLibrary.registerEcosystem(biome, config);
        }
    }

    // ================================
    // 🎨 預設模板快速方法
    // ================================

    /**
     * 🌱 使用草原模板
     */
    public static void addPlains(ResourceKey<Biome> biome, Supplier<Block> grassBlock,
                                 Supplier<Block> soilBlock) {
        addBiome(biome)
                .surface(grassBlock)
                .soil(soilBlock)
                .avoidWater()
                .priority(5)
                .register();
    }

    /**
     * 🏜️ 使用沙漠模板
     */
    public static void addDesert(ResourceKey<Biome> biome, Supplier<Block> sandBlock,
                                 Supplier<Block> sandstoneBlock) {
        addBiome(biome)
                .surface(sandBlock)
                .soil(sandstoneBlock)
                .deepSoil(sandstoneBlock, 30)
                .nearWater()
                .priority(6)
                .register();
    }

    /**
     * 🌋 使用火山模板
     */
    public static void addVolcanic(ResourceKey<Biome> biome, Supplier<Block> ashBlock,
                                   Supplier<Block> basaltBlock) {
        addBiome(biome)
                .surface(ashBlock)
                .soil(ashBlock)
                .stone(basaltBlock, 10)
                .replaceWaterWithLava()
                .priority(9)
                .register();
    }

    /**
     * ❄️ 使用雪原模板
     */
    public static void addSnowy(ResourceKey<Biome> biome, Supplier<Block> snowBlock,
                                Supplier<Block> iceBlock) {
        addBiome(biome)
                .surface(snowBlock)
                .soil(iceBlock)
                .deepSoil(iceBlock, 25)
                .avoidWater()
                .priority(7)
                .register();
    }
}