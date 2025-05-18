package com.github.nalamodikk.client.screenAPI.gui.element;

import com.github.nalamodikk.client.screenAPI.gui.api.IGuiRenderContext;
import net.minecraft.network.chat.Component;

public class GuiToggleButtonElement extends GuiElement {

    private String id;
    private boolean state = false;
    private int x = 0, y = 0;
    private Component label;
    private Runnable onClick = () -> {};

    public GuiToggleButtonElement(String id) {
        super(0, 0); // 初始化預設位置
        this.id = id;
    }

    public GuiToggleButtonElement at(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public GuiToggleButtonElement withLabel(Component label) {
        this.label = label;
        return this;
    }

    public GuiToggleButtonElement withState(boolean state) {
        this.state = state;
        return this;
    }

    public GuiToggleButtonElement onClick(Runnable onClick) {
        this.onClick = onClick;
        return this;
    }

    @Override
    public void render(IGuiRenderContext context, int mouseX, int mouseY, float partialTick) {
        context.renderToggleButton(x, y, label.getString(), state, onClick);
    }

    // Getter (選擇性)
    public String getId() {
        return id;
    }

    public boolean getState() {
        return state;
    }

    public Component getLabel() {
        return label;
    }

    public Runnable getOnClick() {
        return onClick;
    }
}


