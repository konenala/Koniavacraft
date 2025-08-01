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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;

import java.io.IOException;

import static net.minecraft.client.renderer.RenderStateShard.*;

/**
 * 程序化魔法陣渲染系統
 */
public class ProceduralMagicCircle {

    // 自定義著色器程序
    private static ShaderInstance magicCircleShader;

    /**
     * 註冊自定義著色器
     */
    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "magic_circle"),
                        DefaultVertexFormat.POSITION_COLOR
                ),
                shader -> magicCircleShader = shader
        );
    }

    /**
     * 創建程序化魔法陣渲染類型
     */
    private static final RenderType PROCEDURAL_MAGIC_CIRCLE = RenderType.create(
            "koniava_procedural_magic_circle",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            65536, false, true,
            RenderType.CompositeState.builder()
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> magicCircleShader)) // 修正：使用 ShaderStateShard
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
    );

    /**
     * 渲染程序化魔法陣
     */
    public static void renderProceduralMagicCircle(PoseStack poseStack, MultiBufferSource bufferSource,
                                                   Vec3 center, float time, MagicCircleParams params) {

        VertexConsumer vertexConsumer = bufferSource.getBuffer(PROCEDURAL_MAGIC_CIRCLE);
        Matrix4f matrix = poseStack.last().pose();

        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);

        // 傳遞著色器參數
        if (magicCircleShader != null) {
            // 時間參數 - 用於動畫
            magicCircleShader.safeGetUniform("GameTime").set(time);

            // 魔法陣參數
            magicCircleShader.safeGetUniform("CircleRadius").set(params.radius);
            magicCircleShader.safeGetUniform("RingCount").set(params.ringCount);
            magicCircleShader.safeGetUniform("RotationSpeed").set(params.rotationSpeed);
            magicCircleShader.safeGetUniform("Complexity").set(params.complexity);
            magicCircleShader.safeGetUniform("Brightness").set(params.brightness);

            // 顏色參數 - 修正：轉換為 float
            magicCircleShader.safeGetUniform("InnerColor").set(
                    (float) params.innerColor.x, (float) params.innerColor.y, (float) params.innerColor.z);
            magicCircleShader.safeGetUniform("OuterColor").set(
                    (float) params.outerColor.x, (float) params.outerColor.y, (float) params.outerColor.z);
        }

        // 創建一個覆蓋整個魔法陣區域的四邊形
        float size = params.radius * 2.2f; // 稍微大一點確保完整覆蓋
        renderShaderQuad(vertexConsumer, matrix, size, 0xFFFFFF, 1.0f);

        poseStack.popPose();
    }

    /**
     * 渲染著色器四邊形
     */
    private static void renderShaderQuad(VertexConsumer vertexConsumer, Matrix4f matrix,
                                         float size, int color, float alpha) {

        float halfSize = size * 0.5f;
        int alphaInt = (int)(alpha * 255);

        // 四個頂點，著色器會根據位置計算魔法陣圖案
        // 修正：使用正確的 setColor 方法
        vertexConsumer.addVertex(matrix, -halfSize, 0, -halfSize).setColor(color | (alphaInt << 24));
        vertexConsumer.addVertex(matrix, -halfSize, 0, halfSize).setColor(color | (alphaInt << 24));
        vertexConsumer.addVertex(matrix, halfSize, 0, halfSize).setColor(color | (alphaInt << 24));
        vertexConsumer.addVertex(matrix, halfSize, 0, -halfSize).setColor(color | (alphaInt << 24));
    }

    /**
     * 魔法陣參數配置
     */
    public static class MagicCircleParams {
        public float radius = 2.0f;           // 魔法陣半徑
        public int ringCount = 5;             // 圓環數量
        public float rotationSpeed = 0.5f;    // 旋轉速度
        public float complexity = 8.0f;       // 複雜度（符文數量）
        public float brightness = 1.0f;       // 亮度

        public Vec3 innerColor = new Vec3(0.616, 0.275, 0.867); // 紫色
        public Vec3 outerColor = new Vec3(0.231, 0.510, 0.965); // 藍色

        // 根據魔力等級調整參數
        public static MagicCircleParams fromManaLevel(int manaLevel) {
            MagicCircleParams params = new MagicCircleParams();

            // 魔力越高，魔法陣越複雜
            params.ringCount = Math.min(3 + manaLevel / 2000, 12);
            params.complexity = 6.0f + manaLevel * 0.001f;
            params.brightness = 0.8f + Math.min(manaLevel * 0.00005f, 0.4f);

            // 高等級時變成金色
            if (manaLevel > 15000) {
                params.innerColor = new Vec3(1.0, 0.843, 0.0);   // 金色
                params.outerColor = new Vec3(1.0, 0.647, 0.0);   // 橙色
            }

            return params;
        }
    }
}