package com.github.nalamodikk.experimental.particle;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import com.github.nalamodikk.experimental.particle.client.ParticleShaderHandler;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static net.minecraft.client.renderer.RenderStateShard.*;

/**
 * 能量爆發粒子系統 - 實現發光粒子雲效果
 * 特色：中心明亮核心 + 散射粒子雲 + 動態色彩漸變
 */
public class EnergyBurstParticleSystem {


    /**
     * 核心發光渲染類型
     */
    private static final RenderType ENERGY_CORE = RenderType.create(
            "koniava_energy_core",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            65536, false, true,
            RenderType.CompositeState.builder()
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)  // 加法混合產生發光效果
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ParticleShaderHandler.energyBurstShader))
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)  // 只寫顏色，不寫深度
                    .createCompositeState(false)
    );

    /**
     * 粒子雲渲染類型
     */
    private static final RenderType PARTICLE_CLOUD = RenderType.create(
            "koniava_particle_cloud",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            65536, false, true,
            RenderType.CompositeState.builder()
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ParticleShaderHandler.particleCloudShader))
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
    );

    /**
     * 渲染完整的能量爆發效果
     */
    public static void renderEnergyBurst(PoseStack poseStack, MultiBufferSource bufferSource,
                                         Vec3 center, float time, EnergyBurstParams params) {
        
        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);

        // 1. 渲染核心發光
        renderEnergyCore(poseStack, bufferSource, time, params);
        
        // 2. 渲染粒子雲
        renderParticleCloud(poseStack, bufferSource, time, params);
        
        poseStack.popPose();
    }

    /**
     * 渲染發光核心
     */
    private static void renderEnergyCore(PoseStack poseStack, MultiBufferSource bufferSource,
                                         float time, EnergyBurstParams params) {
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(ENERGY_CORE);
        Matrix4f matrix = poseStack.last().pose();

        // 設置著色器參數
        if (ParticleShaderHandler.energyBurstShader != null) {
            ParticleShaderHandler.energyBurstShader.safeGetUniform("GameTime").set(time);
            ParticleShaderHandler.energyBurstShader.safeGetUniform("CoreSize").set(params.coreSize);
            ParticleShaderHandler.energyBurstShader.safeGetUniform("CoreIntensity").set(params.coreIntensity);
            ParticleShaderHandler.energyBurstShader.safeGetUniform("PulseSpeed").set(params.pulseSpeed);
            ParticleShaderHandler.energyBurstShader.safeGetUniform("CoreColor").set(
                    (float) params.coreColor.x, (float) params.coreColor.y, (float) params.coreColor.z);
        }

        // 渲染核心四邊形
        float coreSize = params.coreSize * (1.0f + 0.2f * (float) Math.sin(time * params.pulseSpeed));
        renderQuad(vertexConsumer, matrix, coreSize, 0xFFFFFF, params.coreIntensity);
    }

    /**
     * 渲染粒子雲
     */
    private static void renderParticleCloud(PoseStack poseStack, MultiBufferSource bufferSource,
                                            float time, EnergyBurstParams params) {
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(PARTICLE_CLOUD);
        Matrix4f matrix = poseStack.last().pose();

        // 設置粒子雲著色器參數
        if (ParticleShaderHandler.particleCloudShader != null) {
            ParticleShaderHandler.particleCloudShader.safeGetUniform("GameTime").set(time);
            ParticleShaderHandler.particleCloudShader.safeGetUniform("ParticleCount").set(params.particleCount);
            ParticleShaderHandler.particleCloudShader.safeGetUniform("CloudRadius").set(params.cloudRadius);
            ParticleShaderHandler.particleCloudShader.safeGetUniform("ParticleSize").set(params.particleSize);
            ParticleShaderHandler.particleCloudShader.safeGetUniform("CloudDensity").set(params.cloudDensity);
            ParticleShaderHandler.particleCloudShader.safeGetUniform("AnimationSpeed").set(params.animationSpeed);
            
            // 顏色漸變參數
            ParticleShaderHandler.particleCloudShader.safeGetUniform("InnerColor").set(
                    (float) params.innerColor.x, (float) params.innerColor.y, (float) params.innerColor.z);
            ParticleShaderHandler.particleCloudShader.safeGetUniform("OuterColor").set(
                    (float) params.outerColor.x, (float) params.outerColor.y, (float) params.outerColor.z);
        }

        // 渲染覆蓋整個粒子雲區域的四邊形
        float cloudSize = params.cloudRadius * 2.5f;
        renderTexturedQuad(vertexConsumer, matrix, cloudSize, 0xFFFFFF, 0.8f);
    }

    /**
     * 渲染簡單四邊形
     */
    private static void renderQuad(VertexConsumer vertexConsumer, Matrix4f matrix,
                                   float size, int color, float alpha) {
        float halfSize = size * 0.5f;
        int alphaInt = (int)(alpha * 255);
        int finalColor = color | (alphaInt << 24);

        vertexConsumer.addVertex(matrix, -halfSize, 0, -halfSize).setColor(finalColor);
        vertexConsumer.addVertex(matrix, -halfSize, 0, halfSize).setColor(finalColor);
        vertexConsumer.addVertex(matrix, halfSize, 0, halfSize).setColor(finalColor);
        vertexConsumer.addVertex(matrix, halfSize, 0, -halfSize).setColor(finalColor);
    }

    /**
     * 渲染帶紋理坐標的四邊形
     */
    private static void renderTexturedQuad(VertexConsumer vertexConsumer, Matrix4f matrix,
                                           float size, int color, float alpha) {
        float halfSize = size * 0.5f;
        int alphaInt = (int)(alpha * 255);
        int finalColor = color | (alphaInt << 24);

        vertexConsumer.addVertex(matrix, -halfSize, 0, -halfSize).setColor(finalColor).setUv(0, 0);
        vertexConsumer.addVertex(matrix, -halfSize, 0, halfSize).setColor(finalColor).setUv(0, 1);
        vertexConsumer.addVertex(matrix, halfSize, 0, halfSize).setColor(finalColor).setUv(1, 1);
        vertexConsumer.addVertex(matrix, halfSize, 0, -halfSize).setColor(finalColor).setUv(1, 0);
    }

    /**
     * 能量爆發參數配置
     */
    public static class EnergyBurstParams {
        // 核心參數
        public float coreSize = 0.3f;              // 核心大小
        public float coreIntensity = 1.5f;         // 核心亮度
        public float pulseSpeed = 2.0f;            // 脈動速度
        public Vec3 coreColor = new Vec3(1.0, 0.9, 0.7);  // 核心顏色（偏白黃）

        // 粒子雲參數
        public int particleCount = 150;            // 粒子數量
        public float cloudRadius = 2.0f;           // 雲團半徑
        public float particleSize = 0.05f;         // 單個粒子大小
        public float cloudDensity = 0.7f;          // 雲團密度
        public float animationSpeed = 1.0f;        // 動畫速度

        // 顏色漸變
        public Vec3 innerColor = new Vec3(0.9, 0.8, 1.0);  // 內層顏色（偏白藍）
        public Vec3 outerColor = new Vec3(0.4, 0.2, 0.8);  // 外層顏色（深紫藍）

        /**
         * 根據能量等級創建參數
         */
        public static EnergyBurstParams fromEnergyLevel(int energyLevel) {
            EnergyBurstParams params = new EnergyBurstParams();

            // 能量越高，效果越強烈
            float intensity = Math.min(energyLevel / 10000.0f, 2.0f);
            
            params.coreSize = 0.2f + intensity * 0.3f;
            params.coreIntensity = 1.0f + intensity * 0.8f;
            params.cloudRadius = 1.5f + intensity * 1.0f;
            params.particleCount = (int)(100 + intensity * 100);
            params.cloudDensity = 0.5f + intensity * 0.4f;

            // 高能量時顏色更偏向白金色
            if (energyLevel > 20000) {
                params.coreColor = new Vec3(1.0, 1.0, 0.8);      // 明亮白金
                params.innerColor = new Vec3(1.0, 0.9, 0.7);     // 淺金
                params.outerColor = new Vec3(0.6, 0.4, 0.9);     // 紫藍
            }

            return params;
        }

        /**
         * 創建魔力釋放效果參數
         */
        public static EnergyBurstParams manaReleaseEffect() {
            EnergyBurstParams params = new EnergyBurstParams();
            
            params.coreSize = 0.4f;
            params.coreIntensity = 2.0f;
            params.pulseSpeed = 3.0f;
            params.cloudRadius = 2.5f;
            params.particleCount = 200;
            params.animationSpeed = 1.5f;
            
            // 經典魔力藍紫色調
            params.coreColor = new Vec3(0.8, 0.9, 1.0);
            params.innerColor = new Vec3(0.6, 0.8, 1.0);
            params.outerColor = new Vec3(0.3, 0.1, 0.7);
            
            return params;
        }
    }

    /**
     * 便捷方法：渲染魔力爆發效果
     */
    public static void renderManaExplosion(PoseStack poseStack, MultiBufferSource bufferSource,
                                           Vec3 center, float time, int manaAmount) {
        EnergyBurstParams params = EnergyBurstParams.fromEnergyLevel(manaAmount);
        renderEnergyBurst(poseStack, bufferSource, center, time, params);
    }

    /**
     * 便捷方法：渲染魔力釋放效果
     */
    public static void renderManaRelease(PoseStack poseStack, MultiBufferSource bufferSource,
                                         Vec3 center, float time) {
        EnergyBurstParams params = EnergyBurstParams.manaReleaseEffect();
        renderEnergyBurst(poseStack, bufferSource, center, time, params);
    }
}