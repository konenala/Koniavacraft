package com.github.nalamodikk.experimental.screen3d.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * 世界中渲染的基底面板。
 */
public abstract class FloatingScreen {
    protected Vec3 position;
    protected float scale = 1.0f;
    protected float alpha = 1.0f;
    protected boolean visible = true;
    protected float width = 1.0f;   // ✅ 新增
    protected float height = 0.6f;  // ✅ 新增
    protected boolean faceCamera = true; // 是否總是面向玩家
    protected boolean autoScale = false; // 是否根據距離縮放

    public FloatingScreen(Vec3 position, float width, float height) {
        this.position = position;
        this.width = width;
        this.height = height;
    }
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }


    public void tick() {
        // 預設不做事，可以由子類覆寫
    }

    public abstract void render(PoseStack stack, MultiBufferSource buffer, Camera camera, float partialTick);
}
