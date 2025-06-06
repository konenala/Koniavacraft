package com.github.nalamodikk.draft.screen3d.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 用於在世界中繪製 2D 四邊形貼圖（例如面板、提示 UI 等）
 */
public class GuiQuadBatcher {

    /**
     * 繪製一個貼圖四邊形在玩家眼前。
     */
    public static void drawTexturedQuad(PoseStack stack, MultiBufferSource buffer, Camera camera, float w, float h, float alpha, ResourceLocation texture) {
        Matrix4f pose = stack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));

        float minX = -w / 2;
        float minY = -h / 2;

        putQuad(consumer, pose, minX, minY, w, h, alpha);
    }

    /**
     * 在指定位置與大小繪製四邊形。
     */
    public static void putQuad(VertexConsumer consumer, Matrix4f pose, float x, float y, float w, float h, float alpha) {
        float x0 = x, x1 = x + w;
        float y0 = y, y1 = y + h;
        int light = 0xF000F0;
        int overlay = 0;

        consumer.addVertex(pose, x0, y0, 0).setColor(1f, 1f, 1f, alpha).setUv(0, 1).setLight(light).setOverlay(overlay).setNormal(0, 0, -1);
        consumer.addVertex(pose, x1, y0, 0).setColor(1f, 1f, 1f, alpha).setUv(1, 1).setLight(light).setOverlay(overlay).setNormal(0, 0, -1);
        consumer.addVertex(pose, x1, y1, 0).setColor(1f, 1f, 1f, alpha).setUv(1, 0).setLight(light).setOverlay(overlay).setNormal(0, 0, -1);
        consumer.addVertex(pose, x0, y1, 0).setColor(1f, 1f, 1f, alpha).setUv(0, 0).setLight(light).setOverlay(overlay).setNormal(0, 0, -1);
    }
}

