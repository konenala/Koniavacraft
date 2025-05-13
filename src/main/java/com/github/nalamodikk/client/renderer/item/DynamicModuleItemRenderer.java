package com.github.nalamodikk.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class DynamicModuleItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation("magical_industry", "textures/item/module/default.png");

    public DynamicModuleItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffer, int light, int overlay) {

        ResourceLocation texture = getTextureFromStack(stack);

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, null, null, 0);

        RenderType renderType = RenderType.entityCutout(texture);
        VertexConsumer vertex = buffer.getBuffer(renderType);

        itemRenderer.renderModelLists(model, stack, light, overlay, poseStack, vertex);
    }

    private ResourceLocation getTextureFromStack(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("component_id")) {
            String idStr = stack.getTag().getString("component_id");
            ResourceLocation id = ResourceLocation.tryParse(idStr);
            if (id != null) {
                return new ResourceLocation("magical_industry", "textures/item/module/" + id.getNamespace() + "_" + id.getPath() + ".png");
            }
        }
        return DEFAULT_TEXTURE;
    }
}