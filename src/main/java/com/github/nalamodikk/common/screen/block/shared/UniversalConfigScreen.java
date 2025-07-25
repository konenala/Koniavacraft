package com.github.nalamodikk.common.screen.block.shared;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.button.TooltipButton;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.network.packet.server.manatool.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class UniversalConfigScreen extends AbstractContainerScreen<UniversalConfigMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/universal_config.png");
    private static final ResourceLocation BUTTON_TEXTURE_INPUT = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/button_config_input.png");
    private static final ResourceLocation BUTTON_TEXTURE_OUTPUT = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/button_config_output.png");
    private static final ResourceLocation BUTTON_TEXTURE_BOTH = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/button_config_both.png");
    private static final ResourceLocation BUTTON_TEXTURE_DISABLED = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/button_config_disabled.png");
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;
    private final EnumMap<Direction, TooltipButton> directionButtonMap = new EnumMap<>(Direction.class);

    private final EnumMap<Direction, IOHandlerUtils.IOType> currentIOMap = new EnumMap<>(Direction.class);
    private final BlockEntity blockEntity;

    public UniversalConfigScreen(UniversalConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;

        for (Direction direction : Direction.values()) {
            currentIOMap.put(direction, IOHandlerUtils.IOType.DISABLED); // ✅ 初始化為禁用
        }
    }

    @Override
    protected void init() {
        super.init();
        int baseX = (this.width - this.imageWidth) / 2 + this.imageWidth / 2 - BUTTON_WIDTH / 2;
        int baseY = (this.height - this.imageHeight) / 2 + this.imageHeight / 2 - BUTTON_HEIGHT / 2;

        // ✅ 修復：初始化時直接從BlockEntity獲取最新數據
        updateCurrentConfigFromMenu();
        initDirectionButtons(baseX, baseY);
        updateAllButtonTooltipsAndTextures();
    }

    private void initDirectionButtons(int baseX, int baseY) {
        Direction facing = Direction.NORTH;
        BlockState state = blockEntity.getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        EnumMap<Direction, int[]> directionOffsets = new EnumMap<>(Direction.class);

        Direction front = facing;
        Direction back = front.getOpposite();
        Direction left = front.getCounterClockWise();
        Direction right = front.getClockWise();
        Direction up = Direction.UP;
        Direction down = Direction.DOWN;

        // 視覺 layout：
        //     [UP]
        // [LEFT][FRONT][RIGHT]
        //     [DOWN]
        //     [BACK]
        int adjustedBaseY = (this.height - this.imageHeight) / 2 + this.imageHeight / 2 - BUTTON_HEIGHT / 2 - 10;

        directionOffsets.put(Direction.UP,     new int[]{0, -50});
        directionOffsets.put(Direction.DOWN,   new int[]{0, 30});
        directionOffsets.put(front,            new int[]{0, -20});  // ✅ 改這行
        directionOffsets.put(back,             new int[]{0, 60});
        directionOffsets.put(left,             new int[]{-60, 0});
        directionOffsets.put(right,            new int[]{60, 0});

        directionButtonMap.clear(); // ← 清空舊的映射

        for (Direction direction : Direction.values()) {
            if (directionOffsets.containsKey(direction)) {
                int[] offset = directionOffsets.get(direction);
                int buttonX = baseX + offset[0];
                int buttonY = adjustedBaseY + offset[1];

                IOHandlerUtils.IOType type = currentIOMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);

                ResourceLocation currentTexture = switch (type) {
                    case INPUT -> BUTTON_TEXTURE_INPUT;
                    case OUTPUT -> BUTTON_TEXTURE_OUTPUT;
                    case BOTH -> BUTTON_TEXTURE_BOTH;
                    case DISABLED -> BUTTON_TEXTURE_DISABLED;
                };

                Component label = Component.translatable("direction.koniava." + direction.getName());

                TooltipButton button = new TooltipButton(
                        buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        label,
                        currentTexture, 20, 20,
                        btn -> onDirectionConfigButtonClick(direction),
                        () -> Collections.singletonList(getTooltipText(direction))
                );

                directionButtonMap.put(direction, button); // 🔁 儲存按鈕
                this.addRenderableWidget(button);
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        // 🔧 關鍵修復：從Menu獲取最新數據，而不是直接從BlockEntity
        updateCurrentConfigFromMenu();
    }

    private void updateCurrentConfigFromMenu() {
        try {
            boolean hasChanges = false;

            for (Direction direction : Direction.values()) {
                IOHandlerUtils.IOType syncedType = menu.getIOType(direction);
                IOHandlerUtils.IOType currentDisplayed = currentIOMap.get(direction);

                if (syncedType != currentDisplayed) {
                    currentIOMap.put(direction, syncedType);
                    hasChanges = true;
                }
            }

            // 只在有變化時才更新按鈕顯示
            if (hasChanges) {
                updateAllButtonTooltipsAndTextures();
            }
        } catch (Exception e) {
            // 使用 KoniavacraftMod.LOGGER 記錄錯誤
            KoniavacraftMod.LOGGER.error("Error updating config from menu: {}", e.getMessage(), e);
        }
    }



    private void updateAllButtonTooltipsAndTextures() {
        for (Direction direction : Direction.values()) {
            TooltipButton button = getButtonByDirection(direction);
            if (button != null) {
                updateButtonTooltip(button, direction);
                updateButtonTexture(button, direction);
            }
        }
    }


    private TooltipButton getButtonByDirection(Direction direction) {
        return directionButtonMap.get(direction);
    }


    // 🔧 修復按鈕點擊邏輯
    private void onDirectionConfigButtonClick(Direction direction) {
        try {
            if (blockEntity instanceof IConfigurableBlock configurableBlock) {
                // 🔧 使用Menu的同步數據而不是直接查詢BlockEntity
                IOHandlerUtils.IOType current = menu.getIOType(direction);
                IOHandlerUtils.IOType next = IOHandlerUtils.nextIOType(current);

                // 立即更新本地顯示
                currentIOMap.put(direction, next);

                // 更新按鈕顯示
                TooltipButton button = directionButtonMap.get(direction);
                if (button != null) {
                    updateButtonTexture(button, direction);
                    updateButtonTooltip(button, direction);
                }

                // 發送封包給伺服器
                PacketDistributor.sendToServer(new ConfigDirectionUpdatePacket(
                        blockEntity.getBlockPos(), direction, next));

                // 顯示通知
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(Component.translatable(
                            "message.koniava.config_button_clicked",
                            direction.getName(),
                            Component.translatable("mode.koniava." + next.name().toLowerCase())
                    ), true);
                }

                // 🔧 調試日誌（放在 if 區塊內）
                if (KoniavacraftMod.IS_DEV) {
                    KoniavacraftMod.LOGGER.debug("Direction config button clicked: {} -> {}",
                            direction, next);
                }
            } // ← 這裡關閉 if (blockEntity instanceof IConfigurableBlock) 區塊
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("Error handling direction config button click: {}",
                    e.getMessage(), e);
        }
    }
    @Override
    public void onClose() {
        super.onClose();

        if (blockEntity instanceof IConfigurableBlock) {
            for (Direction direction : Direction.values()) {
                IOHandlerUtils.IOType oldValue = menu.getOriginalIOMap().getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
                IOHandlerUtils.IOType newValue = currentIOMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);

                if (oldValue != newValue) {
                    PacketDistributor.sendToServer(new ConfigDirectionUpdatePacket(blockEntity.getBlockPos(), direction, newValue));

                    if (KoniavacraftMod.IS_DEV) {
                        KoniavacraftMod.LOGGER.info("[Client] Changed direction: {} → {}, sending packet", direction.getName(), newValue.name());
                    }
                }
            }
        }
    }



    private void updateButtonTooltip(TooltipButton button, Direction direction) {
        button.setTooltip(null); // 禁用靜態 tooltip
        directionButtonMap.put(direction, button);
    }

    private void updateButtonTexture(TooltipButton button, Direction direction) {
        IOHandlerUtils.IOType type = currentIOMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
        ResourceLocation newTexture = switch (type) {
            case INPUT -> BUTTON_TEXTURE_INPUT;
            case OUTPUT -> BUTTON_TEXTURE_OUTPUT;
            case BOTH -> BUTTON_TEXTURE_BOTH;
            case DISABLED -> BUTTON_TEXTURE_DISABLED;
        };
        button.setTexture(newTexture, 20, 20);
    }

    private MutableComponent getTooltipText(Direction direction) {
        IOHandlerUtils.IOType type = currentIOMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
        String configType = type.name().toLowerCase();

        Component localizedDirection = Component.translatable("direction.koniava." + direction.getName());
        Component modeText = Component.translatable("screen.koniava." + configType);

        MutableComponent tooltip = Component.translatable("screen.koniava.configure_side.full", localizedDirection, modeText);

        // 🔧 正確的 Shift 鍵檢查方式
        try {
            // 使用 Screen.hasShiftDown() 檢查實際的 Shift 鍵狀態
            if (Screen.hasShiftDown()) {
                tooltip.append("\n")
                        .append(Component.translatable("screen.koniava.debug_world_direction", direction.getName()));
            } else {
                tooltip.append("\n")
                        .append(Component.translatable("screen.koniava.hold_shift"));
            }
        } catch (Exception e) {
            // 如果出現任何錯誤，只顯示基本信息
            tooltip.append("\n")
                    .append(Component.translatable("screen.koniava.hold_shift"));
            KoniavacraftMod.LOGGER.debug("Could not check shift key state: {}", e.getMessage());
        }

        return tooltip;
    }


    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTicks) {
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTicks);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTicks);
        renderButtonTooltips(pGuiGraphics, pMouseX, pMouseY); // ✅ 加上這行

    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int mouseX, int mouseY) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752);
    }

    private boolean renderButtonTooltips(GuiGraphics pGuiGraphics, int mouseX, int mouseY) {
        for (Map.Entry<Direction, TooltipButton> entry : directionButtonMap.entrySet()) {
            Direction direction = entry.getKey();
            TooltipButton button = entry.getValue();

            if (button.isMouseOver(mouseX, mouseY)) {
                MutableComponent tooltip = getTooltipText(direction);
                List<FormattedCharSequence> formatted = Minecraft.getInstance().font.split(tooltip, 200);
                pGuiGraphics.renderTooltip(Minecraft.getInstance().font, formatted, mouseX, mouseY);
                return true;
            }
        }
        return false;
    }
}

