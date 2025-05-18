package com.github.nalamodikk.client.screenAPI.gui.element;


import com.github.nalamodikk.client.screenAPI.gui.api.IGuiRenderContext;

public abstract class GuiElement {
    public final int x;
    public final int y;

    public GuiElement(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public abstract void render(IGuiRenderContext context, int mouseX, int mouseY, float partialTick);
}
