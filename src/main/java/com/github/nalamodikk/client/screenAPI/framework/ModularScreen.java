package com.github.nalamodikk.client.screenAPI.framework;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

public abstract class ModularScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    
    protected Panel rootPanel;

    public ModularScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // 初始化 RootPanel，預設使用背景圖大小
        this.rootPanel = new Panel(leftPos, topPos, imageWidth, imageHeight);
        
        // 建構 GUI (由子類別實作)
        buildGui(rootPanel);
    }

    /**
     *在此方法中組裝您的 GUI。
     * 用法：rootPanel.add(new EnergyWidget(...));
     */
    protected abstract void buildGui(Panel root);

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // 1. 繪製 RootPanel (包含所有子元件)
        // 注意：我們傳入的是相對於螢幕左上角的滑鼠座標
        // RootPanel 位於 (leftPos, topPos)，所以傳給它的 mouseX 應該是 mouseX - leftPos
        // 等等，AbstractWidget.render 的邏輯是傳入 localMouseX
        // RootPanel 的 x, y 是 leftPos, topPos
        // 所以 graphics 不需要先 translate (render 內部會做)
        
        rootPanel.render(graphics, mouseX, mouseY);
        
        // 2. 繪製 Tooltip
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 可以在這裡畫背景圖，或者讓 RootPanel 畫
        // 這裡留空，建議在 buildGui 裡加入一個 ImageWidget 作為背景
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 禁用原版的 Label 繪製 (title, inventory)，改由 Widget 處理
        // 或者手動畫：
        // graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        super.renderTooltip(graphics, x, y);
        
        // 檢查 Widget 的 Tooltip
        List<Component> tooltip = rootPanel.getChildrenTooltip(x, y);
        if (!tooltip.isEmpty()) {
            graphics.renderComponentTooltip(font, tooltip, x, y);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 先讓 Panel 處理
        if (rootPanel.mouseClicked((int)mouseX, (int)mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        rootPanel.mouseReleased((int)mouseX, (int)mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
