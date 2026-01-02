package com.github.nalamodikk.client.screenAPI.component;

import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;

import javax.annotation.Nullable;

/**
 * 通用的垂直進度條元件。
 * 用於能量條、魔力條、液體槽等。
 */
public abstract class VerticalBarWidget extends AbstractWidget {

    @Nullable
    protected final ResourceLocation textureEmpty;
    protected final ResourceLocation textureFull;
    protected final IntSupplier valueSupplier;
    protected final IntSupplier maxSupplier;
    
    // 預設大小
    public static final int DEFAULT_WIDTH = 18;
    public static final int DEFAULT_HEIGHT = 60; // 假設的高度，您可能需要調整
    
    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");

    public VerticalBarWidget(int x, int y, int width, int height, 
                             @Nullable ResourceLocation empty, ResourceLocation full,
                             IntSupplier value, IntSupplier max) {
        super(x, y, width, height);
        this.textureEmpty = empty;
        this.textureFull = full;
        this.valueSupplier = value;
        this.maxSupplier = max;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int localMouseX, int localMouseY, int screenMouseX, int screenMouseY) {
        // 1. 繪製背景 (Empty) - 只有當不為 null 時才畫
        if (textureEmpty != null) {
            graphics.blit(textureEmpty, 0, 0, 0, 0, width, height, width, height);
        }

        // 2. 計算比例
        int max = maxSupplier.getAsInt();
        if (max > 0) {
            int val = valueSupplier.getAsInt();
            // 防止數值溢出
            if (val > max) val = max;
            if (val < 0) val = 0;

            int fillHeight = (int) ((float) val / max * height);
            
            if (fillHeight > 0) {
                // 3. 繪製前景 (Full) - 由下往上裁切
                // blit(texture, x, y, u, v, width, height, textureWidth, textureHeight)
                // 我們要畫在底部：y = height - fillHeight
                // UV 的 v 也要對應：v = height - fillHeight
                
                int yOffset = height - fillHeight;
                graphics.blit(textureFull, 
                    0, yOffset,             // 螢幕上的 (x, y)
                    0, yOffset,             // 材質上的 (u, v)
                    width, fillHeight,      // 繪製的寬高
                    width, height           // 材質總寬高
                );
            }
        }
    }

    @Override
    public List<Component> getTooltip() {
        int val = valueSupplier.getAsInt();
        int max = maxSupplier.getAsInt();
        return Collections.singletonList(
            Component.literal(getTooltipPrefix() + ": " + FORMAT.format(val) + " / " + FORMAT.format(max))
        );
    }
    
    protected abstract String getTooltipPrefix();
}
