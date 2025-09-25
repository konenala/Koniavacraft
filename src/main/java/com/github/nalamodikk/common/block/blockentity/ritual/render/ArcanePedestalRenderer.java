package com.github.nalamodikk.common.block.blockentity.ritual.render;

import com.github.nalamodikk.common.block.blockentity.arcanematrix.arcanepedestal.ArcanePedestalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 奧術基座渲染器 - 渲染浮動的祭品物品
 */
public class ArcanePedestalRenderer implements BlockEntityRenderer<ArcanePedestalBlockEntity> {

    private final ItemRenderer itemRenderer;

    public ArcanePedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ArcanePedestalBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        ItemStack offering = blockEntity.getOffering();
        if (offering.isEmpty()) {
            return; // 沒有物品則不渲染
        }

        poseStack.pushPose();

        // 移動到基座頂部中央
        poseStack.translate(0.5D, 0.75D, 0.5D);

        // 添加浮動效果
        float hoverOffset = blockEntity.getHoverOffset(partialTick);
        poseStack.translate(0.0D, hoverOffset, 0.0D);

        // 旋轉效果
        float rotation = blockEntity.getSpinForRender(partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // 物品被消耗時的特效
        if (blockEntity.isOfferingConsumed()) {
            long time = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
            float pulse = 0.3f + 0.2f * (float) Math.sin(time / 5.0f);
            float consumedScale = 0.8f + pulse * 0.05f;
            poseStack.scale(consumedScale, consumedScale, consumedScale);
        }

        // 縮放物品
        float scale = 0.5f;
        poseStack.scale(scale, scale, scale);

        // 渲染物品
        itemRenderer.renderStatic(
                offering,
                ItemDisplayContext.GROUND,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                0
        );

        poseStack.popPose();
    }
}
