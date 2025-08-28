// 🌍 預設生態系統模板 - 庫版本
package com.github.nalamodikk.biome.lib;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Supplier;

/**
 * 🌱 預設生態系統模板 - 庫版本
 *
 * 提供常見生態系統的預設配置模板，讓開發者能夠快速創建標準的地形生態系統。
 *
 * 使用範例：
 * ```java
 * // 使用草原模板並自定義
 * UniversalTerrainEcosystemLibrary.registerEcosystem(
 *     MyBiomes.CUSTOM_PLAINS,
 *     EcosystemTemplates.plains()
 *         .surfaceBlock(() -> MyBlocks.CUSTOM_GRASS.get())
 *         .soilBlock(() -> MyBlocks.CUSTOM_SOIL.get())
 *         .priority(8)
 *         .build()
 * );
 *
 * // 使用沙漠模板
 * UniversalTerrainEcosystemLibrary.registerEcosystem(
 *     MyBiomes.CUSTOM_DESERT,
 *     EcosystemTemplates.desert()
 *         .surfaceBlock(() -> MyBlocks.CRYSTAL_SAND.get())
 *         .build()
 * );
 * ```
 */
public class EcosystemTemplates {

    /**
     * 🌱 草原生態系統模板
     *
     * 特色：
     * - 草地地表
     * - 分層的土壤系統
     * - 避免水下替換
     * - 中等優先級
     */
    public static EcosystemConfig.Builder plains() {
        return EcosystemConfig.builder()
                .surfaceBlock(() -> Blocks.GRASS_BLOCK)
                .soilBlock(() -> Blocks.DIRT)
                .deepSoilBlock(() -> Blocks.COARSE_DIRT, 20)
                .waterRules(WaterRules.AVOID_WATER)
                .priority(5);
    }

    /**
     * 🏜️ 沙漠生態系統模板
     *
     * 特色：
     * - 沙子地表
     * - 砂岩底層
     * - 允許靠近水源（綠洲效應）
     * - 中等優先級
     */
    public static EcosystemConfig.Builder desert() {
        return EcosystemConfig.builder()
                .surfaceBlock(() -> Blocks.SAND)
                .soilBlock(() -> Blocks.SANDSTONE)
                .deepSoilBlock(() -> Blocks.SANDSTONE, 30)
                .waterRules(WaterRules.ONLY_NEAR_WATER)
                .priority(6);
    }

    /**
     * 🌋 火山灰原生態系統模板
     *
     * 特色：
     * - 粗糙泥土地表
     * - 玄武岩石頭替換
     * - 水變岩漿
     * - 高優先級
     */
    public static EcosystemConfig.Builder volcanic() {
        return EcosystemConfig.builder()
                .surfaceBlock(() -> Blocks.COARSE_DIRT)
                .soilBlock(() -> Blocks.COARSE_DIRT)
                .stoneBlock(() -> Blocks.BASALT, 10)
                .waterRules(WaterRules.REPLACE_WITH_LAVA)
                .priority(9);
    }

    /**
     * 🌲 森林生態系統模板
     *
     * 特色：
     * - 原版草地（適合樹木生長）
     * - 灰土土壤
     * - 避免水下替換
     * - 中等優先級
     */
    public static EcosystemConfig.Builder forest() {
        return EcosystemConfig.builder()
                .surfaceBlock(() -> Blocks.GRASS_BLOCK)
                .soilBlock(() -> Blocks.PODZOL)
                .deepSoilBlock(() -> Blocks.COARSE_DIRT, 15)
                .waterRules(WaterRules.AVOID_WATER)
                .priority(5);
    }

    /**
     * ❄️ 雪原生態系統模板
     *
     * 特色：
     * - 雪塊地表
     * - 冰層底層
     * - 凍土深層
     * - 避免水下替換
     * - 中等優先級
     */
    public static EcosystemConfig.Builder snowy() {
        return EcosystemConfig.builder()
                .surfaceBlock(() -> Blocks.SNOW_BLOCK)
                .soilBlock(() -> Blocks.PACKED_ICE)
                .deepSoilBlock(() -> Blocks.BLUE_ICE, 25)
                .waterRules(WaterRules.AVOID_WATER)
                .priority(7);
    }

    /**
     * 🍄 蘑菇島生態系統模板
     *
     * 特色：
     * - 菌絲地表
     * - 菌絲土壤
     * - 允許水下生長
     * - 中等優先級
     */
    public static EcosystemConfig.Builder mushroom() {
        return EcosystemConfig.builder()
                .surfaceBlock(() -> Blocks.MYCELIUM)
                .soilBlock(() -> Blocks.MYCELIUM)
                .deepSoilBlock(() -> Blocks.DIRT, 20)
                .waterRules(WaterRules.ALLOW_UNDERWATER)
                .priority(6);
    }

    /**
     * 🏔️ 山脈生態系統模板
     *
     * 特色：
     * - 石頭地表（高海拔）
     * - 多層石頭系統
     * - 礫石土壤
     * - 避免水下替換
     * - 中低優先級
     */
    public static EcosystemConfig.Builder mountains() {
        return EcosystemConfig.builder()
                .surfaceBlock(() -> Blocks.STONE)
                .soilBlock(() -> Blocks.GRAVEL)
                .stoneBlock(() -> Blocks.DEEPSLATE, 0)
                .waterRules(WaterRules.AVOID_WATER)
                .priority(4);
    }

    /**
     * 🌊 海洋生態系統模板
     *
     * 特色：
     * - 沙子地表
     * - 允許水下替換
     * - 海底沙石結構
     * - 低優先級
     */
    public static EcosystemConfig.Builder ocean() {
        return EcosystemConfig.builder()
                .surfaceBlock(() -> Blocks.SAND)
                .soilBlock(() -> Blocks.SAND)
                .deepSoilBlock(() -> Blocks.GRAVEL, 40)
                .waterRules(WaterRules.ALLOW_UNDERWATER)
                .priority(3);
    }

    /**
     * 💎 洞窟生態系統模板
     *
     * 特色：
     * - 方解石地表
     * - 紫水晶底層
     * - 允許水下生長（地下湖泊）
     * - 高優先級
     */
    public static EcosystemConfig.Builder cave() {
        return EcosystemConfig.builder()
                .surfaceBlock(() -> Blocks.CALCITE)
                .soilBlock(() -> Blocks.CALCITE)
                .stoneBlock(() -> Blocks.AMETHYST_BLOCK, 10)
                .waterRules(WaterRules.ALLOW_UNDERWATER)
                .priority(9);
    }

    /**
     * 🌿 茂盛生態系統模板
     *
     * 特色：
     * - 苔蘚地表
     * - 根土土壤
     * - 靠近水源生長
     * - 中高優先級
     */
    public static EcosystemConfig.Builder lush() {
        return EcosystemConfig.builder()
                .surfaceBlock(() -> Blocks.MOSS_BLOCK)
                .soilBlock(() -> Blocks.ROOTED_DIRT)
                .deepSoilBlock(() -> Blocks.DIRT, 15)
                .waterRules(WaterRules.ONLY_NEAR_WATER)
                .priority(7);
    }

    // ===============================
    // 🎨 組合式模板方法
    // ===============================

    /**
     * 🎨 創建基於現有模板的變種
     *
     * @param baseTemplate 基礎模板
     * @param customizer 自定義函數
     * @return 自定義後的建構器
     */
    public static EcosystemConfig.Builder createVariant(
            EcosystemConfig.Builder baseTemplate,
            java.util.function.Function<EcosystemConfig.Builder, EcosystemConfig.Builder> customizer) {
        return customizer.apply(baseTemplate);
    }

    /**
     * 🌈 創建彩色版本的生態系統
     *
     * 將任何生態系統轉換為更豐富多彩的版本
     */
    public static EcosystemConfig.Builder makeColorful(EcosystemConfig.Builder base) {
        return base.priority(base.build().priority() + 1); // 提高優先級
    }

    /**
     * ❄️ 創建冰凍版本的生態系統
     *
     * 將任何生態系統轉換為冰凍變種
     */
    public static EcosystemConfig.Builder makeFrozen(EcosystemConfig.Builder base) {
        return EcosystemConfig.builder()
                .surfaceBlock(() -> Blocks.SNOW_BLOCK)
                .soilBlock(() -> Blocks.PACKED_ICE)
                .deepSoilBlock(() -> Blocks.BLUE_ICE, 20)
                .waterRules(WaterRules.AVOID_WATER)
                .priority(base.build().priority() + 2); // 冰凍版本優先級更高
    }

    /**
     * 🌿 創建豐茂版本的生態系統
     *
     * 將任何生態系統轉換為更茂盛的版本
     */
    public static EcosystemConfig.Builder makeLush(EcosystemConfig.Builder base) {
        return base
                .soilBlock(() -> Blocks.MOSS_BLOCK)
                .deepSoilBlock(() -> Blocks.ROOTED_DIRT, 15)
                .waterRules(WaterRules.ONLY_NEAR_WATER)
                .priority(base.build().priority() + 1);
    }

    /**
     * 🔥 創建火山版本的生態系統
     *
     * 將任何生態系統轉換為火山變種
     */
    public static EcosystemConfig.Builder makeVolcanic(EcosystemConfig.Builder base) {
        return base
                .stoneBlock(() -> Blocks.BASALT, 5)
                .waterRules(WaterRules.REPLACE_WITH_LAVA)
                .priority(base.build().priority() + 3); // 火山版本最高優先級
    }

    // ===============================
    // 🛠️ 自定義方塊模板方法
    // ===============================

    /**
     * 🎯 創建自定義草原（使用自定義方塊）
     *
     * @param grassBlock 自定義草地方塊
     * @param soilBlock 自定義土壤方塊
     * @param deepSoilBlock 自定義深層土壤方塊
     */
    public static EcosystemConfig.Builder customPlains(
            Supplier<Block> grassBlock,
            Supplier<Block> soilBlock,
            Supplier<Block> deepSoilBlock) {
        return EcosystemConfig.builder()
                .surfaceBlock(grassBlock)
                .soilBlock(soilBlock)
                .deepSoilBlock(deepSoilBlock, 20)
                .waterRules(WaterRules.AVOID_WATER)
                .priority(8); // 自定義版本較高優先級
    }

    /**
     * 🎯 創建自定義沙漠（使用自定義方塊）
     *
     * @param sandBlock 自定義沙子方塊
     * @param sandstoneBlock 自定義砂岩方塊
     */
    public static EcosystemConfig.Builder customDesert(
            Supplier<Block> sandBlock,
            Supplier<Block> sandstoneBlock) {
        return EcosystemConfig.builder()
                .surfaceBlock(sandBlock)
                .soilBlock(sandstoneBlock)
                .deepSoilBlock(sandstoneBlock, 30)
                .waterRules(WaterRules.ONLY_NEAR_WATER)
                .priority(8);
    }

    /**
     * 🎯 創建完全自定義的生態系統
     *
     * @param surfaceBlock 地表方塊
     * @param soilBlock 土壤方塊
     * @param deepSoilBlock 深層土壤方塊
     * @param stoneBlock 石頭方塊
     * @param waterRule 水規則
     * @param priority 優先級
     */
    public static EcosystemConfig.Builder fullyCustom(
            Supplier<Block> surfaceBlock,
            Supplier<Block> soilBlock,
            Supplier<Block> deepSoilBlock,
            Supplier<Block> stoneBlock,
            WaterRules waterRule,
            int priority) {
        return EcosystemConfig.builder()
                .surfaceBlock(surfaceBlock)
                .soilBlock(soilBlock)
                .deepSoilBlock(deepSoilBlock, 20)
                .stoneBlock(stoneBlock, 5)
                .waterRules(waterRule)
                .priority(priority);
    }
}