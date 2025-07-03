package com.github.nalamodikk.common.block.conduit;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.button.TooltipButton;
import com.github.nalamodikk.common.network.packet.server.manatool.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.conduit.PriorityUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.conduit.ResetPrioritiesPacket;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.List;

public class ArcaneConduitConfigScreen extends AbstractContainerScreen<ArcaneConduitConfigMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "textures/gui/conduit_config.png"
    );

    // 使用你現有的 TooltipButton 系統
    private final EnumMap<Direction, CustomPrioritySlider> prioritySliders = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, TooltipButton> ioButtons = new EnumMap<>(Direction.class);

    public ArcaneConduitConfigScreen(ArcaneConduitConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 180;
    }

    @Override
    protected void init() {
        super.init();

        int startX = leftPos + 20;
        int startY = topPos + 30;

        // 為每個方向創建配置控件
        for (Direction dir : Direction.values()) {
            createDirectionConfig(dir, startX, startY + dir.ordinal() * 22);
        }

        // 添加全局按鈕
        addGlobalControls();
    }

    private void createDirectionConfig(Direction dir, int x, int y) {
        // IO 類型按鈕 - 使用你現有的 TooltipButton
        TooltipButton ioButton = new TooltipButton(
                x + 60, y, 50, 18,
                getIOTypeLabel(menu.getIOType(dir)),
                getIOTypeTexture(menu.getIOType(dir)),
                50, 18,
                button -> onIOButtonClick(dir),
                () -> List.of(Component.translatable("tooltip.koniava.io_type", menu.getIOType(dir).name().toLowerCase()))
        );

        // 自定義優先級滑桿
        CustomPrioritySlider prioritySlider = new CustomPrioritySlider(
                x + 120, y, 80, 18,
                dir,
                menu.getPriority(dir)
        );

        ioButtons.put(dir, ioButton);
        prioritySliders.put(dir, prioritySlider);

        this.addRenderableWidget(ioButton);
        this.addRenderableWidget(prioritySlider);
    }

    // 🆕 獨立的 IO 按鈕點擊處理方法
    private void onIOButtonClick(Direction dir) {
        // 循環切換 IO 類型
        IOHandlerUtils.IOType currentType = menu.getIOType(dir);
        IOHandlerUtils.IOType nextType = getNextIOType(currentType);

        // 🔧 修正：使用 PacketDistributor.sendToServer 發送封包
        PacketDistributor.sendToServer(new ConfigDirectionUpdatePacket(menu.getConduitPos(), dir, nextType));

        // 更新按鈕顯示
        TooltipButton button = ioButtons.get(dir);
        if (button != null) {
            button.setMessage(getIOTypeLabel(nextType));
            button.setTexture(getIOTypeTexture(nextType), 50, 18);
        }
    }

    private void addGlobalControls() {
        // 重置所有優先級按鈕 - 使用 TooltipButton
        TooltipButton resetButton = new TooltipButton(
                leftPos + 20, topPos + 150, 80, 20,
                Component.translatable("button.koniava.reset_priorities"),
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/reset_button.png"),
                80, 20,
                button -> {
                    // 🔧 修正：使用 PacketDistributor.sendToServer 發送封包
                    PacketDistributor.sendToServer(new ResetPrioritiesPacket(menu.getConduitPos()));
                    // 滑桿會通過同步數據自動更新
                },
                () -> List.of(Component.translatable("tooltip.koniava.reset_priorities"))
        );

        // 關閉按鈕
        TooltipButton closeButton = new TooltipButton(
                leftPos + 110, topPos + 150, 60, 20,
                Component.translatable("button.koniava.close"),
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/close_button.png"),
                60, 20,
                button -> this.minecraft.setScreen(null),
                () -> List.of(Component.translatable("tooltip.koniava.close"))
        );

        this.addRenderableWidget(resetButton);
        this.addRenderableWidget(closeButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        // 每個tick更新滑桿顯示
        for (Direction dir : Direction.values()) {
            CustomPrioritySlider slider = prioritySliders.get(dir);
            if (slider != null) {
                slider.updateFromMenu(menu.getPriority(dir));
            }
        }
    }

    // 自定義優先級滑桿類
    private class CustomPrioritySlider extends AbstractSliderButton {
        private final Direction direction;
        private int currentPriority;

        public CustomPrioritySlider(int x, int y, int width, int height, Direction direction, int initialPriority) {
            super(x, y, width, height, Component.literal(String.valueOf(initialPriority)), (initialPriority - 1) / 99.0);
            this.direction = direction;
            this.currentPriority = initialPriority;
        }

        @Override
        protected void updateMessage() {
            this.currentPriority = (int) (this.value * 99) + 1;
            this.setMessage(Component.literal(String.valueOf(currentPriority)));
        }

        @Override
        protected void applyValue() {
            this.currentPriority = (int) (this.value * 99) + 1;
            // 🔧 修正：使用 PacketDistributor.sendToServer 發送封包
            PacketDistributor.sendToServer(new PriorityUpdatePacket(menu.getConduitPos(), direction, currentPriority));
        }

        public void updateFromMenu(int newPriority) {
            if (this.currentPriority != newPriority) {
                this.currentPriority = newPriority;
                this.value = (newPriority - 1) / 99.0;
                this.updateMessage();
            }
        }
    }

    // 輔助方法
    private Component getIOTypeLabel(IOHandlerUtils.IOType type) {
        return Component.translatable("mode.koniava." + type.name().toLowerCase());
    }

    private ResourceLocation getIOTypeTexture(IOHandlerUtils.IOType type) {
        String textureName = switch (type) {
            case INPUT -> "input";
            case OUTPUT -> "output";
            case BOTH -> "both";
            case DISABLED -> "disabled";
        };
        return ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/io_" + textureName + ".png");
    }

    private IOHandlerUtils.IOType getNextIOType(IOHandlerUtils.IOType current) {
        return switch (current) {
            case DISABLED -> IOHandlerUtils.IOType.INPUT;
            case INPUT -> IOHandlerUtils.IOType.OUTPUT;
            case OUTPUT -> IOHandlerUtils.IOType.BOTH;
            case BOTH -> IOHandlerUtils.IOType.DISABLED;
        };
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 渲染背景（暫時使用純色，之後可以添加紋理）
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xCC000000);

        // 渲染邊框
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 2, 0xFFFFFFFF);
        guiGraphics.fill(leftPos, topPos + imageHeight - 2, leftPos + imageWidth, topPos + imageHeight, 0xFFFFFFFF);
        guiGraphics.fill(leftPos, topPos, leftPos + 2, topPos + imageHeight, 0xFFFFFFFF);
        guiGraphics.fill(leftPos + imageWidth - 2, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 標題
        Component title = Component.translatable("gui.koniava.conduit_config");
        guiGraphics.drawString(font, title, 8, 6, 0xFFFFFF, false);

        // 各方向標籤
        for (Direction dir : Direction.values()) {
            String dirName = dir.name().toLowerCase();
            Component label = Component.translatable("direction.koniava." + dirName);
            guiGraphics.drawString(font, label, 8, 24 + dir.ordinal() * 22, 0xFFFFFF, false);
        }

        // 列標題
        guiGraphics.drawString(font, Component.translatable("gui.koniava.io_type"), 60, 14, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.translatable("gui.koniava.priority"), 120, 14, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染工具提示
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}