package com.github.nalamodikk.client.screenAPI.gui.element;


import com.github.nalamodikk.client.screenAPI.gui.api.IGuiRenderContext;
import net.minecraft.network.chat.Component;

public class GuiLabelElement extends GuiElement {

    private final Component text;
    private final int x;
    private final int y;

    public GuiLabelElement(Component text, int x, int y) {
        super(x, y);
        this.text = text;
        this.x = x;
        this.y = y;
    }

    @Override
    public void render(IGuiRenderContext context, int mouseX, int mouseY, float partialTick) {
        context.drawText(text, x, y);
    }
}
