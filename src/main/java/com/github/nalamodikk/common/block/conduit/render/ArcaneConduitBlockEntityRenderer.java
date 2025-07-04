package com.github.nalamodikk.common.block.conduit.render;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * 簡潔的導管渲染器 - 移除粒子效果
 *
 * 保留功能：
 * 1. IO方向視覺指示
 * 2. 核心發光效果
 * 3. 性能優化
 *
 * 移除功能：
 * - 飄浮的符文粒子
 * - 魔力流動粒子
 */
public class ArcaneConduitBlockEntityRenderer implements BlockEntityRenderer<ArcaneConduitBlockEntity> {

    // === 材質資源 ===
    private static final ResourceLocation CRYSTAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/conduit/arcane_crystal.png");

    public ArcaneConduitBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ArcaneConduitBlockEntity conduit, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        Level level = conduit.getLevel();
        if (level == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // 🔮 渲染核心（總是渲染）
        renderCleanCore(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 🎯 渲染IO方向指示

        poseStack.popPose();
    }

    /**
     * 🔮 簡潔的核心渲染 - 只有核心發光，無粒子
     */
    private void renderCleanCore(ArcaneConduitBlockEntity conduit, float partialTick,
                                 PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay) {

        long gameTime = conduit.getLevel().getGameTime();

        // 簡單的脈動效果
        float pulse = 0.9f + 0.1f * Mth.sin((gameTime + partialTick) * 0.08f);

        // 魔力比例影響亮度
        float manaRatio = conduit.getManaStored() > 0 ?
                (float) conduit.getManaStored() / Math.max(1, conduit.getMaxManaStored()) : 0.2f;

        float brightness = 0.3f + 0.5f * manaRatio;

        poseStack.pushPose();

        // 慢速旋轉
        float rotation = (gameTime + partialTick) * 0.2f;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // 基於魔力的輕微縮放
        float scale = 0.15f + (0.05f * manaRatio * pulse);
        poseStack.scale(scale, scale, scale);

        VertexConsumer crystalConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(CRYSTAL_TEXTURE));

        // 根據魔力狀態調整顏色
        float r, g, b;
        if (conduit.getManaStored() > 0) {
            // 有魔力：藍紫色調
            r = 0.4f + 0.2f * manaRatio;
            g = 0.6f + 0.3f * manaRatio;
            b = 1.0f;
        } else {
            // 無魔力：暗淡灰色
            r = 0.3f;
            g = 0.3f;
            b = 0.4f;
            brightness *= 0.3f;
        }

        renderSimpleCube(poseStack, crystalConsumer, r, g, b, brightness, packedLight, packedOverlay);

        poseStack.popPose();
    }
    /**
     * 應用方向旋轉
     */
    private void applyDirectionRotation(PoseStack poseStack, Direction direction) {
        switch (direction) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180));
            case UP -> { /* 默認向上 */ }
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
        }
    }

    /**
     * 獲取IO類型對應的顏色
     */
    private float[] getIOTypeColor(IOHandlerUtils.IOType ioType) {
        return switch (ioType) {
            case INPUT -> new float[]{0.2f, 0.6f, 1.0f};  // 藍色
            case OUTPUT -> new float[]{1.0f, 0.3f, 0.2f}; // 紅色
            case BOTH -> new float[]{0.8f, 0.8f, 0.2f};   // 黃色
            default -> new float[]{0.5f, 0.5f, 0.5f};     // 灰色
        };
    }

    /**
     * 渲染方向箭頭
     */
    private void renderDirectionalArrow(PoseStack poseStack, VertexConsumer consumer,
                                        float r, float g, float b, float a,
                                        int packedLight, int packedOverlay, boolean pointsInward) {
        if (pointsInward) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        }

        renderQuad(poseStack, consumer, r, g, b, a, packedLight, packedOverlay);
    }

    /**
     * 🔧 簡化的立方體渲染
     */
    private void renderSimpleCube(PoseStack poseStack, VertexConsumer consumer,
                                  float r, float g, float b, float a, int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        // 渲染6個面，但保持簡潔
        // 前面
        addVertex(consumer, matrix, -0.5f, -0.5f, 0.5f, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, -0.5f, 0.5f, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, 0.5f, r, g, b, a, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, 0.5f, 0.5f, r, g, b, a, 0, 0, packedLight, packedOverlay);

        // 後面
        addVertex(consumer, matrix, 0.5f, -0.5f, -0.5f, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, -0.5f, -0.5f, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, 0.5f, -0.5f, r, g, b, a, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, -0.5f, r, g, b, a, 0, 0, packedLight, packedOverlay);

        // 上面
        addVertex(consumer, matrix, -0.5f, 0.5f, -0.5f, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, -0.5f, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, 0.5f, r, g, b, a, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, 0.5f, 0.5f, r, g, b, a, 0, 0, packedLight, packedOverlay);

        // 底面
        addVertex(consumer, matrix, -0.5f, -0.5f, 0.5f, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, -0.5f, 0.5f, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, -0.5f, -0.5f, r, g, b, a, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, -0.5f, -0.5f, r, g, b, a, 0, 0, packedLight, packedOverlay);

        // 左面
        addVertex(consumer, matrix, -0.5f, -0.5f, -0.5f, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, -0.5f, 0.5f, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, 0.5f, 0.5f, r, g, b, a, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, 0.5f, -0.5f, r, g, b, a, 0, 0, packedLight, packedOverlay);

        // 右面
        addVertex(consumer, matrix, 0.5f, -0.5f, 0.5f, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, -0.5f, -0.5f, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, -0.5f, r, g, b, a, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, 0.5f, r, g, b, a, 0, 0, packedLight, packedOverlay);
    }

    /**
     * 四邊形渲染
     */
    private void renderQuad(PoseStack poseStack, VertexConsumer consumer,
                            float r, float g, float b, float a, int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        addVertex(consumer, matrix, -0.5f, -0.5f, 0, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, -0.5f, 0, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, 0, r, g, b, a, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, 0.5f, 0, r, g, b, a, 0, 0, packedLight, packedOverlay);
    }

    /**
     * 頂點添加
     */
    private void addVertex(VertexConsumer consumer, Matrix4f matrix,
                           float x, float y, float z,
                           float r, float g, float b, float a,
                           float u, float v, int packedLight, int packedOverlay) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(0, 0, 1);
    }

    @Override
    public int getViewDistance() {
        return 32;
    }
}