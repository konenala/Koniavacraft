package com.github.nalamodikk.client.screenAPI.component;

import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ImageWidget extends AbstractWidget {
    private final ResourceLocation texture;
    private final int u, v;
    private final int textureWidth, textureHeight;
    
    // 顏色濾鏡 (RGBA)
    private float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;

    public ImageWidget(int x, int y, int width, int height, ResourceLocation texture, int u, int v) {
        this(x, y, width, height, texture, u, v, 256, 256);
    }

    public ImageWidget(int x, int y, int width, int height, ResourceLocation texture, int u, int v, int texW, int texH) {
        super(x, y, width, height);
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.textureWidth = texW;
        this.textureHeight = texH;
    }

    public ImageWidget setColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int localMouseX, int localMouseY, int screenMouseX, int screenMouseY) {
        graphics.setColor(r, g, b, a);
        graphics.blit(texture, 0, 0, u, v, width, height, textureWidth, textureHeight);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
