package com.github.nalamodikk.mixin;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.biome.ModSurfaceRulesHandler;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 🌍 地表規則混入 - 安全版本，帶完整錯誤處理
 *
 * 🎯 功能：
 * - 攔截原版的 overworld() 方法
 * - 安全地注入自定義規則
 * - 如果出錯不會導致遊戲崩潰
 */
@Mixin(SurfaceRuleData.class)
public class SurfaceRuleDataMixin {

    /**
     * 🎯 注入點：overworld() 方法的返回處
     *
     * 使用最安全的方式，確保不會因為初始化問題而崩潰
     */
    @Inject(
            method = "overworld",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void injectKoniavacraftSurfaceRules(CallbackInfoReturnable<SurfaceRules.RuleSource> cir) {
        SurfaceRules.RuleSource originalRules = null;
        SurfaceRules.RuleSource modRules = null;

        try {
            // 🔧 獲取原版的地表規則
            originalRules = cir.getReturnValue();

            if (originalRules == null) {
                KoniavacraftMod.LOGGER.warn("⚠️ 原版地表規則為空，跳過注入");
                return;
            }

            // 🌍 獲取我們的自定義規則（帶安全檢查）
            modRules = ModSurfaceRulesHandler.getModSurfaceRules();

            if (modRules == null) {
                KoniavacraftMod.LOGGER.warn("⚠️ 模組地表規則為空，跳過注入");
                return;
            }

            // 🎯 組合規則：我們的規則優先，然後是原版規則
            SurfaceRules.RuleSource combinedRules = SurfaceRules.sequence(
                    modRules,      // 我們的規則先執行
                    originalRules  // 原版規則作為後備
            );

            // 🔄 設置新的返回值
            cir.setReturnValue(combinedRules);

            // 📝 成功日誌
            KoniavacraftMod.LOGGER.info("✅ Koniavacraft 地表規則已成功注入！");

        } catch (IllegalArgumentException e) {
            // 處理 "Need at least 1 rule for a sequence" 錯誤
            KoniavacraftMod.LOGGER.error("❌ 地表規則序列錯誤: {}", e.getMessage());

            // 保持原版規則不變
            if (originalRules != null) {
                cir.setReturnValue(originalRules);
            }

        } catch (RuntimeException e) {
            // 處理 "unbound value" 和其他運行時錯誤
            if (e.getMessage() != null && e.getMessage().contains("unbound")) {
                KoniavacraftMod.LOGGER.warn("⚠️ DeferredHolder 尚未綁定，將在稍後通過事件處理地表方塊");
            } else {
                KoniavacraftMod.LOGGER.error("❌ Koniavacraft SurfaceRules 注入失敗: {}", e.getMessage());
            }

            // 保持原版規則不變
            if (originalRules != null) {
                cir.setReturnValue(originalRules);
            }

        } catch (Exception e) {
            // 處理所有其他異常
            KoniavacraftMod.LOGGER.error("❌ 未預期的錯誤: {}", e.getMessage(), e);

            // 保持原版規則不變
            if (originalRules != null) {
                cir.setReturnValue(originalRules);
            }
        }
    }
}