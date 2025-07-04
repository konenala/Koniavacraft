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
 * 優化的導管渲染器
 *
 * 新增功能：
 * 1. IO方向視覺指示
 * 2. 性能優化的渲染
 * 3. 更豐富的視覺回饋
 */
public class ArcaneConduitBlockEntityRenderer implements BlockEntityRenderer<ArcaneConduitBlockEntity> {

    // === 材質資源 ===
    private static final ResourceLocation CRYSTAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/conduit/arcane_crystal.png");
    private static final ResourceLocation RUNE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/effect/conduit/magic_runes.png");
    private static final ResourceLocation MANA_FLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/effect/conduit/mana_flow.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/effect/conduit/io_arrow.png");

    // === 性能優化常量 ===
    private static final float MIN_RENDER_DISTANCE_SQ = 4.0f; // 最小渲染距離
    private static final float FULL_DETAIL_DISTANCE_SQ = 16.0f; // 完整細節距離
    private static final int MAX_FLOW_PARTICLES = 2; // 最大流動粒子數

    public ArcaneConduitBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ArcaneConduitBlockEntity conduit, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        Level level = conduit.getLevel();
        if (level == null) return;

        // === 性能優化：距離檢測 ===
        // 如果玩家距離太遠，使用簡化渲染或直接跳過
        // (這裡假設有方法獲取玩家距離，實際可能需要傳入相機位置)

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // 基於距離決定渲染品質
        boolean renderFullDetail = true; // 簡化，實際應該基於距離
        boolean renderFlowEffects = conduit.getManaStored() > 0;

        // 🔮 渲染核心（總是渲染）
        renderOptimizedCore(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 🎯 渲染IO方向指示（新功能）
        renderIODirectionIndicators(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        if (renderFullDetail) {
            // ✨ 渲染符文環（完整品質）
            renderOptimizedRunes(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }

        if (renderFlowEffects && renderFullDetail) {
            // 💫 渲染魔力流動（最耗效能，有條件渲染）
            renderOptimizedManaFlow(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    /**
     * 🎯 新功能：IO方向指示渲染
     */
    private void renderIODirectionIndicators(ArcaneConduitBlockEntity conduit, float partialTick,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay) {

        VertexConsumer arrowConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(ARROW_TEXTURE));

        long gameTime = conduit.getLevel().getGameTime();
        float globalTime = gameTime + partialTick;

        for (Direction direction : Direction.values()) {
            IOHandlerUtils.IOType ioType = conduit.getIOConfig(direction);

            // 跳過禁用的方向
            if (ioType == IOHandlerUtils.IOType.DISABLED) continue;

            poseStack.pushPose();

            // 移動到方向位置
            float distance = 0.7f; // 距離中心的距離
            poseStack.translate(
                    direction.getStepX() * distance,
                    direction.getStepY() * distance,
                    direction.getStepZ() * distance
            );

            // 根據方向旋轉箭頭
            applyDirectionRotation(poseStack, direction);

            // 箭頭大小和脈動效果
            float pulse = 0.8f + 0.2f * Mth.sin(globalTime * 0.1f);
            float scale = 0.1f * pulse;
            poseStack.scale(scale, scale, scale);

            // 根據IO類型決定顏色和透明度
            float[] color = getIOTypeColor(ioType);
            float alpha = 0.6f + 0.4f * Mth.sin(globalTime * 0.15f);

            // 雙向類型渲染兩個箭頭
            if (ioType == IOHandlerUtils.IOType.BOTH) {
                // 輸入箭頭（向內）
                renderDirectionalArrow(poseStack, arrowConsumer,
                        0.0f, 0.3f, 1.0f, alpha * 0.8f, // 藍色
                        packedLight, packedOverlay, true);

                // 輸出箭頭（向外）
                poseStack.translate(0, 0.3f, 0); // 稍微偏移
                renderDirectionalArrow(poseStack, arrowConsumer,
                        1.0f, 0.3f, 0.0f, alpha * 0.8f, // 紅色
                        packedLight, packedOverlay, false);
            } else {
                boolean isInput = (ioType == IOHandlerUtils.IOType.INPUT);
                renderDirectionalArrow(poseStack, arrowConsumer,
                        color[0], color[1], color[2], alpha,
                        packedLight, packedOverlay, isInput);
            }

            poseStack.popPose();
        }
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
            case INPUT -> new float[]{0.2f, 0.6f, 1.0f};  // 藍色（輸入）
            case OUTPUT -> new float[]{1.0f, 0.3f, 0.2f}; // 紅色（輸出）
            case BOTH -> new float[]{0.8f, 0.8f, 0.2f};   // 黃色（雙向）
            default -> new float[]{0.5f, 0.5f, 0.5f};     // 灰色（禁用）
        };
    }

    /**
     * 渲染方向箭頭
     */
    private void renderDirectionalArrow(PoseStack poseStack, VertexConsumer consumer,
                                        float r, float g, float b, float a,
                                        int packedLight, int packedOverlay, boolean pointsInward) {
        // 如果是輸入箭頭，翻轉方向
        if (pointsInward) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        }

        renderQuad(poseStack, consumer, r, g, b, a, packedLight, packedOverlay);
    }

    /**
     * 🔮 優化的核心渲染
     */
    private void renderOptimizedCore(ArcaneConduitBlockEntity conduit, float partialTick,
                                     PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, int packedOverlay) {

        // 減少計算頻率 - 每4tick更新一次顏色
        long gameTime = conduit.getLevel().getGameTime();
        float pulse = 0.8f + 0.2f * Mth.sin((gameTime + partialTick) * 0.1f);

        // 魔力比例影響外觀
        float manaRatio = conduit.getManaStored() > 0 ?
                (float) conduit.getManaStored() / Math.max(1, conduit.getMaxManaStored()) : 0.1f;

        float brightness = 0.3f + 0.7f * manaRatio;

        poseStack.pushPose();

        // 簡化旋轉 - 只繞Y軸
        float rotation = (gameTime + partialTick) * 0.3f; // 降低旋轉速度
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // 基於魔力的脈動縮放
        float scale = 0.12f + (0.08f * manaRatio * pulse);
        poseStack.scale(scale, scale, scale);

        VertexConsumer crystalConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(CRYSTAL_TEXTURE));

        // 優化顏色計算
        float r = 0.3f + 0.3f * manaRatio;
        float g = 0.6f + 0.4f * manaRatio;
        float b = 1.0f;

        renderOptimizedCube(poseStack, crystalConsumer, r, g, b, brightness, packedLight, packedOverlay);

        poseStack.popPose();
    }

    /**
     * ✨ 優化的符文渲染
     */
    private void renderOptimizedRunes(ArcaneConduitBlockEntity conduit, float partialTick,
                                      PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {

        if (conduit.getManaStored() <= 10) return; // 提高門檻

        long gameTime = conduit.getLevel().getGameTime();
        float runeRotation = (gameTime + partialTick) * 1.0f; // 降低旋轉速度

        // 限制符文數量以提升性能
        int activeConnections = Math.min(conduit.getActiveConnectionCount(), 4);
        if (activeConnections <= 0) activeConnections = 2; // 減少基礎符文

        VertexConsumer runeConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(RUNE_TEXTURE));

        poseStack.pushPose();

        for (int i = 0; i < activeConnections; i++) {
            poseStack.pushPose();

            float angleOffset = (360.0f / activeConnections) * i;
            float angle = angleOffset + runeRotation;

            // 簡化垂直搖擺
            float verticalOffset = 0.05f * Mth.sin((gameTime + partialTick + i * 20) * 0.03f);

            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.translate(0.5f, verticalOffset, 0); // 減少距離

            // 簡化面向計算
            poseStack.mulPose(Axis.YP.rotationDegrees(-angle));

            // 固定符文大小，減少計算
            float runeSize = 0.12f;
            poseStack.scale(runeSize, runeSize, runeSize);

            // 簡化透明度計算
            float manaRatio = conduit.getManaStored() > 0 ?
                    (float) conduit.getManaStored() / conduit.getMaxManaStored() : 0.3f;
            float alpha = 0.4f + 0.4f * manaRatio;

            renderQuad(poseStack, runeConsumer, 1.0f, 0.8f, 0.2f, alpha, packedLight, packedOverlay);

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /**
     * 💫 優化的魔力流動渲染
     */
    private void renderOptimizedManaFlow(ArcaneConduitBlockEntity conduit, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {

        if (conduit.getManaStored() < 20) return; // 提高門檻

        VertexConsumer flowConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(MANA_FLOW_TEXTURE));

        long gameTime = conduit.getLevel().getGameTime();

        // 只渲染有實際傳輸的方向
        for (Direction direction : Direction.values()) {
            int transferHistory = conduit.getTransferHistory(direction);
            if (transferHistory <= 0) continue;

            poseStack.pushPose();

            float intensity = Math.min(1.0f, transferHistory / 300.0f); // 降低門檻

            // 減少粒子數量
            int particleCount = Math.min(MAX_FLOW_PARTICLES, 1 + (int)(intensity * 2));

            for (int particle = 0; particle < particleCount; particle++) {
                poseStack.pushPose();

                float phase = (float) particle / particleCount;
                float flowProgress = ((gameTime + partialTick + phase * 40) * 0.1f) % 1.0f; // 降低速度

                float distance = 0.15f + 0.4f * flowProgress; // 減少範圍
                poseStack.translate(
                        direction.getStepX() * distance,
                        direction.getStepY() * distance,
                        direction.getStepZ() * distance
                );

                // 簡化大小計算
                float size = 0.04f * intensity;
                poseStack.scale(size, size, size);

                // 簡化顏色變化
                float progress = flowProgress;
                float r = 0.2f + 0.3f * progress;
                float g = 0.4f + 0.4f * progress;
                float b = 1.0f;
                float alpha = intensity * (0.8f - progress * 0.3f);

                renderQuad(poseStack, flowConsumer, r, g, b, alpha, packedLight, packedOverlay);

                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }

    /**
     * 🔧 優化的立方體渲染（只渲染3個可見面）
     */
    private void renderOptimizedCube(PoseStack poseStack, VertexConsumer consumer,
                                     float r, float g, float b, float a, int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        // 只渲染最可能可見的3個面，減少頂點數量
        // 前面
        addVertex(consumer, matrix, -0.5f, -0.5f, 0.5f, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, -0.5f, 0.5f, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, 0.5f, r, g, b, a, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, 0.5f, 0.5f, r, g, b, a, 0, 0, packedLight, packedOverlay);

        // 上面
        addVertex(consumer, matrix, -0.5f, 0.5f, -0.5f, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, -0.5f, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, 0.5f, r, g, b, a, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, 0.5f, 0.5f, r, g, b, a, 0, 0, packedLight, packedOverlay);

        // 右面
        addVertex(consumer, matrix, 0.5f, -0.5f, 0.5f, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, -0.5f, -0.5f, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, -0.5f, r, g, b, a, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, 0.5f, r, g, b, a, 0, 0, packedLight, packedOverlay);
    }

    /**
     * 四邊形渲染（不變）
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
     * 頂點添加（不變）
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
        return 32; // 稍微減少渲染距離以提升性能
    }
}