package com.github.nalamodikk.common.block.conduit.render;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.conduit.ArcaneConduitBlock;
import com.github.nalamodikk.common.block.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.joml.Matrix4f;

/**
 * 增強版導管渲染器 - 添加 Mek 風格的 IO 形狀指示器
 *
 * 新增功能：
 * 1. 🔵 輸入端：圓形凹槽指示器
 * 2. 🔴 輸出端：方形突出指示器
 * 3. ⚪ 雙向端：菱形指示器
 * 4. ❌ 禁用端：不顯示指示器
 *
 * 保留原有功能：
 * - 核心發光效果
 * - 性能優化
 */
public class ArcaneConduitBlockEntityRenderer implements BlockEntityRenderer<ArcaneConduitBlockEntity> {

    // === 材質資源 ===
    private static final ResourceLocation CRYSTAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/conduit/arcane_crystal.png");

    public ArcaneConduitBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    // 1. 🎨 更豐富的顏色方案
    private float[] getIOTypeColor(IOHandlerUtils.IOType ioType, boolean isActive) {
        float brightness = isActive ? 1.0f : 0.6f; // 活躍狀態更亮

        return switch (ioType) {
            case INPUT -> new float[]{0.2f * brightness, 0.8f * brightness, 1.0f * brightness};   // 藍色
            case OUTPUT -> new float[]{1.0f * brightness, 0.3f * brightness, 0.2f * brightness};  // 紅色
            case BOTH -> new float[]{0.2f * brightness, 1.0f * brightness, 0.3f * brightness};    // 綠色
            default -> new float[]{0.5f * brightness, 0.5f * brightness, 0.5f * brightness};      // 灰色
        };
    }


    // 5. ⚡ 性能優化版本
    private void renderOptimizedIOIndicators(ArcaneConduitBlockEntity conduit, float partialTick,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay) {

        // 距離檢查 - 遠距離時不渲染細節
        double distanceToPlayer = getDistanceToPlayer(conduit);
        if (distanceToPlayer > 16) return; // 16格外不渲染

        boolean renderHighDetail = distanceToPlayer < 8; // 8格內高品質渲染

        for (Direction direction : Direction.values()) {
            IOHandlerUtils.IOType ioType = conduit.getIOConfig(direction);
            if (ioType == IOHandlerUtils.IOType.DISABLED) continue;

            boolean isConnected = isConnected(conduit.getBlockState(), direction);
            if (!isConnected) continue;

            if (renderHighDetail) {
                renderDetailedIOIndicator(poseStack, bufferSource, direction, ioType,
                        partialTick, packedLight, packedOverlay);
            } else {
                renderSimpleIOIndicator(poseStack, bufferSource, direction, ioType,
                        partialTick, packedLight, packedOverlay);
            }
        }
    }

    // === 🎯 使用您現有系統的完整渲染方法 ===
    @Override
    public void render(ArcaneConduitBlockEntity conduit, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        Level level = conduit.getLevel();
        if (level == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // 🔮 您的核心渲染 (保持不變)
        renderCleanCore(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 🎯 您的 Mek 風格 IO 指示器 (保持不變)
        renderMekStyleIOIndicators(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 🚀 新增功能 (可選)
        if (shouldRenderEnhancements()) {
            renderManaFlowAnimation(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            renderPriorityNumbers(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    // 2. 🔄 優先級可視化 (數字顯示)
    private void renderPriorityIndicator(PoseStack poseStack, MultiBufferSource bufferSource,
                                         Direction direction, int priority,
                                         int packedLight, int packedOverlay) {
        if (priority == 0) return; // 不顯示默認優先級

        poseStack.pushPose();

        // 移動到指示器旁邊
        moveToDirectionEnd(poseStack, direction);
        poseStack.translate(0, 0.15f, 0); // 稍微上移

        // 渲染優先級數字
        String priorityText = String.valueOf(priority);
        float scale = 0.01f;

        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(180)); // 面向玩家

        // 使用 Minecraft 的字體渲染系統
        // (這裡需要 Minecraft.getInstance().font)

        poseStack.popPose();
    }

    // 3. 🌊 魔力流動動畫
    private void renderManaFlowAnimation(ArcaneConduitBlockEntity conduit, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {

        // 獲取傳輸統計
        var transferStats = conduit.getTransferStats();
        long gameTime = conduit.getLevel().getGameTime();

        for (Direction dir : Direction.values()) {
            int transferHistory = conduit.getTransferHistory(dir);
            if (transferHistory > 0) {
                // 渲染魔力粒子流動效果
                renderManaParticles(poseStack, bufferSource, dir, transferHistory,
                        gameTime, partialTick, packedLight, packedOverlay);
            }
        }
    }

    private void renderEnhancedInputIndicator(PoseStack poseStack, VertexConsumer consumer,
                                              float[] color, float pulse, int packedLight, int packedOverlay) {
        // 多層圓環，營造深度感
        float[] radii = {0.06f, 0.08f, 0.10f};
        float[] depths = {0.01f, 0.02f, 0.03f};
        float[] alphas = {0.9f, 0.7f, 0.5f};

        for (int i = 0; i < radii.length; i++) {
            float[] layerColor = {color[0], color[1], color[2], alphas[i] * pulse};
            renderCircularInset(poseStack, consumer, radii[i] * pulse, depths[i],
                    layerColor, packedLight, packedOverlay);
        }
    }



    /**
     * 渲染優先級數字 - 添加到 ArcaneConduitBlockEntityRenderer 中
     */
    private void renderPriorityNumbers(ArcaneConduitBlockEntity conduit, float partialTick,
                                       PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 只有在特定條件下才顯示數字
        boolean shouldShowNumbers = shouldShowPriorityNumbers(conduit, mc.player);
        if (!shouldShowNumbers) return;

        Font font = mc.font;

        for (Direction direction : Direction.values()) {
            IOHandlerUtils.IOType ioType = conduit.getIOConfig(direction);
            if (ioType == IOHandlerUtils.IOType.DISABLED) continue;

            boolean isConnected = isConnected(conduit.getBlockState(), direction);
            if (!isConnected) continue;

            int priority = conduit.getPriority(direction);
            if (priority == 0) continue; // 不顯示默認優先級

            poseStack.pushPose();

            // 移動到方向末端
            moveToDirectionEnd(poseStack, direction);

            // 稍微偏移避免與IO指示器重疊
            switch (direction) {
                case UP, DOWN -> poseStack.translate(0.2f, 0, 0);
                default -> poseStack.translate(0, 0.2f, 0);
            }

            // 面向玩家
            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180));

            // 縮放
            float scale = 0.015f;
            poseStack.scale(-scale, -scale, scale);

            // 準備文字
            String text = formatPriorityNumber(priority);
            int color = getPriorityTextColor(priority);

            // 渲染背景
            int textWidth = font.width(text);
            int bgColor = 0x80000000; // 半透明黑色背景

            // 渲染文字
            font.drawInBatch(text, -textWidth / 2f, -4, color, false,
                    poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.NORMAL, bgColor, packedLight);

            poseStack.popPose();
        }
    }

    /**
     * 決定是否顯示優先級數字
     */
    private boolean shouldShowPriorityNumbers(ArcaneConduitBlockEntity conduit, Player player) {
        // 1. 距離檢查
        double distance = player.distanceToSqr(conduit.getBlockPos().getCenter());
        if (distance > 64) return false; // 8格外不顯示

        // 2. 手持科技魔杖時顯示
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof BasicTechWandItem) {
            return true;
        }

        // 3. 潛行時顯示
        if (player.isCrouching()) {
            return true;
        }

        // 4. 特殊模式下顯示 (可通過配置開關)
        return false;
    }

    /**
     * 格式化優先級數字顯示
     */
    private String formatPriorityNumber(int priority) {
        if (priority == 0) return "";

        // 大數值使用簡化顯示
        if (Math.abs(priority) >= 1_000_000) {
            return String.format("%.1fM", priority / 1_000_000.0);
        } else if (Math.abs(priority) >= 1_000) {
            return String.format("%.1fK", priority / 1_000.0);
        } else {
            return String.valueOf(priority);
        }
    }

    /**
     * 根據優先級數值選擇顏色
     */
    private int getPriorityTextColor(int priority) {
        if (priority > 1000) {
            return 0xFF55FF55; // 亮綠色 - 超高優先級
        } else if (priority > 100) {
            return 0xFF55FFFF; // 青色 - 高優先級
        } else if (priority > 0) {
            return 0xFFFFFFFF; // 白色 - 正常優先級
        } else if (priority > -100) {
            return 0xFFFFFF55; // 黃色 - 低優先級
        } else {
            return 0xFFFF5555; // 紅色 - 很低優先級
        }
    }

    /**
     * 🔮 簡潔的核心渲染 - 保持原有實現
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
     * 🎯 新增：渲染 Mek 風格的 IO 指示器
     */
    private void renderMekStyleIOIndicators(ArcaneConduitBlockEntity conduit, float partialTick,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay) {

        Level level = conduit.getLevel();
        if (level == null) return;

        BlockState state = level.getBlockState(conduit.getBlockPos());

        // 檢查每個方向是否有連接
        for (Direction direction : Direction.values()) {
            if (isConnected(state, direction)) {
                IOHandlerUtils.IOType ioType = conduit.getIOConfig(direction);
                if (ioType != IOHandlerUtils.IOType.DISABLED) {
                    renderIOIndicator(poseStack, bufferSource, direction, ioType,
                            partialTick, packedLight, packedOverlay);
                }
            }
        }
    }

    /**
     * 🎨 渲染單個方向的 IO 指示器
     */
    private void renderIOIndicator(PoseStack poseStack, MultiBufferSource bufferSource,
                                   Direction direction, IOHandlerUtils.IOType ioType,
                                   float partialTick, int packedLight, int packedOverlay) {

        poseStack.pushPose();

        // 移動到管道末端
        moveToDirectionEnd(poseStack, direction);

        // 獲取 IO 類型對應的顏色
        float[] color = getIOTypeColor(ioType);

        // 輕微的脈動效果
        long gameTime = System.currentTimeMillis();
        float pulse = 0.8f + 0.2f * Mth.sin(gameTime * 0.003f + direction.ordinal());

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());

        // 根據 IO 類型渲染不同形狀
        switch (ioType) {
            case INPUT -> renderInputIndicator(poseStack, consumer, color, pulse, packedLight, packedOverlay);
            case OUTPUT -> renderOutputIndicator(poseStack, consumer, color, pulse, packedLight, packedOverlay);
            case BOTH -> renderBothIndicator(poseStack, consumer, color, pulse, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    /**
     * 🔵 渲染輸入指示器 - 圓形凹槽（Mek 風格）
     */
    private void renderInputIndicator(PoseStack poseStack, VertexConsumer consumer,
                                      float[] color, float pulse, int packedLight, int packedOverlay) {
        // 圓形凹槽，表示"接收"
        float radius = 0.08f * pulse;
        float depth = 0.02f;

        // 渲染內凹的圓環
        renderCircularInset(poseStack, consumer, radius, depth, color, packedLight, packedOverlay);
    }

    /**
     * 🔴 渲染輸出指示器 - 方形突出（Mek 風格）
     */
    private void renderOutputIndicator(PoseStack poseStack, VertexConsumer consumer,
                                       float[] color, float pulse, int packedLight, int packedOverlay) {
        // 方形突出，表示"推送"
        float size = 0.06f * pulse;
        float height = 0.03f;

        // 渲染突出的方塊
        renderSquareProtrusion(poseStack, consumer, size, height, color, packedLight, packedOverlay);
    }

    /**
     * ⚪ 渲染雙向指示器 - 菱形（改良的 Mek 風格）
     */
    private void renderBothIndicator(PoseStack poseStack, VertexConsumer consumer,
                                     float[] color, float pulse, int packedLight, int packedOverlay) {
        // 菱形，表示"雙向"
        float size = 0.07f * pulse;

        // 渲染菱形指示器
        renderDiamondIndicator(poseStack, consumer, size, color, packedLight, packedOverlay);
    }

    /**
     * 🔵 圓形凹槽渲染
     */
    private void renderCircularInset(PoseStack poseStack, VertexConsumer consumer,
                                     float radius, float depth, float[] color,
                                     int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();
        int segments = 16; // 圓形精度

        // 渲染圓環
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (2 * Math.PI * i / segments);
            float angle2 = (float) (2 * Math.PI * (i + 1) / segments);

            float x1 = Mth.cos(angle1) * radius;
            float y1 = Mth.sin(angle1) * radius;
            float x2 = Mth.cos(angle2) * radius;
            float y2 = Mth.sin(angle2) * radius;

            // 外圈
            addVertex(consumer, matrix, x1, y1, 0, color[0], color[1], color[2], 0.8f, 0, 0, packedLight, packedOverlay);
            addVertex(consumer, matrix, x2, y2, 0, color[0], color[1], color[2], 0.8f, 1, 0, packedLight, packedOverlay);
            addVertex(consumer, matrix, x2, y2, -depth, color[0], color[1], color[2], 0.8f, 1, 1, packedLight, packedOverlay);
            addVertex(consumer, matrix, x1, y1, -depth, color[0], color[1], color[2], 0.8f, 0, 1, packedLight, packedOverlay);
        }
    }

    /**
     * 🔴 方形突出渲染
     */
    private void renderSquareProtrusion(PoseStack poseStack, VertexConsumer consumer,
                                        float size, float height, float[] color,
                                        int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        // 渲染突出的方塊
        // 前面
        addVertex(consumer, matrix, -size, -size, 0, color[0], color[1], color[2], 0.8f, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, -size, 0, color[0], color[1], color[2], 0.8f, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, size, 0, color[0], color[1], color[2], 0.8f, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, -size, size, 0, color[0], color[1], color[2], 0.8f, 0, 0, packedLight, packedOverlay);

        // 頂面
        addVertex(consumer, matrix, -size, size, 0, color[0], color[1], color[2], 0.8f, 0, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, size, 0, color[0], color[1], color[2], 0.8f, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, size, height, color[0], color[1], color[2], 0.8f, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, -size, size, height, color[0], color[1], color[2], 0.8f, 0, 1, packedLight, packedOverlay);

        // 側面（簡化，只渲染兩個）
        addVertex(consumer, matrix, size, -size, 0, color[0], color[1], color[2], 0.8f, 0, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, size, 0, color[0], color[1], color[2], 0.8f, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, size, height, color[0], color[1], color[2], 0.8f, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, -size, height, color[0], color[1], color[2], 0.8f, 0, 1, packedLight, packedOverlay);
    }

    /**
     * 計算到玩家的距離
     */
    private double getDistanceToPlayer(ArcaneConduitBlockEntity conduit) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return Double.MAX_VALUE;

        return mc.player.distanceToSqr(conduit.getBlockPos().getCenter());
    }

    /**
     * 是否應該渲染增強效果
     */
    private boolean shouldRenderEnhancements() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        // 可以根據配置、距離、性能模式等決定
        return true; // 簡單實現，總是渲染
    }

    /**
     * 渲染詳細的IO指示器（近距離）
     */
    private void renderDetailedIOIndicator(PoseStack poseStack, MultiBufferSource bufferSource,
                                           Direction direction, IOHandlerUtils.IOType ioType,
                                           float partialTick, int packedLight, int packedOverlay) {

        poseStack.pushPose();
        moveToDirectionEnd(poseStack, direction);

        // 獲取活躍狀態（可以根據實際傳輸狀態決定）
        boolean isActive = true; // 簡化實現
        float[] color = getIOTypeColor(ioType, isActive);

        long gameTime = System.currentTimeMillis();
        float pulse = 0.8f + 0.2f * Mth.sin(gameTime * 0.003f + direction.ordinal());

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());

        // 渲染增強版的形狀（更多細節）
        switch (ioType) {
            case INPUT -> renderEnhancedInputIndicator(poseStack, consumer, color, pulse, packedLight, packedOverlay);
            case OUTPUT -> renderOutputIndicator(poseStack, consumer, color, pulse, packedLight, packedOverlay);
            case BOTH -> renderBothIndicator(poseStack, consumer, color, pulse, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    /**
     * 渲染簡單的IO指示器（遠距離）
     */
    private void renderSimpleIOIndicator(PoseStack poseStack, MultiBufferSource bufferSource,
                                         Direction direction, IOHandlerUtils.IOType ioType,
                                         float partialTick, int packedLight, int packedOverlay) {

        poseStack.pushPose();
        moveToDirectionEnd(poseStack, direction);

        float[] color = getIOTypeColor(ioType);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());

        // 簡化的渲染，只用顏色區分
        renderSimpleColoredSquare(poseStack, consumer, color, 0.05f, packedLight, packedOverlay);

        poseStack.popPose();
    }

    /**
     * 渲染簡單的彩色方塊（遠距離用）
     */
    private void renderSimpleColoredSquare(PoseStack poseStack, VertexConsumer consumer,
                                           float[] color, float size, int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        // 簡單的方形面
        addVertex(consumer, matrix, -size, -size, 0.01f, color[0], color[1], color[2], 0.7f, 0, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, -size, 0.01f, color[0], color[1], color[2], 0.7f, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, size, 0.01f, color[0], color[1], color[2], 0.7f, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, -size, size, 0.01f, color[0], color[1], color[2], 0.7f, 0, 1, packedLight, packedOverlay);
    }

    /**
     * 渲染魔力粒子效果
     */
    private void renderManaParticles(PoseStack poseStack, MultiBufferSource bufferSource,
                                     Direction direction, int transferHistory,
                                     long gameTime, float partialTick,
                                     int packedLight, int packedOverlay) {

        // 根據傳輸歷史決定粒子數量和強度
        int particleCount = Math.min(transferHistory / 100, 5); // 最多5個粒子
        if (particleCount <= 0) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());

        for (int i = 0; i < particleCount; i++) {
            poseStack.pushPose();

            // 沿著方向移動粒子
            float progress = ((gameTime + i * 200) % 1000) / 1000.0f; // 1秒循環
            moveAlongDirection(poseStack, direction, progress);

            // 渲染小的發光粒子
            float size = 0.02f + 0.01f * Mth.sin(gameTime * 0.01f + i);
            float[] particleColor = {0.5f, 0.8f, 1.0f}; // 魔力藍色

            renderSimpleColoredSquare(poseStack, consumer, particleColor, size, packedLight, packedOverlay);

            poseStack.popPose();
        }
    }

    /**
     * 沿著指定方向移動
     */
    private void moveAlongDirection(PoseStack poseStack, Direction direction, float progress) {
        // 從中心到邊緣
        float distance = progress * 0.5f;

        switch (direction) {
            case NORTH -> poseStack.translate(0, 0, -distance);
            case SOUTH -> poseStack.translate(0, 0, distance);
            case WEST -> poseStack.translate(-distance, 0, 0);
            case EAST -> poseStack.translate(distance, 0, 0);
            case UP -> poseStack.translate(0, distance, 0);
            case DOWN -> poseStack.translate(0, -distance, 0);
        }
    }


    /**
     * ⚪ 菱形指示器渲染
     */

    private void renderDiamondIndicator(PoseStack poseStack, VertexConsumer consumer,
                                        float size, float[] color,
                                        int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        // 菱形的四個頂點
        float[][] vertices = {
                {0, size, 0.01f},    // 上
                {size, 0, 0.01f},    // 右
                {0, -size, 0.01f},   // 下
                {-size, 0, 0.01f}    // 左
        };

        // 渲染菱形面（修復版）
        addVertex(consumer, matrix, vertices[0][0], vertices[0][1], vertices[0][2],
                color[0], color[1], color[2], 0.8f, 0.5f, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, vertices[1][0], vertices[1][1], vertices[1][2],
                color[0], color[1], color[2], 0.8f, 1, 0.5f, packedLight, packedOverlay);
        addVertex(consumer, matrix, vertices[2][0], vertices[2][1], vertices[2][2],
                color[0], color[1], color[2], 0.8f, 0.5f, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, vertices[3][0], vertices[3][1], vertices[3][2],
                color[0], color[1], color[2], 0.8f, 0, 0.5f, packedLight, packedOverlay);
    }
    /**
     * 🎯 移動到指定方向的末端
     */
    private void moveToDirectionEnd(PoseStack poseStack, Direction direction) {
        float offset = 0.51f; // 略微超出方塊邊界

        switch (direction) {
            case NORTH -> poseStack.translate(0, 0, -offset);
            case SOUTH -> poseStack.translate(0, 0, offset);
            case WEST -> poseStack.translate(-offset, 0, 0);
            case EAST -> poseStack.translate(offset, 0, 0);
            case UP -> poseStack.translate(0, offset, 0);
            case DOWN -> poseStack.translate(0, -offset, 0);
        }

        // 旋轉使指示器朝向正確方向
        switch (direction) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> { /* 默認方向 */ }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
        }
    }

    /**
     * 🎨 獲取 IO 類型對應的顏色
     */
    private float[] getIOTypeColor(IOHandlerUtils.IOType ioType) {
        return switch (ioType) {
            case INPUT -> new float[]{0.3f, 0.6f, 1.0f};   // 藍色
            case OUTPUT -> new float[]{1.0f, 0.4f, 0.3f};  // 紅色
            case BOTH -> new float[]{0.2f, 1.0f, 0.4f};    // 綠色
            default -> new float[]{0.5f, 0.5f, 0.5f};      // 灰色
        };
    }

    /**
     * 🔗 檢查指定方向是否有連接
     */
    private boolean isConnected(BlockState state, Direction direction) {
        if (!(state.getBlock() instanceof ArcaneConduitBlock)) {
            return false; // 不是導管方塊就返回 false
        }
        BooleanProperty property = switch (direction) {
            case NORTH -> ArcaneConduitBlock.NORTH;
            case SOUTH -> ArcaneConduitBlock.SOUTH;
            case WEST -> ArcaneConduitBlock.WEST;
            case EAST -> ArcaneConduitBlock.EAST;
            case UP -> ArcaneConduitBlock.UP;
            case DOWN -> ArcaneConduitBlock.DOWN;
        };

        return state.getValue(property);
    }

    // === 保持原有的輔助方法 ===

    /**
     * 🔧 簡化的立方體渲染（保持不變）
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
     * 頂點添加（保持不變）
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