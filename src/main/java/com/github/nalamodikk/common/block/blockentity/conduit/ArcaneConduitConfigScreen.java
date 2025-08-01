package com.github.nalamodikk.common.block.blockentity.conduit;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.button.TooltipButton;
import com.github.nalamodikk.common.network.packet.server.manatool.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.conduit.PriorityUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.conduit.ResetPrioritiesPacket;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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

    // 🔧 改用 EditBox 替代滑桿
    private final EnumMap<Direction, EditBox> priorityInputs = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, TooltipButton> ioButtons = new EnumMap<>(Direction.class);

    public ArcaneConduitConfigScreen(ArcaneConduitConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 200;
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
                x + 60, y, 20, 20,
                getIOTypeLabel(menu.getIOType(dir)),
                getIOTypeTexture(menu.getIOType(dir)),
                20, 20,
                button -> onIOButtonClick(dir),
                () -> List.of(Component.translatable("tooltip.koniava.io_type", menu.getIOType(dir).name().toLowerCase()))
        );

        // 🆕 優先級文字輸入框 (修改後)
        EditBox priorityInput = new EditBox(
                this.font,
                x + 120, y, 80, 18,  // ✅ 寬度從60增加到80，容納更長的數字
                Component.translatable("gui.koniava.priority_input")
        );

        // 🔧 設定輸入框屬性 (修改後)
        priorityInput.setValue(String.valueOf(menu.getPriority(dir)));
        priorityInput.setMaxLength(12); // ✅ 從6位增加到12位，支援完整Integer範圍 (-2147483648)
        priorityInput.setFilter(this::isValidPriorityInput); // ✅ 使用修改後的驗證方法
        priorityInput.setResponder(value -> onPriorityChanged(dir, value)); // 當數值改變時

        // 🆕 添加工具提示 (修改後)
        priorityInput.setTooltip(Tooltip.create(
                Component.translatable("tooltip.koniava.priority_input") // ✅ 工具提示文字已在語言檔案中更新
        ));

        ioButtons.put(dir, ioButton);
        priorityInputs.put(dir, priorityInput);

        this.addRenderableWidget(ioButton);
        this.addRenderableWidget(priorityInput);
    }

    // 🆕 驗證輸入是否為有效的優先級數字
    private boolean isValidPriorityInput(String input) {
        if (input.isEmpty()) {
            return true; // 允許空白（用於編輯過程中）
        }

        // ✅ 允許負數，支援完整Integer範圍
        if (input.equals("-")) {
            return true; // 允許輸入負號
        }

        try {
            long value = Long.parseLong(input);
            // ✅ 檢查是否在Integer範圍內
            return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
        } catch (NumberFormatException e) {
            return false; // 不是數字
        }
    }

    // 🆕 當優先級輸入改變時
    private void onPriorityChanged(Direction dir, String value) {
        if (value.isEmpty()) {
            return; // 空白時不處理
        }

        try {
            long longPriority = Long.parseLong(value);
            // ✅ 確保在Integer範圍內
            if (longPriority >= Integer.MIN_VALUE && longPriority <= Integer.MAX_VALUE) {
                int priority = (int) longPriority;
                // 🔧 發送封包更新優先級
                PacketDistributor.sendToServer(new PriorityUpdatePacket(menu.getConduitPos(), dir, priority));
            }
        } catch (NumberFormatException e) {
            // 無效輸入，恢復原值
            EditBox input = priorityInputs.get(dir);
            if (input != null) {
                input.setValue(String.valueOf(menu.getPriority(dir)));
            }
        }
    }

    // 🆕 獨立的 IO 按鈕點擊處理方法
    private void onIOButtonClick(Direction dir) {
        // 循環切換 IO 類型
        IOHandlerUtils.IOType currentType = menu.getIOType(dir);
        IOHandlerUtils.IOType nextType = getNextIOType(currentType);

        // 🔧 發送封包到伺服器
        PacketDistributor.sendToServer(new ConfigDirectionUpdatePacket(menu.getConduitPos(), dir, nextType));
    }

    // 🔧 修復關閉按鈕的 NullPointerException 問題

    private void addGlobalControls() {
        // 重置所有優先級按鈕
        TooltipButton resetButton = new TooltipButton(
                leftPos + 20, topPos + 165, 80, 20,
                Component.translatable("button.koniava.reset_priorities"),
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/reset_button.png"),
                80, 20,
                button -> {
                    PacketDistributor.sendToServer(new ResetPrioritiesPacket(menu.getConduitPos()));
                    // 🆕 重置後更新所有輸入框顯示
                    for (Direction dir : Direction.values()) {
                        EditBox input = priorityInputs.get(dir);
                        if (input != null && !input.isFocused()) {
                            input.setValue("0"); // ✅ 重置為0
                        }
                    }
                },
                () -> List.of(Component.translatable("tooltip.koniava.reset_priorities"))
        );

        // 🔧 修復關閉按鈕的安全問題
        TooltipButton closeButton = new TooltipButton(
                leftPos + 110, topPos + 165, 60, 20,
                Component.translatable("button.koniava.close"),
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/close_button.png"),
                60, 20,
                button -> {
                    // 🔧 安全的關閉方式
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(null);
                    } else {
                        // 備用關閉方式
                        this.onClose();
                    }
                },
                () -> List.of(Component.translatable("tooltip.koniava.close"))
        );

        this.addRenderableWidget(resetButton);
        this.addRenderableWidget(closeButton);
    }


    @Override
    protected void containerTick() {
        super.containerTick();
        updateAllControls();
    }

    // 🔧 更新所有控件

    private void updateAllControls() {
        for (Direction dir : Direction.values()) {
            // 更新 IO 按鈕 - 使用您現有的 TooltipButton
            TooltipButton ioButton = ioButtons.get(dir);
            if (ioButton != null) {
                IOHandlerUtils.IOType currentType = menu.getIOType(dir);
                ioButton.setMessage(getIOTypeLabel(currentType));
                ioButton.setTexture(getIOTypeTexture(currentType), 20, 20);
            }

            // 🔧 修復：更新優先級輸入框時檢查焦點狀態
            EditBox priorityInput = priorityInputs.get(dir);
            if (priorityInput != null) {
                int currentPriority = menu.getPriority(dir);
                String currentValue = priorityInput.getValue();
                String newValue = String.valueOf(currentPriority);

                // 🔧 關鍵修復：只有在輸入框沒有焦點且值不同時才更新
                if (!priorityInput.isFocused() && !currentValue.equals(newValue)) {
                    priorityInput.setValue(newValue);
                }
            }
        }
    }

    // 🔧 新增：檢查輸入框是否有選中的文字


    // 🔧 處理鍵盤輸入
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 先讓輸入框處理鍵盤事件
        for (EditBox input : priorityInputs.values()) {
            if (input.isFocused() && input.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        // 如果輸入框沒有處理，則使用預設處理
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // 先讓輸入框處理字符輸入
        for (EditBox input : priorityInputs.values()) {
            if (input.isFocused() && input.charTyped(codePoint, modifiers)) {
                return true;
            }
        }

        return super.charTyped(codePoint, modifiers);
    }

    // 輔助方法
    private Component getIOTypeLabel(IOHandlerUtils.IOType type) {
        return Component.translatable("mode.koniava." + type.name().toLowerCase());
    }

    private ResourceLocation getIOTypeTexture(IOHandlerUtils.IOType type) {
        String textureName = switch (type) {
            case INPUT -> "button_config_input";
            case OUTPUT -> "button_config_output";
            case BOTH -> "button_config_both";
            case DISABLED -> "button_config_disabled";
        };
        return ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/" + textureName + ".png");
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
        // 🎨 使用材質渲染背景
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // 🔧 如果材質檔案不存在，可以使用備用方案
        // guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
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
            guiGraphics.drawString(font, label, 8, 35 + dir.ordinal() * 22, 0xFFFFFF, false);
        }

        // 列標題
        guiGraphics.drawString(font, Component.translatable("gui.koniava.io_type"), 75, 14, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.translatable("gui.koniava.priority"), 140, 14, 0xFFFFFF, false);

        // 🆕 添加輸入提示
        guiGraphics.drawString(font, Component.literal("(±2B)"), 185, 14, 0xAAAAAA, false);
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