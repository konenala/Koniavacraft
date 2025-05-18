package com.github.nalamodikk.client.screenAPI.gui.element;

import com.github.nalamodikk.client.screenAPI.gui.api.IGuiRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class GuiBigSlotElement extends GuiElement {

    private final IItemHandlerModifiable handler;
    private final int slotIndex;
    private final int width;
    private final int height;
    private final Component tooltip;

    public GuiBigSlotElement(IItemHandlerModifiable handler, int slotIndex, int x, int y, int width, int height, Component tooltip) {
        super(x, y);
        this.handler = handler;
        this.slotIndex = slotIndex;
        this.width = width;
        this.height = height;
        this.tooltip = tooltip;
    }

    // 這是判定滑鼠是否在這個放大格子裡面
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX < this.x + width && mouseY >= this.y && mouseY < this.y + height;
    }

    // 繪製放大格子的物品和提示
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick, Screen screen) {
        ItemStack stack = handler.getStackInSlot(slotIndex);
        if (!stack.isEmpty()) {
            // 繪製物品圖示
            g.renderItem(stack, x, y);
        }
        // 顯示 tooltip
        renderTooltip(g, screen, mouseX, mouseY);
    }

    // 顯示 tooltip 的函數
    public void renderTooltip(GuiGraphics g, Screen screen, int mouseX, int mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            ItemStack stack = handler.getStackInSlot(slotIndex);
            if (!stack.isEmpty()) {
                g.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
            } else if (tooltip != null) {
                g.renderTooltip(Minecraft.getInstance().font, List.of(tooltip), Optional.empty(), mouseX, mouseY);
            }
        }
    }


    @Override
    public void render(IGuiRenderContext context, int mouseX, int mouseY, float partialTick) {
        // 從 context 取得 GuiGraphics 跟 Screen
        GuiGraphics g = context.getGuiGraphics();
        Screen screen = context.getScreen();

        // 取得這個 slot 的 ItemStack
        ItemStack stack = handler.getStackInSlot(slotIndex);

        // 如果有物品，畫出物品圖示
        if (!stack.isEmpty()) {
            g.renderItem(stack, x, y);
        }

        // 顯示 tooltip（如果滑鼠在格子內）
        renderTooltip(g, screen, mouseX, mouseY);
    }


    // 你可以依需求加上點擊事件等其他方法
}
