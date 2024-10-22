package com.github.nalamodikk.API;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class CustomTextureButton extends Button {
    private final ResourceLocation buttonTexture;

    public CustomTextureButton(int x, int y, int width, int height, ResourceLocation texture, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.buttonTexture = texture;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        // 使用自定義紋理渲染按鈕
        guiGraphics.blit(buttonTexture, this.getX(), this.getY(), 0, 0, this.width, this.height);

        // 如果需要，可以添加鼠標懸停時的高亮效果
        if (this.isHoveredOrFocused()) {
            guiGraphics.blit(buttonTexture, this.getX(), this.getY(), 0, this.height, this.width, this.height);
        }
    }
}
