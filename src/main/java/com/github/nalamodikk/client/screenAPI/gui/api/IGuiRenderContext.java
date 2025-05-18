package com.github.nalamodikk.client.screenAPI.gui.api;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.items.IItemHandler;

public interface IGuiRenderContext {
    void renderSlot(int x, int y, int slotIndex, IItemHandler handler, String tooltipKey);
    void renderToggleButton(int x, int y, String labelKey, boolean currentState, Runnable onClick);
    void drawText(Component text, int x, int y);
    GuiGraphics getGuiGraphics();
    Screen getScreen();
}
