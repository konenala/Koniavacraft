package com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.render;

import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.ManaPylonBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 魔力塔渲染器 - 渲染能量特效和魔力流動
 */
public class ManaPylonRenderer implements BlockEntityRenderer<ManaPylonBlockEntity> {

    private static final ResourceLocation BEAM_TEXTURE = ResourceLocation.parse("textures/entity/beacon_beam.png");

    public ManaPylonRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ManaPylonBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        // 只在魔力塔有魔力時渲染特效
        float fillRatio = (float) blockEntity.getStoredMana() / blockEntity.getMaxManaCapacity();
        if (fillRatio <= 0.1f) {
            return; // 魔力太少時不渲染
        }

        poseStack.pushPose();

        // 渲染頂部水晶發光效果
        renderCrystalGlow(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 渲染能量流動效果
        if (blockEntity.isConnectedToNetwork()) {
            renderEnergyFlow(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    /**
     * 渲染水晶發光效果
     */
    private void renderCrystalGlow(ManaPylonBlockEntity blockEntity, float partialTick,
                                  PoseStack poseStack, MultiBufferSource bufferSource,
                                  int packedLight, int packedOverlay) {

        float glowIntensity = blockEntity.getCrystalGlow();
        float animationPhase = blockEntity.getEnergyAnimation();

        poseStack.pushPose();

        // 移動到水晶位置
        poseStack.translate(0.5D, 1.0D, 0.5D);

        // 脈動效果
        float pulse = 1.0f + 0.1f * (float)Math.sin(animationPhase * 4);
        poseStack.scale(pulse, pulse, pulse);

        // 慢速旋轉
        poseStack.mulPose(Axis.YP.rotationDegrees(animationPhase * 20));

        // 這裡可以渲染自定義的發光立方體或粒子效果
        // 由於簡化，我們使用基本的渲染邏輯

        poseStack.popPose();
    }

    /**
     * 渲染能量流動效果
     */
    private void renderEnergyFlow(ManaPylonBlockEntity blockEntity, float partialTick,
                                 PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay) {

        float animationPhase = blockEntity.getEnergyAnimation();

        poseStack.pushPose();

        // 移動到塔底部
        poseStack.translate(0.5D, 0.1D, 0.5D);

        // 創建螺旋上升的能量流
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        // 簡化的能量線條渲染
        for (int i = 0; i < 20; i++) {
            float height = i * 0.05f;
            float angle = animationPhase + i * 0.3f;
            float radius = 0.3f * (1.0f - height / 1.0f); // 向上收縮

            float x1 = radius * (float)Math.cos(angle);
            float z1 = radius * (float)Math.sin(angle);
            float y1 = height;

            float x2 = radius * (float)Math.cos(angle + 0.3f);
            float z2 = radius * (float)Math.sin(angle + 0.3f);
            float y2 = height + 0.05f;

            // 繪製能量線段
            consumer.addVertex(matrix, x1, y1, z1)
                    .setColor(0.3f, 0.7f, 1.0f, 0.8f);
            consumer.addVertex(matrix, x2, y2, z2)
                    .setColor(0.3f, 0.7f, 1.0f, 0.6f);
        }

        poseStack.popPose();
    }
}
