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
 * 🎨 增強版導管渲染器 - 整合 Mekanism + EnderIO 風格
 *
 * 功能：
 * 🔌 EnderIO 風格連接器 - 只在連接非導管機器時顯示
 * 🎯 Mekanism 風格 IO 指示器 - 顯示 IO 類型
 * 📊 優先級數字顯示 - 手持科技魔杖時顯示
 * ⚡ 核心發光水晶 - 根據魔力狀態變化
 */
public class ArcaneConduitBlockEntityRenderer implements BlockEntityRenderer<ArcaneConduitBlockEntity> {

    // === 材質資源 ===
    private static final ResourceLocation CRYSTAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/conduit/arcane_crystal.png");

    // 連接器尺寸設定
    private static final float CONNECTOR_SIZE = 0.1f;       // 連接器大小
    private static final float CONNECTOR_LENGTH = 0.08f;   // 連接器伸出長度
    private static final float CONDUIT_RADIUS = 0.1875f;   // 導管半徑

    public ArcaneConduitBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // 初始化渲染器
    }

    @Override
    public void render(ArcaneConduitBlockEntity conduit, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        Level level = conduit.getLevel();
        if (level == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // 🔮 核心發光水晶
        renderCleanCore(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 🎯 Mekanism 風格 IO 指示器
        renderMekStyleIOIndicators(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 🔌 EnderIO 風格連接器
        renderEnderIOConnectors(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 🚀 增強功能
        if (shouldRenderEnhancements()) {
            renderManaFlowAnimation(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            renderPriorityNumbers(conduit, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    /**
     * 🔌 渲染 EnderIO 風格連接器
     * 只在連接到非導管機器時顯示
     */
    private void renderEnderIOConnectors(ArcaneConduitBlockEntity conduit, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {

        Level level = conduit.getLevel();
        if (level == null) return;

        BlockState state = level.getBlockState(conduit.getBlockPos());

        for (Direction direction : Direction.values()) {
            if (shouldRenderConnector(conduit, state, direction)) {
                renderConnector(conduit, direction, poseStack, bufferSource,
                        packedLight, packedOverlay, partialTick);
            }
        }
    }

    /**
     * 🔧 修正：判斷是否應該渲染連接器
     */
    private boolean shouldRenderConnector(ArcaneConduitBlockEntity conduit, BlockState state, Direction direction) {
        // 1. 檢查該方向是否有連接
        if (!isConnected(state, direction)) {
            return false;
        }

        // 2. 檢查 IO 配置是否禁用
        IOHandlerUtils.IOType ioType = conduit.getIOConfig(direction);
        if (ioType == IOHandlerUtils.IOType.DISABLED) {
            return false;
        }

        // 3. 🔧 修正：使用正確的方法名
        // 只有連接到非導管的機器才顯示連接器
        return !conduit.isConnectedToConduit(direction);
    }

    /**
     * 🔌 渲染單個連接器
     */
    private void renderConnector(ArcaneConduitBlockEntity conduit, Direction direction,
                                 PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay, float partialTick) {

        poseStack.pushPose();

        // 移動到導管邊緣
        moveToConduitEdge(poseStack, direction);

        // 根據 IO 類型獲取顏色
        IOHandlerUtils.IOType ioType = conduit.getIOConfig(direction);
        float[] color = getConnectorColor(ioType);
        float glowIntensity = calculateConnectorGlow(ioType, conduit, partialTick);

        // 設置連接器朝向
        setupConnectorOrientation(poseStack, direction);

        // 渲染連接器幾何體
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());
        renderConnectorGeometry(poseStack.last().pose(), buffer, color, glowIntensity,
                packedLight, packedOverlay);

        poseStack.popPose();
    }

    /**
     * 移動到導管邊緣（連接器起始位置）
     */
    private void moveToConduitEdge(PoseStack poseStack, Direction direction) {
        float offset = CONDUIT_RADIUS;
        poseStack.translate(
                direction.getStepX() * offset,
                direction.getStepY() * offset,
                direction.getStepZ() * offset
        );
    }

    /**
     * 設置連接器朝向
     */
    private void setupConnectorOrientation(PoseStack poseStack, Direction direction) {
        switch (direction) {
            case UP:
                // 已經是正確方向
                break;
            case DOWN:
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
                break;
            case NORTH:
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                break;
            case SOUTH:
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                break;
            case WEST:
                poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                break;
            case EAST:
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
                break;
        }
    }

    /**
     * 渲染連接器幾何體
     */
    private void renderConnectorGeometry(Matrix4f matrix, VertexConsumer buffer,
                                         float[] color, float glowIntensity,
                                         int packedLight, int packedOverlay) {

        float r = color[0], g = color[1], b = color[2];
        float alpha = 0.9f;

        // 計算發光亮度
        int finalLight = Math.max(packedLight, (int)(glowIntensity * 240) << 4);

        float size = CONNECTOR_SIZE / 2;
        float length = CONNECTOR_LENGTH;

        // 渲染連接器主體（立方體）
        renderConnectorCube(matrix, buffer, size, length, r, g, b, alpha, finalLight, packedOverlay);

        // 渲染連接端（較小的突出部分）
        renderConnectorTip(matrix, buffer, size * 0.7f, length, r, g, b, alpha, finalLight, packedOverlay);
    }

    /**
     * 渲染連接器主體立方體
     */
    private void renderConnectorCube(Matrix4f matrix, VertexConsumer buffer,
                                     float size, float length,
                                     float r, float g, float b, float alpha,
                                     int light, int overlay) {

        // 前面（連接端）
        addQuad(buffer, matrix,
                -size, -size, length,
                size, -size, length,
                size,  size, length,
                -size,  size, length,
                r, g, b, alpha, light, overlay);

        // 後面（連接導管端）
        addQuad(buffer, matrix,
                size, -size, 0,
                -size, -size, 0,
                -size,  size, 0,
                size,  size, 0,
                r * 0.8f, g * 0.8f, b * 0.8f, alpha, light, overlay);

        // 側面
        renderConnectorSides(matrix, buffer, size, length, r, g, b, alpha, light, overlay);
    }

    /**
     * 渲染連接器側面
     */
    private void renderConnectorSides(Matrix4f matrix, VertexConsumer buffer,
                                      float size, float length,
                                      float r, float g, float b, float alpha,
                                      int light, int overlay) {

        float sideMultiplier = 0.9f;
        float sR = r * sideMultiplier, sG = g * sideMultiplier, sB = b * sideMultiplier;

        // 上面
        addQuad(buffer, matrix,
                -size,  size, 0,
                size,  size, 0,
                size,  size, length,
                -size,  size, length,
                sR, sG, sB, alpha, light, overlay);

        // 下面
        addQuad(buffer, matrix,
                -size, -size, length,
                size, -size, length,
                size, -size, 0,
                -size, -size, 0,
                sR, sG, sB, alpha, light, overlay);

        // 左面
        addQuad(buffer, matrix,
                -size, -size, 0,
                -size, -size, length,
                -size,  size, length,
                -size,  size, 0,
                sR, sG, sB, alpha, light, overlay);

        // 右面
        addQuad(buffer, matrix,
                size,  size, 0,
                size,  size, length,
                size, -size, length,
                size, -size, 0,
                sR, sG, sB, alpha, light, overlay);
    }

    /**
     * 渲染連接器尖端
     */
    private void renderConnectorTip(Matrix4f matrix, VertexConsumer buffer,
                                    float size, float baseLength,
                                    float r, float g, float b, float alpha,
                                    int light, int overlay) {

        float tipLength = baseLength + 0.02f; // 稍微突出一點
        float brightR = Math.min(1.0f, r * 1.2f);
        float brightG = Math.min(1.0f, g * 1.2f);
        float brightB = Math.min(1.0f, b * 1.2f);

        // 渲染更亮的尖端
        addQuad(buffer, matrix,
                -size, -size, tipLength,
                size, -size, tipLength,
                size,  size, tipLength,
                -size,  size, tipLength,
                brightR, brightG, brightB, alpha, light, overlay);
    }

    /**
     * 根據 IO 類型獲取連接器顏色
     */
    private float[] getConnectorColor(IOHandlerUtils.IOType ioType) {
        return switch (ioType) {
            case INPUT -> new float[]{0.2f, 0.8f, 0.3f};   // 綠色 - 輸入
            case OUTPUT -> new float[]{0.8f, 0.3f, 0.2f};  // 紅色 - 輸出
            case BOTH -> new float[]{0.2f, 0.4f, 0.8f};    // 藍色 - 雙向
            case DISABLED -> new float[]{0.4f, 0.4f, 0.4f}; // 灰色 - 不應該顯示
        };
    }

    /**
     * 計算連接器發光強度
     */
    private float calculateConnectorGlow(IOHandlerUtils.IOType ioType,
                                         ArcaneConduitBlockEntity conduit,
                                         float partialTick) {

        // 基礎發光
        float baseGlow = switch (ioType) {
            case INPUT -> 0.4f;
            case OUTPUT -> 0.6f;
            case BOTH -> 0.5f;
            case DISABLED -> 0.0f;
        };

        // 根據魔力量調整
        int manaStored = conduit.getManaStored();
        int maxMana = conduit.getMaxManaStored();

        if (maxMana > 0) {
            float manaRatio = (float) manaStored / maxMana;
            baseGlow += manaRatio * 0.3f;
        }

        // 呼吸效果
        float time = (System.currentTimeMillis() + partialTick * 50) / 1000.0f;
        float breathe = (Mth.sin(time * 1.5f) + 1.0f) * 0.15f;

        return Math.min(1.0f, baseGlow + breathe);
    }

    // === 以下保持你原有的所有方法不變 ===

    // 只添加一個輔助方法來避免重複代碼
    private void addQuad(VertexConsumer buffer, Matrix4f matrix,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         float r, float g, float b, float alpha,
                         int light, int overlay) {

        addVertex(buffer, matrix, x1, y1, z1, r, g, b, alpha, 0, 0, light, overlay);
        addVertex(buffer, matrix, x2, y2, z2, r, g, b, alpha, 1, 0, light, overlay);
        addVertex(buffer, matrix, x3, y3, z3, r, g, b, alpha, 1, 1, light, overlay);
        addVertex(buffer, matrix, x4, y4, z4, r, g, b, alpha, 0, 1, light, overlay);
    }

    // === 原有方法保持不變 ===

    private float[] getIOTypeColor(IOHandlerUtils.IOType ioType, boolean isActive) {
        float brightness = isActive ? 1.0f : 0.6f;
        return switch (ioType) {
            case INPUT -> new float[]{0.2f * brightness, 0.8f * brightness, 1.0f * brightness};
            case OUTPUT -> new float[]{1.0f * brightness, 0.3f * brightness, 0.2f * brightness};
            case BOTH -> new float[]{0.2f * brightness, 1.0f * brightness, 0.3f * brightness};
            default -> new float[]{0.5f * brightness, 0.5f * brightness, 0.5f * brightness};
        };
    }

    // ... [保持所有其他原有方法不變] ...

    private void renderPriorityNumbers(ArcaneConduitBlockEntity conduit, float partialTick,
                                       PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean shouldShowNumbers = shouldShowPriorityNumbers(conduit, mc.player);
        if (!shouldShowNumbers) return;

        Font font = mc.font;

        for (Direction direction : Direction.values()) {
            IOHandlerUtils.IOType ioType = conduit.getIOConfig(direction);
            if (ioType == IOHandlerUtils.IOType.DISABLED) continue;

            boolean isConnected = isConnected(conduit.getBlockState(), direction);
            if (!isConnected) continue;

            int priority = conduit.getPriority(direction);
            if (priority == 0) continue;

            poseStack.pushPose();
            moveToDirectionEnd(poseStack, direction);

            switch (direction) {
                case UP, DOWN -> poseStack.translate(0.2f, 0, 0);
                default -> poseStack.translate(0, 0.2f, 0);
            }

            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180));

            float scale = 0.015f;
            poseStack.scale(-scale, -scale, scale);

            String text = formatPriorityNumber(priority);
            int color = getPriorityTextColor(priority);
            int textWidth = font.width(text);
            int bgColor = 0x80000000;

            font.drawInBatch(text, -textWidth / 2f, -4, color, false,
                    poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.NORMAL, bgColor, packedLight);

            poseStack.popPose();
        }
    }

    private boolean shouldShowPriorityNumbers(ArcaneConduitBlockEntity conduit, Player player) {
        double distance = player.distanceToSqr(conduit.getBlockPos().getCenter());
        if (distance > 64) return false;

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof BasicTechWandItem) {
            return true;
        }

        return player.isCrouching();
    }

    private String formatPriorityNumber(int priority) {
        if (priority == 0) return "";
        if (Math.abs(priority) >= 1_000_000) {
            return String.format("%.1fM", priority / 1_000_000.0);
        } else if (Math.abs(priority) >= 1_000) {
            return String.format("%.1fK", priority / 1_000.0);
        } else {
            return String.valueOf(priority);
        }
    }

    private int getPriorityTextColor(int priority) {
        if (priority > 1000) {
            return 0xFF55FF55;
        } else if (priority > 100) {
            return 0xFF55FFFF;
        } else if (priority > 0) {
            return 0xFFFFFFFF;
        } else if (priority > -100) {
            return 0xFFFFFF55;
        } else {
            return 0xFFFF5555;
        }
    }

    private void renderCleanCore(ArcaneConduitBlockEntity conduit, float partialTick,
                                 PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay) {
        long gameTime = conduit.getLevel().getGameTime();
        float pulse = 0.9f + 0.1f * Mth.sin((gameTime + partialTick) * 0.08f);
        float manaRatio = conduit.getManaStored() > 0 ?
                (float) conduit.getManaStored() / Math.max(1, conduit.getMaxManaStored()) : 0.2f;
        float brightness = 0.3f + 0.5f * manaRatio;

        poseStack.pushPose();
        float rotation = (gameTime + partialTick) * 0.2f;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        float scale = 0.15f + (0.05f * manaRatio * pulse);
        poseStack.scale(scale, scale, scale);

        VertexConsumer crystalConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(CRYSTAL_TEXTURE));

        float r, g, b;
        if (conduit.getManaStored() > 0) {
            r = 0.4f + 0.2f * manaRatio;
            g = 0.6f + 0.3f * manaRatio;
            b = 1.0f;
        } else {
            r = 0.3f;
            g = 0.3f;
            b = 0.4f;
            brightness *= 0.3f;
        }

        renderSimpleCube(poseStack, crystalConsumer, r, g, b, brightness, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderMekStyleIOIndicators(ArcaneConduitBlockEntity conduit, float partialTick,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay) {
        Level level = conduit.getLevel();
        if (level == null) return;

        BlockState state = level.getBlockState(conduit.getBlockPos());

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

    private void renderIOIndicator(PoseStack poseStack, MultiBufferSource bufferSource,
                                   Direction direction, IOHandlerUtils.IOType ioType,
                                   float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        moveToDirectionEnd(poseStack, direction);
        float[] color = getIOTypeColor(ioType);
        long gameTime = System.currentTimeMillis();
        float pulse = 0.8f + 0.2f * Mth.sin(gameTime * 0.003f + direction.ordinal());
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());

        switch (ioType) {
            case INPUT -> renderInputIndicator(poseStack, consumer, color, pulse, packedLight, packedOverlay);
            case OUTPUT -> renderOutputIndicator(poseStack, consumer, color, pulse, packedLight, packedOverlay);
            case BOTH -> renderBothIndicator(poseStack, consumer, color, pulse, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private void renderInputIndicator(PoseStack poseStack, VertexConsumer consumer,
                                      float[] color, float pulse, int packedLight, int packedOverlay) {
        float radius = 0.08f * pulse;
        float depth = 0.02f;
        renderCircularInset(poseStack, consumer, radius, depth, color, packedLight, packedOverlay);
    }

    private void renderOutputIndicator(PoseStack poseStack, VertexConsumer consumer,
                                       float[] color, float pulse, int packedLight, int packedOverlay) {
        float size = 0.06f * pulse;
        float height = 0.03f;
        renderSquareProtrusion(poseStack, consumer, size, height, color, packedLight, packedOverlay);
    }

    private void renderBothIndicator(PoseStack poseStack, VertexConsumer consumer,
                                     float[] color, float pulse, int packedLight, int packedOverlay) {
        float size = 0.07f * pulse;
        renderDiamondIndicator(poseStack, consumer, size, color, packedLight, packedOverlay);
    }

    private void renderCircularInset(PoseStack poseStack, VertexConsumer consumer,
                                     float radius, float depth, float[] color,
                                     int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();
        int segments = 16;

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (2 * Math.PI * i / segments);
            float angle2 = (float) (2 * Math.PI * (i + 1) / segments);

            float x1 = Mth.cos(angle1) * radius;
            float y1 = Mth.sin(angle1) * radius;
            float x2 = Mth.cos(angle2) * radius;
            float y2 = Mth.sin(angle2) * radius;

            addVertex(consumer, matrix, x1, y1, 0, color[0], color[1], color[2], 0.8f, 0, 0, packedLight, packedOverlay);
            addVertex(consumer, matrix, x2, y2, 0, color[0], color[1], color[2], 0.8f, 1, 0, packedLight, packedOverlay);
            addVertex(consumer, matrix, x2, y2, -depth, color[0], color[1], color[2], 0.8f, 1, 1, packedLight, packedOverlay);
            addVertex(consumer, matrix, x1, y1, -depth, color[0], color[1], color[2], 0.8f, 0, 1, packedLight, packedOverlay);
        }
    }

    private void renderSquareProtrusion(PoseStack poseStack, VertexConsumer consumer,
                                        float size, float height, float[] color,
                                        int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        addVertex(consumer, matrix, -size, -size, 0, color[0], color[1], color[2], 0.8f, 0, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, -size, 0, color[0], color[1], color[2], 0.8f, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, size, 0, color[0], color[1], color[2], 0.8f, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, -size, size, 0, color[0], color[1], color[2], 0.8f, 0, 0, packedLight, packedOverlay);

        addVertex(consumer, matrix, -size, size, 0, color[0], color[1], color[2], 0.8f, 0, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, size, 0, color[0], color[1], color[2], 0.8f, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, size, height, color[0], color[1], color[2], 0.8f, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, -size, size, height, color[0], color[1], color[2], 0.8f, 0, 1, packedLight, packedOverlay);

        addVertex(consumer, matrix, size, -size, 0, color[0], color[1], color[2], 0.8f, 0, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, size, 0, color[0], color[1], color[2], 0.8f, 1, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, size, height, color[0], color[1], color[2], 0.8f, 1, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, size, -size, height, color[0], color[1], color[2], 0.8f, 0, 1, packedLight, packedOverlay);
    }

    private void renderDiamondIndicator(PoseStack poseStack, VertexConsumer consumer,
                                        float size, float[] color,
                                        int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

        float[][] vertices = {
                {0, size, 0.01f},
                {size, 0, 0.01f},
                {0, -size, 0.01f},
                {-size, 0, 0.01f}
        };

        addVertex(consumer, matrix, vertices[0][0], vertices[0][1], vertices[0][2],
                color[0], color[1], color[2], 0.8f, 0.5f, 0, packedLight, packedOverlay);
        addVertex(consumer, matrix, vertices[1][0], vertices[1][1], vertices[1][2],
                color[0], color[1], color[2], 0.8f, 1, 0.5f, packedLight, packedOverlay);
        addVertex(consumer, matrix, vertices[2][0], vertices[2][1], vertices[2][2],
                color[0], color[1], color[2], 0.8f, 0.5f, 1, packedLight, packedOverlay);
        addVertex(consumer, matrix, vertices[3][0], vertices[3][1], vertices[3][2],
                color[0], color[1], color[2], 0.8f, 0, 0.5f, packedLight, packedOverlay);
    }

    private void moveToDirectionEnd(PoseStack poseStack, Direction direction) {
        float offset = 0.51f;

        switch (direction) {
            case NORTH -> poseStack.translate(0, 0, -offset);
            case SOUTH -> poseStack.translate(0, 0, offset);
            case WEST -> poseStack.translate(-offset, 0, 0);
            case EAST -> poseStack.translate(offset, 0, 0);
            case UP -> poseStack.translate(0, offset, 0);
            case DOWN -> poseStack.translate(0, -offset, 0);
        }

        switch (direction) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> { /* 默認方向 */ }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
        }
    }

    private float[] getIOTypeColor(IOHandlerUtils.IOType ioType) {
        return switch (ioType) {
            case INPUT -> new float[]{0.3f, 0.6f, 1.0f};
            case OUTPUT -> new float[]{1.0f, 0.4f, 0.3f};
            case BOTH -> new float[]{0.2f, 1.0f, 0.4f};
            default -> new float[]{0.5f, 0.5f, 0.5f};
        };
    }

    private boolean isConnected(BlockState state, Direction direction) {
        if (!(state.getBlock() instanceof ArcaneConduitBlock)) {
            return false;
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

    private void renderSimpleCube(PoseStack poseStack, VertexConsumer consumer,
                                  float r, float g, float b, float a, int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();

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

    private double getDistanceToPlayer(ArcaneConduitBlockEntity conduit) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return Double.MAX_VALUE;
        return mc.player.distanceToSqr(conduit.getBlockPos().getCenter());
    }

    private boolean shouldRenderEnhancements() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        return true;
    }

    private void renderManaFlowAnimation(ArcaneConduitBlockEntity conduit, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {
        // 魔力流動動畫實現 - 可以根據你的需求來實現
        // 這裡留作將來擴展
    }

    @Override
    public int getViewDistance() {
        return 32;
    }
}