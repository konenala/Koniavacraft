package com.github.nalamodikk.client.screenAPI.gui.slot;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class BigSlot extends SlotItemHandler {
    private final int width;
    private final int height;

    public BigSlot(IItemHandler handler, int index, int x, int y, int width, int height) {
        super(handler, index, x, y);
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
