// 🔧 Koniavacraft NoiseGeneratorSettings Mixin - 簡潔版
package com.github.nalamodikk.mixin;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.biome.lib.BiomeTerrainLibAPI;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 🎯 Mixin to inject Koniavacraft's custom terrain rules into Surface Rules
 *
 * 簡潔版：在原版規則前面插入自定義規則，讓原版邏輯自然繼續
 */
@Mixin(NoiseGeneratorSettings.class)
public class NoiseGeneratorSettingsMixin {

    @Shadow
    @Final
    private NoiseSettings noiseSettings;

    /**
     * 🔧 在原版規則前面插入 Koniavacraft 的地形規則
     *
     * 策略：修改返回值，不完全攔截
     */
    @Inject(method = "surfaceRule", at = @At("RETURN"), cancellable = true)
    private void injectKoniavaSurfaceRules(CallbackInfoReturnable<SurfaceRules.RuleSource> cir) {
        try {
            // 🌍 只處理主世界
            if (this.noiseSettings.equals(NoiseSettingsAccessor.getOverworldNoiseSettings())) {

                // 🌟 獲取庫的地形規則
                SurfaceRules.RuleSource libraryRules = BiomeTerrainLibAPI.getAllRules();

                if (libraryRules != null) {
                    // 🎯 獲取原版已經處理完的規則
                    SurfaceRules.RuleSource vanillaRules = cir.getReturnValue();

                    // 🚀 在原版規則前面插入你的規則
                    SurfaceRules.RuleSource combinedRules = SurfaceRules.sequence(
                            libraryRules,    // 🌱 你的自定義規則（優先處理）
                            vanillaRules     // 🌍 原版規則（自然後備）
                    );

                    cir.setReturnValue(combinedRules);


                }
            }

            // 🔥 地獄和 🌌 終界自動保持原版行為

        } catch (Exception e) {
            // 🚨 如果出錯，記錄錯誤但不修改返回值，保持原版規則
            KoniavacraftMod.LOGGER.error("❌ Koniavacraft 地形規則注入失敗: {}", e.getMessage(), e);
        }
    }
}