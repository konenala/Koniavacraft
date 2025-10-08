package com.github.nalamodikk.common.block.blockentity.ritual.jei;

import com.github.nalamodikk.client.ritual.RitualStructureProjectionManager;
import com.github.nalamodikk.common.block.blockentity.ritual.structure.RitualStructureBlueprint;
import com.github.nalamodikk.common.block.blockentity.ritual.structure.StructureElement;
import com.github.nalamodikk.common.block.blockentity.ritual.structure.StructureElementType;
import com.github.nalamodikk.common.block.blockentity.ritual.structure.StructureRing;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 儀式結構藍圖顯示與互動 Widget。
 */
class RitualBlueprintWidget implements IRecipeWidget, IJeiGuiEventListener {

    private static final int WIDTH = 68;
    private static final int HEIGHT = 60;
    private static final int GRID_PADDING = 4;
    private static final int BUTTON_HEIGHT = 11;
    private static final int BUTTON_WIDTH = 26;
    private static final int PROJECT_BUTTON_HEIGHT = 12;

    private final ScreenPosition position;
    private final RitualStructureBlueprint blueprint;
    private final RitualStructureProjectionManager manager = RitualStructureProjectionManager.getInstance();

    RitualBlueprintWidget(int x, int y, RitualStructureBlueprint blueprint) {
        this.position = new ScreenPosition(x, y);
        this.blueprint = blueprint;
    }

    @Override
    public ScreenPosition getPosition() {
        return position;
    }

    @Override
    public void drawWidget(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 背景
        guiGraphics.fill(0, 0, WIDTH, HEIGHT, 0xAA101018);
        guiGraphics.fill(0, 0, WIDTH, 1, 0xFF1F1F2A);
        guiGraphics.fill(0, HEIGHT - 1, WIDTH, HEIGHT, 0xFF1F1F2A);
        guiGraphics.fill(0, 0, 1, HEIGHT, 0xFF1F1F2A);
        guiGraphics.fill(WIDTH - 1, 0, WIDTH, HEIGHT, 0xFF1F1F2A);

        drawBlueprint(guiGraphics);
        drawButtons(guiGraphics, mouseX, mouseY);
    }

    private void drawBlueprint(GuiGraphics guiGraphics) {
        List<StructureElement> elements = filteredElements();
        if (elements.isEmpty()) {
            return;
        }

        int minX = elements.stream().mapToInt(e -> e.offset().getX()).min().orElse(0);
        int maxX = elements.stream().mapToInt(e -> e.offset().getX()).max().orElse(0);
        int minZ = elements.stream().mapToInt(e -> e.offset().getZ()).min().orElse(0);
        int maxZ = elements.stream().mapToInt(e -> e.offset().getZ()).max().orElse(0);

        int columns = maxX - minX + 1;
        int rows = maxZ - minZ + 1;

        int gridWidth = WIDTH - GRID_PADDING * 2;
        int gridHeight = HEIGHT - GRID_PADDING * 2 - 20; // 預留按鈕區域

        int cell = Math.max(4, Math.min(gridWidth / columns, gridHeight / rows));
        int actualWidth = cell * columns;
        int actualHeight = cell * rows;

        int startX = GRID_PADDING + (gridWidth - actualWidth) / 2;
        int startY = GRID_PADDING + (gridHeight - actualHeight) / 2;

        // 格線背景
        guiGraphics.fill(startX - 2, startY - 2, startX + actualWidth + 2, startY + actualHeight + 2, 0x33000000);

        for (int i = 0; i <= columns; i++) {
            int x = startX + i * cell;
            guiGraphics.fill(x, startY, x + 1, startY + actualHeight, 0x33101010);
        }
        for (int i = 0; i <= rows; i++) {
            int y = startY + i * cell;
            guiGraphics.fill(startX, y, startX + actualWidth, y + 1, 0x33101010);
        }

        for (StructureElement element : elements) {
            int relX = element.offset().getX() - minX;
            int relZ = maxZ - element.offset().getZ();

            int x = startX + relX * cell;
            int y = startY + relZ * cell;
            int color = colorFor(element.type());

            guiGraphics.fill(x + 1, y + 1, x + cell - 1, y + cell - 1, color);
            guiGraphics.fill(x + 1, y + 1, x + cell - 1, y + 2, 0x33FFFFFF);
            guiGraphics.fill(x + 1, y + cell - 2, x + cell - 1, y + cell - 1, 0x33000000);
        }

        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("jei.koniava.ritual.blueprint.title"), GRID_PADDING, GRID_PADDING - 5, 0xFFE0E0E0, false);
    }

    private void drawButtons(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        boolean ring2 = manager.isRingEnabled(StructureRing.RING2);
        boolean ring3 = manager.isRingEnabled(StructureRing.RING3);
        boolean overRing2 = contains(ring2Area(), mouseX, mouseY);
        boolean overRing3 = contains(ring3Area(), mouseX, mouseY);
        boolean overProject = contains(projectArea(), mouseX, mouseY);

        drawToggle(guiGraphics, ring2Area(), ring2, overRing2, Component.translatable("jei.koniava.ritual.toggle_ring2"));
        drawToggle(guiGraphics, ring3Area(), ring3, overRing3, Component.translatable("jei.koniava.ritual.toggle_ring3"));
        drawAction(guiGraphics, projectArea(), overProject, Component.translatable("jei.koniava.ritual.project"));
    }

    private void drawToggle(GuiGraphics graphics, ScreenRectangle area, boolean enabled, boolean hovered, Component label) {
        int background = enabled ? 0xFF3C6E71 : 0xFF3A3A44;
        if (hovered) {
            background = enabled ? 0xFF4D8890 : 0xFF4A4A55;
        }
        graphics.fill(area.left(), area.top(), area.right(), area.bottom(), background);
        graphics.drawString(Minecraft.getInstance().font, label, area.left() + 4, area.top() + 2, 0xFFEFEFEF, false);
    }

    private void drawAction(GuiGraphics graphics, ScreenRectangle area, boolean hovered, Component label) {
        int background = hovered ? 0xFF6B7FD6 : 0xFF5465C1;
        graphics.fill(area.left(), area.top(), area.right(), area.bottom(), background);
        graphics.drawString(Minecraft.getInstance().font, label, area.left() + 6, area.top() + 3, 0xFFFFFFFF, false);
    }

    private List<StructureElement> filteredElements() {
        List<StructureElement> filtered = new ArrayList<>();
        for (StructureElement element : blueprint.elements()) {
            if (element.type() == StructureElementType.PEDESTAL) {
                if (element.ring() == StructureRing.RING2 && !manager.isRingEnabled(StructureRing.RING2)) {
                    continue;
                }
                if (element.ring() == StructureRing.RING3 && !manager.isRingEnabled(StructureRing.RING3)) {
                    continue;
                }
            }
            filtered.add(element);
        }
        return filtered;
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        if (contains(ring2Area(), mouseX, mouseY)) {
            tooltip.add(Component.translatable("jei.koniava.ritual.toggle_ring2.desc"));
        } else if (contains(ring3Area(), mouseX, mouseY)) {
            tooltip.add(Component.translatable("jei.koniava.ritual.toggle_ring3.desc"));
        } else if (contains(projectArea(), mouseX, mouseY)) {
            tooltip.add(Component.translatable("jei.koniava.ritual.project.desc"));
        }
    }

    @Override
    public ScreenRectangle getArea() {
        return new ScreenRectangle(position.x(), position.y(), WIDTH, HEIGHT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (contains(ring2Area(), mouseX, mouseY)) {
            boolean next = !manager.isRingEnabled(StructureRing.RING2);
            manager.setRingEnabled(StructureRing.RING2, next);
            return true;
        }
        if (contains(ring3Area(), mouseX, mouseY)) {
            boolean next = !manager.isRingEnabled(StructureRing.RING3);
            manager.setRingEnabled(StructureRing.RING3, next);
            return true;
        }
        if (contains(projectArea(), mouseX, mouseY)) {
            manager.toggleProjection(blueprint);
            return true;
        }
        return false;
    }

    private ScreenRectangle ring2Area() {
        return new ScreenRectangle(6, HEIGHT - 24, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private ScreenRectangle ring3Area() {
        return new ScreenRectangle(6 + BUTTON_WIDTH + 6, HEIGHT - 24, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private ScreenRectangle projectArea() {
        return new ScreenRectangle(6, HEIGHT - PROJECT_BUTTON_HEIGHT - 6, WIDTH - 12, PROJECT_BUTTON_HEIGHT);
    }

    private int colorFor(StructureElementType type) {
        return switch (type) {
            case CORE -> 0xFF5564E3;
            case PEDESTAL -> 0xFF3DA6F5;
            case PYLON -> 0xFFF26CF7;
            case RUNE -> 0xFFF6D05C;
            case GLYPH -> 0xFFEFEFEF;
        };
    }

    private boolean contains(ScreenRectangle area, double mouseX, double mouseY) {
        return mouseX >= area.left() && mouseX < area.right()
                && mouseY >= area.top() && mouseY < area.bottom();
    }
}
