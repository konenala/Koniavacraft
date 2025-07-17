package com.github.nalamodikk.common.block.blockentity.collector.solarmana;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;

public class SolarCollectorRenderer implements BlockEntityRenderer<SolarManaCollectorBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public SolarCollectorRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(SolarManaCollectorBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        // 🎯 就是渲染你的原有模型，但是用 BlockEntityRenderer 的方式
        BlockState blockState = blockEntity.getBlockState();
        BakedModel model = blockRenderer.getBlockModel(blockState);

        poseStack.pushPose();

        // 📐 移動到正確位置
        poseStack.translate(0, 0, 0);

        // 🎨 渲染你的模型（這樣就不會有陰影問題了）
        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.translucent()),
                blockState,
                model,
                1.0f, 1.0f, 1.0f,  // RGB 顏色
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }
}
