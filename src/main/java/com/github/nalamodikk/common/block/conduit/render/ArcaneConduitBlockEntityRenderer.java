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

    // 材質資源位置
    private static final ResourceLocation CRYSTAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/conduit/arcane_crystal.png");
    private static final ResourceLocation RUNE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/effect/conduit/magic_runes.png");
    private static final ResourceLocation MANA_FLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/effect/conduit/mana_flow.png");

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

        // 魔力量影響亮度
        float manaRatio = (float) conduit.getManaStored() / conduit.getMaxManaStored();
        float brightness = 0.3f + 0.7f * manaRatio;

        poseStack.pushPose();

        // 緩慢旋轉
        float rotation = (gameTime + partialTick) * 0.5f;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // 脈動縮放
        float scale = 0.3f * pulse;
        poseStack.scale(scale, scale, scale);

        // 渲染發光水晶
        VertexConsumer crystalConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(CRYSTAL_TEXTURE));

        renderCube(poseStack, crystalConsumer, 1.0f, 1.0f, 1.0f, brightness, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void renderRotatingRunes(ArcaneConduitBlockEntity conduit, float partialTick,
                                     PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, int packedOverlay) {

        // 只在有魔力時顯示符文
        if (conduit.getManaStored() <= 0) return;

        long gameTime = conduit.getLevel().getGameTime();
        float runeRotation = (gameTime + partialTick) * 2.0f;

        // 獲取活躍連接數決定符文數量
        int activeConnections = conduit.getActiveConnectionCount();
        if (activeConnections <= 0) return;

        VertexConsumer runeConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(RUNE_TEXTURE));

        poseStack.pushPose();

        // 符文環繞核心旋轉
        for (int i = 0; i < Math.min(activeConnections, 6); i++) {
            poseStack.pushPose();

            float angle = (360.0f / activeConnections) * i + runeRotation;
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));

            // 距離核心0.8格
            poseStack.translate(0.8f, 0, 0);

            // 始終面向玩家（廣告牌效果）
            poseStack.mulPose(Axis.YP.rotationDegrees(-angle));

            // 符文大小
            float runeSize = 0.2f;
            poseStack.scale(runeSize, runeSize, runeSize);

            // 符文透明度根據魔力量變化
            float alpha = 0.5f + 0.5f * ((float) conduit.getManaStored() / conduit.getMaxManaStored());

            renderQuad(poseStack, runeConsumer, 1.0f, 0.8f, 0.2f, alpha, packedLight, packedOverlay);

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderManaFlow(ArcaneConduitBlockEntity conduit, float partialTick,
                                PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {

        // 只在傳輸魔力時顯示流動效果
        if (conduit.getManaStored() <= 0) return;

        VertexConsumer flowConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(MANA_FLOW_TEXTURE));

        long gameTime = conduit.getLevel().getGameTime();

        // 為每個有傳輸歷史的方向渲染流動效果
        for (Direction direction : Direction.values()) {
            int transferHistory = conduit.getTransferHistory(direction);
            if (transferHistory <= 0) continue;

            poseStack.pushPose();

            // 計算流動強度（基於傳輸歷史）
            float intensity = Math.min(1.0f, transferHistory / 1000.0f);
            if (intensity < 0.1f) {
                poseStack.popPose();
                continue;
            }

            // 流動動畫
            float flowProgress = ((gameTime + partialTick) * 0.2f) % 1.0f;

            // 沿著方向移動
            float distance = 0.3f + 0.4f * flowProgress;
            poseStack.translate(
                    direction.getStepX() * distance,
                    direction.getStepY() * distance,
                    direction.getStepZ() * distance
            );

            // 粒子大小隨距離變化
            float size = 0.1f * (1.0f - flowProgress) * intensity;
            poseStack.scale(size, size, size);

            // 顏色：藍色到白色漸變
            float r = 0.3f + 0.7f * flowProgress;
            float g = 0.6f + 0.4f * flowProgress;
            float b = 1.0f;
            float alpha = intensity * (1.0f - flowProgress);

            renderQuad(poseStack, flowConsumer, r, g, b, alpha, packedLight, packedOverlay);

            poseStack.popPose();
        }
    }

    // 🔧 修復：正確的立方體渲染方法
    private void renderCube(PoseStack poseStack, VertexConsumer consumer,
                            float r, float g, float b, float a, int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        // 簡化的立方體渲染 - 只渲染前面作為示例
        // 前面 (使用三角形帶渲染四邊形)
        addVertex(consumer, matrix, -0.5f, -0.5f, 0.5f, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, -0.5f, 0.5f, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, 0.5f, 0.5f, r, g, b, a, 0, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, 0.5f, r, g, b, a, 1, 0, packedLight, packedOverlay);
    }

    private void renderQuad(PoseStack poseStack, VertexConsumer consumer,
                            float r, float g, float b, float a, int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        // 渲染一個四邊形 (使用三角形帶)
        addVertex(consumer, matrix, -0.5f, -0.5f, 0, r, g, b, a, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, -0.5f, 0, r, g, b, a, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, -0.5f, 0.5f, 0, r, g, b, a, 0, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, 0.5f, 0.5f, 0, r, g, b, a, 1, 0, packedLight, packedOverlay);
    }

    // 🔧 修復：完整的頂點數據
    private void addVertex(VertexConsumer consumer, Matrix4f matrix,
                           float x, float y, float z,
                           float r, float g, float b, float a,
                           float u, float v, int packedLight, int packedOverlay) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(packedOverlay)  // 🔥 添加覆蓋層UV
                .setLight(packedLight)      // 🔥 光照信息
                .setNormal(0, 0, 1);        // 🔥 法線向量
    }

    @Override
    public int getViewDistance() {
        return 64; // 渲染距離
    }
}