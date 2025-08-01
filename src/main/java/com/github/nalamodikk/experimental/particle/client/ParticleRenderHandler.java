package com.github.nalamodikk.experimental.particle.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.experimental.particle.EnergyBurstParticleSystem;
import com.github.nalamodikk.experimental.particle.item.DebugParticleItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 粒子渲染事件處理器
 * 負責在正確的渲染階段調用粒子系統
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ParticleRenderHandler {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // 在透明物體渲染階段處理粒子效果
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            // 使用 Minecraft 的 renderBuffers 來獲取 MultiBufferSource
            Minecraft mc = Minecraft.getInstance();
            if (mc.levelRenderer != null) {
                renderParticleEffects(event.getPoseStack(), mc.renderBuffers().bufferSource());
            }
        }
    }

    private static void renderParticleEffects(PoseStack poseStack, MultiBufferSource bufferSource) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        float currentTime = (float)(System.currentTimeMillis() % 100000) / 1000.0f;

        // 渲染所有活躍的能量爆發效果
        for (DebugParticleItem.EnergyBurstEffectManager.EffectData effect : 
             DebugParticleItem.EnergyBurstEffectManager.getActiveEffects()) {
            
            float effectTime = currentTime - effect.startTime;
            if (effectTime >= 0 && effectTime <= effect.duration) {
                
                // 計算效果強度（淡入淡出）
                float intensity = 1.0f;
                if (effectTime > effect.duration * 0.7f) {
                    intensity = 1.0f - (effectTime - effect.duration * 0.7f) / (effect.duration * 0.3f);
                }

                // 創建參數並調整強度
                EnergyBurstParticleSystem.EnergyBurstParams params = 
                    EnergyBurstParticleSystem.EnergyBurstParams.manaReleaseEffect();
                params.coreIntensity *= intensity;
                params.cloudDensity *= intensity;

                // 實際渲染調用
                EnergyBurstParticleSystem.renderEnergyBurst(
                    poseStack, bufferSource, effect.position, effectTime, params
                );
            }
        }

        // 清理過期效果
        DebugParticleItem.EnergyBurstEffectManager.tick();
    }
}