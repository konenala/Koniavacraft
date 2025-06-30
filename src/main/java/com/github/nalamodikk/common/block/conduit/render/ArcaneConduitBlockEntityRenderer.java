package com.github.nalamodikk.common.block.conduit.render;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.conduit.ArcaneConduitBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

public class ArcaneConduitBlockEntityRenderer implements BlockEntityRenderer<ArcaneConduitBlockEntity> {

    // 修復：材質路徑和你的項目結構匹配
    private static final ResourceLocation CRYSTAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/arcane_crystal.png");
    private static final ResourceLocation RUNE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/effect/magic_runes.png");
    private static final ResourceLocation MANA_FLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/effect/mana_flow.png");

    public ArcaneConduitBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ArcaneConduitBlockEntity conduit, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        Level level = conduit.getLevel();
        if (level == null) return;

        poseStack.pushPose();

        // 移動到方塊中心
        poseStack.translate(0.5, 0.5, 0.5);

        // 🔮 渲染發光水晶核心
        renderCrystalCore(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // ✨ 渲染旋轉符文
        renderRotatingRunes(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 💫 渲染魔力流動效果
        renderManaFlow(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void renderCrystalCore(ArcaneConduitBlockEntity conduit, float partialTick,
                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay) {

        // 水晶脈動效果
        long gameTime = conduit.getLevel().getGameTime();
        float pulse = (float) (0.8 + 0.2 * Math.sin((gameTime + partialTick) * 0.1));

        // 魔力量影響亮度和大小
        float manaRatio = Math.max(0.1f, (float) conduit.getManaStored() / Math.max(1, conduit.getMaxManaStored()));
        float brightness = 0.3f + 0.7f * manaRatio;

        poseStack.pushPose();

        // 緩慢旋轉
        float rotation = (gameTime + partialTick) * 0.5f;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation * 0.7f)); // 添加多軸旋轉

        // 脈動縮放，基於魔力量
        float scale = 0.15f + (0.15f * manaRatio * pulse);
        poseStack.scale(scale, scale, scale);

        // 渲染發光水晶
        VertexConsumer crystalConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(CRYSTAL_TEXTURE));

        // 藍色水晶顏色，亮度基於魔力
        float r = 0.3f + 0.3f * manaRatio;
        float g = 0.6f + 0.4f * manaRatio;
        float b = 1.0f;

        renderCube(poseStack, crystalConsumer, r, g, b, brightness, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void renderRotatingRunes(ArcaneConduitBlockEntity conduit, float partialTick,
                                     PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, int packedOverlay) {

        // 只在有魔力時顯示符文
        if (conduit.getManaStored() <= 0) return;

        long gameTime = conduit.getLevel().getGameTime();
        float runeRotation = (gameTime + partialTick) * 1.5f;

        // 獲取活躍連接數決定符文數量
        int activeConnections = conduit.getActiveConnectionCount();
        if (activeConnections <= 0) {
            // 如果沒有外部連接，顯示基礎符文
            activeConnections = 3;
        }

        VertexConsumer runeConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(RUNE_TEXTURE));

        poseStack.pushPose();

        // 符文環繞核心旋轉
        int runeCount = Math.min(activeConnections, 6);
        for (int i = 0; i < runeCount; i++) {
            poseStack.pushPose();

            float angleOffset = (360.0f / runeCount) * i;
            float angle = angleOffset + runeRotation;

            // 垂直搖擺效果
            float verticalOffset = 0.1f * (float) Math.sin((gameTime + partialTick + i * 20) * 0.05);

            poseStack.mulPose(Axis.YP.rotationDegrees(angle));

            // 距離核心0.6格，添加垂直偏移
            poseStack.translate(0.6f, verticalOffset, 0);

            // 始終面向玩家（廣告牌效果）
            poseStack.mulPose(Axis.YP.rotationDegrees(-angle));

            // 符文大小變化
            float runeSize = 0.15f + 0.05f * (float) Math.sin((gameTime + partialTick + i * 10) * 0.1);
            poseStack.scale(runeSize, runeSize, runeSize);

            // 符文透明度根據魔力量變化
            float manaRatio = (float) conduit.getManaStored() / Math.max(1, conduit.getMaxManaStored());
            float alpha = 0.4f + 0.6f * manaRatio;

            // 金色符文
            renderQuad(poseStack, runeConsumer, 1.0f, 0.8f, 0.2f, alpha, packedLight, packedOverlay);

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderManaFlow(ArcaneConduitBlockEntity conduit, float partialTick,
                                PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {

        // 只在有足夠魔力時顯示流動效果
        if (conduit.getManaStored() < 10) return;

        VertexConsumer flowConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(MANA_FLOW_TEXTURE));

        long gameTime = conduit.getLevel().getGameTime();

        // 為每個有傳輸歷史的方向渲染流動效果
        for (Direction direction : Direction.values()) {
            int transferHistory = conduit.getTransferHistory(direction);

            // 即使沒有歷史，也顯示基礎的魔力流動
            if (transferHistory <= 0 && conduit.getManaStored() < 50) continue;

            poseStack.pushPose();

            // 計算流動強度
            float intensity = Math.max(0.3f, Math.min(1.0f, transferHistory / 500.0f));

            // 多個粒子流動效果
            for (int particle = 0; particle < 3; particle++) {
                poseStack.pushPose();

                // 流動動畫，每個粒子有不同的相位
                float phase = (float) particle / 3.0f;
                float flowProgress = ((gameTime + partialTick + phase * 60) * 0.15f) % 1.0f;

                // 沿著方向移動
                float distance = 0.2f + 0.5f * flowProgress;
                poseStack.translate(
                        direction.getStepX() * distance,
                        direction.getStepY() * distance,
                        direction.getStepZ() * distance
                );

                // 粒子大小隨距離和強度變化
                float size = 0.06f * (1.0f - flowProgress * 0.5f) * intensity;
                poseStack.scale(size, size, size);

                // 顏色：藍色能量球，隨流動進度變化
                float r = 0.2f + 0.5f * flowProgress;
                float g = 0.4f + 0.6f * flowProgress;
                float b = 1.0f;
                float alpha = intensity * (1.0f - flowProgress * 0.7f);

                renderQuad(poseStack, flowConsumer, r, g, b, alpha, packedLight, packedOverlay);

                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }

    // 🔧 簡化的立方體渲染（性能友好）
    private void renderCube(PoseStack poseStack, VertexConsumer consumer,
                            float r, float g, float b, float a, int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        // 使用 6 個面但簡化的方法
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
    }

    private void renderQuad(PoseStack poseStack, VertexConsumer consumer,
                            float r, float g, float b, float a, int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        // 渲染一個四邊形 (順時針頂點順序)
        addVertex(consumer, matrix, -0.5f, -0.5f, 0, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, -0.5f, 0, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, 0, r, g, b, a, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, 0.5f, 0, r, g, b, a, 0, 0, packedLight, packedOverlay);
    }

    // 🔧 完整的頂點數據
    private void addVertex(VertexConsumer consumer, Matrix4f matrix,
                           float x, float y, float z,
                           float r, float g, float b, float a,
                           float u, float v, int packedLight, int packedOverlay) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(packedOverlay)  // UV1 - 覆蓋層座標
                .setLight(packedLight)      // UV2 - 光照座標
                .setNormal(0, 0, 1);        // 法線向量
    }

    @Override
    public int getViewDistance() {
        return 48; // 適中的渲染距離，性能友好
    }
}