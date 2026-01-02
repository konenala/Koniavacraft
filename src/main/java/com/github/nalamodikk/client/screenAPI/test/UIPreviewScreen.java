package com.github.nalamodikk.client.screenAPI.test;

import com.github.nalamodikk.client.screenAPI.component.EnergyBarWidget;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 用於測試 UI Widget 的預覽介面。
 * 不依賴伺服器容器，純客戶端渲染。
 */
public class UIPreviewScreen extends Screen {

    private Panel rootPanel;

    public UIPreviewScreen() {
        super(Component.literal("UI Preview"));
    }

    @Override
    protected void init() {
        // 建立全螢幕 Panel
        this.rootPanel = new Panel(0, 0, this.width, this.height);
        
        // 模擬數據：正弦波跳動 (0 ~ 10000)
        long time = System.currentTimeMillis();
        
        // --- 測試區 ---
        
        // 1. 能量條 (Energy Bar) - 放在左邊 (50, 50)
        rootPanel.add(new EnergyBarWidget(50, 50, 
            () -> (int) ((Math.sin(System.currentTimeMillis() / 500.0) + 1) * 5000), // Value: 0~10000
            () -> 10000 // Max: 10000
        ));
        
        // 2. 魔力條 (Mana Bar) - 放在右邊 (100, 50)
        rootPanel.add(new ManaBarWidget(100, 50, 
            () -> (int) ((Math.cos(System.currentTimeMillis() / 500.0) + 1) * 5000), 
            () -> 10000
        ));

        // 3. 測試邊界 (Panel Background) - 畫一個灰色半透明背景來確認 Panel 位置
        // rootPanel.setBackground(0x80000000); // 如果 Panel 支援的話
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        
        // 渲染我們的 Panel
        rootPanel.render(graphics, mouseX, mouseY);
        
        // 繪製說明文字
        graphics.drawString(this.font, "UI Widget Test Screen", 10, 10, 0xFFFFFF);
        graphics.drawString(this.font, "X: " + mouseX + " Y: " + mouseY, 10, 20, 0xAAAAAA);
        
        // 處理 Tooltips
        var tooltips = rootPanel.getChildrenTooltip(mouseX, mouseY);
        if (!tooltips.isEmpty()) {
            graphics.renderComponentTooltip(this.font, tooltips, mouseX, mouseY);
        }
    }
}
