package com.github.nalamodikk.client.screenAPI.gui.api;

import net.minecraftforge.items.IItemHandler;

public interface IGuiLayoutBuilder {
    void addSlot(String id, int localSlot, IItemHandler handler, int x, int y, String tooltipKey);
    void addToggle(String id, String labelKey, boolean currentState, Runnable onClick);
    void addLabel(String textKey, int x, int y);
}
