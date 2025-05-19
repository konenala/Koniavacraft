package com.github.nalamodikk.common.ComponentSystem.API.machine.gui;

import com.github.nalamodikk.client.screenAPI.gui.api.IGuiDataContext;
import com.github.nalamodikk.client.screenAPI.gui.api.IGuiElementProvider;
import com.github.nalamodikk.client.screenAPI.gui.api.IGuiLayoutBuilder;
import com.github.nalamodikk.client.screenAPI.gui.element.GuiBigSlotElement;
import com.github.nalamodikk.client.screenAPI.gui.element.GuiElement;
import com.github.nalamodikk.client.screenAPI.gui.element.GuiLabelElement;
import com.github.nalamodikk.client.screenAPI.gui.element.GuiToggleButtonElement;
import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.ComponentSystem.API.machine.grid.ComponentRecord;
import com.github.nalamodikk.common.ComponentSystem.register.component.ComponentRegistry;
import net.minecraft.network.chat.Component;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class ModularMachineScreenBuilder implements IGuiLayoutBuilder {
    private final List<GuiElement> elements = new ArrayList<>();
    private int nextY = 20; // 自動排版的 Y 軸起點

    public void addElement(GuiElement element) {
        elements.add(element);
    }


    public void addToggleButton(String buttonKey, String labelKey, boolean state, Runnable onClick) {
        int x = 120;
        int y = nextY;
        nextY += 20;

        GuiToggleButtonElement button = new GuiToggleButtonElement(buttonKey)
                .withState(state)
                .at(x, y)
                .onClick(onClick)
                .withLabel(Component.translatable(labelKey)); // ✅ 現在這行不會紅了！

        this.addElement(button);
    }


    public List<GuiElement> getElements() {
        return elements;
    }

    public void injectFrom(ComponentGrid grid, List<ComponentRecord> records) {
        for (ComponentRecord record : records) {
            IGridComponent component = ComponentRegistry.get(record.id().toString()); // ✅ 傳入字串
            if (component instanceof IGuiElementProvider provider) {
                IGuiDataContext context = new ComponentGuiContext(grid, record.pos(), component, record.data());
                provider.provideGuiElements(this, context); // ✅ 正確簽名
            }
        }
    }


    @Override
    public void addSlot(String id, int localSlot, IItemHandler handler, int x, int y, String tooltipKey) {
        int slotIndex = localSlot; // 如果你不處理 offset，可以這樣暫代
//
//        GuiSlotElement slot = new GuiSlotElement(
//                handler,
//                slotIndex,
//                x, y,
//                Component.translatable(tooltipKey)
//        );
//        this.elements.add(slot);
    }

    public void addSlot(int slotIndex, int localSlotId, ItemStackHandler handler, String tooltipKey) {
        int x = 20; // 你之前的座標邏輯
        int y = nextY;
        nextY += 18;

        elements.add(new GuiBigSlotElement(
                (IItemHandlerModifiable) handler,
                slotIndex,
                x,
                y,
                18, // 寬度，依實際定義
                18, // 高度，依實際定義
                Component.translatable(tooltipKey)
        ));
    }


    @Override
    public void addLabel(String textKey, int x, int y) {
        this.addElement(new GuiLabelElement(Component.translatable(textKey), x, y));
    }
    @Override
    public void addToggle(String id, String labelKey, boolean currentState, Runnable onClick) {
        GuiToggleButtonElement button = new GuiToggleButtonElement(id)
                .at(10, 10) // ❗你可自訂座標
                .withLabel(Component.translatable(labelKey))
                .withState(currentState)
                .onClick(onClick);
        this.elements.add(button);
    }



}
