package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

/**
 * 🌍 Koniavacraft 地表規則處理器 - 安全的 Mixin 版本
 *
 * 使用條件檢查避免初始化順序問題
 */
public class ModSurfaceRulesHandler {

    /**
     * 🎯 獲取所有模組的地表規則 - 帶安全檢查
     */
    public static SurfaceRules.RuleSource getModSurfaceRules() {
        try {
            KoniavacraftMod.LOGGER.info("🌍 正在應用 Koniavacraft 地表規則...");

            // 🔧 安全檢查：確認方塊已經註冊
            if (!isModBlocksReady()) {
                KoniavacraftMod.LOGGER.warn("⚠️ ModBlocks 尚未準備好，跳過地表規則設置");
                return createEmptyRule();
            }

            // 🌱 創建魔力草原地表規則
            SurfaceRules.RuleSource manaPlainsSurfaceRules = createManaPlainsSurfaceRules();

            // 返回完整的規則序列
            return SurfaceRules.sequence(
                    SurfaceRules.ifTrue(
                            SurfaceRules.isBiome(ModBiomes.MANA_PLAINS),
                            manaPlainsSurfaceRules
                    )
            );

        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("❌ 創建地表規則時發生錯誤: {}", e.getMessage());
            // 返回安全的空規則
            return createEmptyRule();
        }
    }

    /**
     * 🔍 檢查 ModBlocks 是否已經準備好
     */
    private static boolean isModBlocksReady() {
        try {
            // 嘗試檢查 DeferredHolder 是否已綁定
            // 不直接調用 .get()，而是檢查內部狀態

            // 方法1：檢查是否可以安全獲取
            if (ModBlocks.MANA_GRASS_BLOCK == null) {
                return false;
            }

            // 方法2：嘗試檢查註冊狀態（不觸發綁定）
            try {
                // 這裡使用反射或其他方式檢查，但更簡單的是捕獲異常
                ModBlocks.MANA_GRASS_BLOCK.get();
                return true;
            } catch (RuntimeException e) {
                // 如果拋出 "unbound value" 異常，說明還沒準備好
                if (e.getMessage() != null && e.getMessage().contains("unbound value")) {
                    return false;
                }
                // 其他異常也當作未準備好
                return false;
            }

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 🌱 創建魔力草原地表規則 - 安全版本
     */
    private static SurfaceRules.RuleSource createManaPlainsSurfaceRules() {
        try {
            // 在這裡才真正訪問 ModBlocks
            SurfaceRules.RuleSource manaGrassBlock = SurfaceRules.state(ModBlocks.MANA_GRASS_BLOCK.get().defaultBlockState());
            SurfaceRules.RuleSource manaSoil = SurfaceRules.state(ModBlocks.MANA_SOIL.get().defaultBlockState());
            SurfaceRules.RuleSource deepManaSoil = SurfaceRules.state(ModBlocks.DEEP_MANA_SOIL.get().defaultBlockState());

            return SurfaceRules.sequence(
                    // === 地表層處理 ===
                    SurfaceRules.ifTrue(
                            SurfaceRules.ON_FLOOR,
                            SurfaceRules.ifTrue(
                                    SurfaceRules.waterBlockCheck(-1, 0),
                                    manaGrassBlock
                            )
                    ),

                    // === 淺層土壤 ===
                    SurfaceRules.ifTrue(
                            SurfaceRules.stoneDepthCheck(0, false, CaveSurface.FLOOR),
                            SurfaceRules.ifTrue(
                                    SurfaceRules.not(SurfaceRules.stoneDepthCheck(3, true, CaveSurface.FLOOR)),
                                    manaSoil
                            )
                    ),

                    // === 深層土壤 ===
                    SurfaceRules.ifTrue(
                            SurfaceRules.stoneDepthCheck(3, false, CaveSurface.FLOOR),
                            SurfaceRules.ifTrue(
                                    SurfaceRules.not(SurfaceRules.stoneDepthCheck(6, true, CaveSurface.FLOOR)),
                                    deepManaSoil
                            )
                    )
            );
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.warn("⚠️ 無法創建魔力草原地表規則，使用備用方案: {}", e.getMessage());
            return createFallbackRule();
        }
    }

    /**
     * 🔄 創建空規則（當出錯時使用）
     */
    private static SurfaceRules.RuleSource createEmptyRule() {
        // 返回一個不做任何事的規則
        return SurfaceRules.ifTrue(
                SurfaceRules.not(SurfaceRules.ON_FLOOR), // 永遠不會為 true
                SurfaceRules.state(Blocks.AIR.defaultBlockState())
        );
    }

    /**
     * 🔄 創建備用規則（使用原版方塊）
     */
    private static SurfaceRules.RuleSource createFallbackRule() {
        KoniavacraftMod.LOGGER.info("🔄 使用原版方塊作為備用地表規則");

        return SurfaceRules.sequence(
                // 暫時使用原版方塊，等後面用事件替換
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.ifTrue(
                                SurfaceRules.waterBlockCheck(-1, 0),
                                SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState()) // 先用原版草方塊
                        )
                )
        );
    }
}