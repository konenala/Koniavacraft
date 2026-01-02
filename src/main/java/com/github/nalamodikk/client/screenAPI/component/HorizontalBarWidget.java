package com.github.nalamodikk.client.screenAPI.component;

import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.IntSupplier;

/**
 * 通用的水平進度條元件。
 * 用於加工箭頭、熔爐火苗(如果是橫向的話)等。
 */
public class HorizontalBarWidget extends AbstractWidget {

    @Nullable
    protected final ResourceLocation textureEmpty;
    protected final ResourceLocation textureFull;
    protected final IntSupplier progressSupplier;
    protected final IntSupplier maxSupplier;

    public HorizontalBarWidget(int x, int y, int width, int height,
                               @Nullable ResourceLocation empty, ResourceLocation full,
                               IntSupplier progress, IntSupplier max) {
        super(x, y, width, height);
        this.textureEmpty = empty;
        this.textureFull = full;
        this.progressSupplier = progress;
        this.maxSupplier = max;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int localMouseX, int localMouseY, int screenMouseX, int screenMouseY) {
        // 1. 繪製背景 (Empty)
        if (textureEmpty != null) {
            // 背景縮放到 Widget 大小
            graphics.blit(textureEmpty, 0, 0, 0, 0, width, height, width, height);
        }

        // 2. 計算比例
        int max = maxSupplier.getAsInt();
        if (max > 0) {
            int progress = progressSupplier.getAsInt();
            if (progress > max) progress = max;
            if (progress < 0) progress = 0;

            float ratio = (float) progress / max;
            
            // 螢幕上的像素寬度 (根據 Widget 設定的 width)
            int fillWidth = (int) (ratio * width);
            
            // 材質上的像素寬度 (這裡我們假設材質圖的寬度等於我們預設的 width)
            // 💡 改進：這裡的 textureWidth 與 textureHeight 我們可以直接傳入 width, height
            // 這樣 blit 會自動幫我們處理縮放。
            
            if (fillWidth > 0) {
                // 3. 繪製前景 (Full) - 由左往右
                // 這裡使用 blit 的縮放版本
                graphics.blit(textureFull, 
                    0, 0,               // 螢幕座標
                    fillWidth, height,  // 螢幕寬高 (決定了畫出來多長)
                    0, 0,               // UV 起點
                    fillWidth, height,  // UV 寬高 (決定了從圖片取多少)
                    width, height       // 圖片總大小 (用於計算 UV 比例)
                );
            }
        }
    }
    // 進度條通常不需要 Tooltip，除非您想顯示 "50%"
}
