package com.github.nalamodikk.client.screenAPI.framework;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 模組化按鈕元件。
 * 繼承自 AbstractWidget，支援相對座標與自動縮放。
 */
public class ButtonWidget extends AbstractWidget {
    
    private final ResourceLocation texture;
    private final int texWidth, texHeight;
    private final Consumer<ButtonWidget> onPress;
    private Supplier<List<Component>> tooltipSupplier;
    private Component message = Component.empty();

    public ButtonWidget(int x, int y, int width, int height, ResourceLocation texture, int texW, int texH, Consumer<ButtonWidget> onPress) {
        super(x, y, width, height);
        this.texture = texture;
        this.texWidth = texW;
        this.texHeight = texH;
        this.onPress = onPress;
    }

    public ButtonWidget setTooltip(Supplier<List<Component>> tooltip) {
        this.tooltipSupplier = tooltip;
        return this;
    }

    public ButtonWidget setMessage(Component message) {
        this.message = message;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int localMouseX, int localMouseY, int screenMouseX, int screenMouseY) {
        // 1. 畫背景圖
        // 這裡可以根據 hover 狀態改變顏色或亮度
        if (isMouseOver(screenMouseX, screenMouseY)) {
            graphics.setColor(0.9f, 0.9f, 1.0f, 1.0f); // Hover 時稍微變亮/變藍
        }
        
        graphics.blit(texture, 0, 0, 0, 0, width, height, texWidth, texHeight);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        // 2. 畫文字
        if (message != null && !message.getString().isEmpty()) {
            int textColor = isMouseOver(screenMouseX, screenMouseY) ? 0xFFFFFF : 0xAAAAAA;
            graphics.drawCenteredString(Minecraft.getInstance().font, message, width / 2, (height - 8) / 2, textColor);
        }
    }

    @Override
    protected boolean onMouseClicked(int localMouseX, int localMouseY, int button) {
        if (button == 0) { // 左鍵點擊
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            onPress.accept(this);
            return true;
        }
        return false;
    }

    @Override
    public List<Component> getTooltip() {
        return tooltipSupplier != null ? tooltipSupplier.get() : Collections.emptyList();
    }
}
