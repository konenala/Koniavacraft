package com.github.nalamodikk.client.render.api;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import org.joml.Vector3f;

/**
 * 自訂 VertexConsumer 包裝器，避免使用 IDE 無法解析的預設方法鏈式呼叫。
 */
public class SimpleVertexWriter {
    private final VertexConsumer vc;
    private final PoseStack.Pose pose;
    private int color = FastColor.ARGB32.color(255, 255, 255, 255);
    private int overlay = OverlayTexture.NO_OVERLAY;
    private int light = 15728880;
    private Vector3f normal = new Vector3f(0, 0, -1);

    public SimpleVertexWriter(VertexConsumer vc, PoseStack.Pose pose) {
        this.vc = vc;
        this.pose = pose;
    }

    public SimpleVertexWriter color(int color) {
        this.color = color;
        return this;
    }

    public SimpleVertexWriter overlay(int overlay) {
        this.overlay = overlay;
        return this;
    }

    public SimpleVertexWriter light(int light) {
        this.light = light;
        return this;
    }

    public SimpleVertexWriter normal(float x, float y, float z) {
        this.normal.set(x, y, z);
        return this;
    }

    public void vertex(float x, float y, float z, float u, float v) {
        vc.setColor(color);
        vc.setUv(u, v);
        vc.setOverlay(overlay);
        vc.setLight(light);
        vc.setNormal(pose, normal.x, normal.y, normal.z);
        vc.addVertex(pose, x, y, z);
    }

    public void quad(float x1, float y1, float x2, float y2, float z, float alpha) {
        int a = (int)(255 * alpha);
        this.color = FastColor.ARGB32.color(a, 255, 255, 255);
        vertex(x1, y2, z, 0, 1);
        vertex(x2, y2, z, 1, 1);
        vertex(x2, y1, z, 1, 0);
        vertex(x1, y1, z, 0, 0);
    }
}
