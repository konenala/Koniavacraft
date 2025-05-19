package com.github.nalamodikk.client.screenAPI.gui.slot.helper;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class BigSlot extends Slot {
    private final int width;
    private final int height;

    public BigSlot(Container container, int index, int x, int y, int width, int height) {
        super(container, index, x, y);
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isPointInside(double mouseX, double mouseY, int guiLeft, int guiTop) {
        int slotX = guiLeft + this.x;
        int slotY = guiTop + this.y;
        return mouseX >= slotX && mouseX < slotX + width &&
                mouseY >= slotY && mouseY < slotY + height;
    }
}
