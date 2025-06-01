package com.github.nalamodikk.common.screen.shared;

import com.github.nalamodikk.client.screenAPI.GenericButtonWithTooltip;
import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.packet.server.manatool.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
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

public class UniversalConfigScreen extends AbstractContainerScreen<UniversalConfigMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/universal_config.png");
    private static final ResourceLocation BUTTON_TEXTURE_INPUT = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/button_config_input.png");
    private static final ResourceLocation BUTTON_TEXTURE_OUTPUT = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/button_config_output.png");
    private static final ResourceLocation BUTTON_TEXTURE_BOTH = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/button_config_both.png");
    private static final ResourceLocation BUTTON_TEXTURE_DISABLED = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/button_config_disabled.png");
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;
    private final EnumMap<Direction, GenericButtonWithTooltip> directionButtonMap = new EnumMap<>(Direction.class);

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

        updateCurrentConfigFromBlockEntity();
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

        directionOffsets.put(up,    new int[]{0, -60});
        directionOffsets.put(down,  new int[]{0, 60});
        directionOffsets.put(front, new int[]{0, -20});
        directionOffsets.put(back,  new int[]{0, 90});
        directionOffsets.put(left,  new int[]{-60, 0});
        directionOffsets.put(right, new int[]{60, 0});

        directionButtonMap.clear(); // ← 清空舊的映射

        for (Direction direction : Direction.values()) {
            if (directionOffsets.containsKey(direction)) {
                int[] offset = directionOffsets.get(direction);
                int buttonX = baseX + offset[0];
                int buttonY = baseY + offset[1];

                IOHandlerUtils.IOType type = currentIOMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);

                ResourceLocation currentTexture = switch (type) {
                    case INPUT -> BUTTON_TEXTURE_INPUT;
                    case OUTPUT -> BUTTON_TEXTURE_OUTPUT;
                    case BOTH -> BUTTON_TEXTURE_BOTH;
                    case DISABLED -> BUTTON_TEXTURE_DISABLED;
                };

                Component label = Component.translatable("direction.magical_industry." + direction.getName());

                GenericButtonWithTooltip button = new GenericButtonWithTooltip(
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

    private void updateCurrentConfigFromBlockEntity() {
        if (blockEntity instanceof IConfigurableBlock configurableBlock) {
            for (Direction direction : Direction.values()) {
                currentIOMap.put(direction, configurableBlock.getIOConfig(direction)); // ✅ 直接用 IOType
            }
        }
    }

    private void updateAllButtonTooltipsAndTextures() {
        for (Direction direction : Direction.values()) {
            GenericButtonWithTooltip button = getButtonByDirection(direction);
            if (button != null) {
                updateButtonTooltip(button, direction);
                updateButtonTexture(button, direction);
            }
        }
    }

    private GenericButtonWithTooltip getButtonByDirection(Direction direction) {
        return directionButtonMap.get(direction);
    }


    private void onDirectionConfigButtonClick(Direction direction) {
        if (blockEntity instanceof IConfigurableBlock configurableBlock) {
            // 取得目前 IOType 並循環切換
            IOHandlerUtils.IOType current = configurableBlock.getIOConfig(direction);
            IOHandlerUtils.IOType next = IOHandlerUtils.nextIOType(current);

            // 更新本地狀態（按鈕顯示用）
            currentIOMap.put(direction, next); // ← 你可能要把 currentConfig 改成 currentIOMap<Direction, IOType>

            // 實際設定
            configurableBlock.setIOConfig(direction, next);
            blockEntity.setChanged();

            // 發送封包給伺服器同步 IO 設定
            PacketDistributor.sendToServer(new ConfigDirectionUpdatePacket(blockEntity.getBlockPos(), direction, next));

            // 顯示玩家通知（用本地化）
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(
                    "message.magical_industry.config_button_clicked",
                    direction.getName(),
                    Component.translatable("mode.magical_industry." + next.name().toLowerCase()) // 本地化鍵如 mode.magical_industry.output
            ), true);

            updateAllButtonTooltipsAndTextures();
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

                    if (MagicalIndustryMod.IS_DEV) {
                        MagicalIndustryMod.LOGGER.info("[Client] Changed direction: {} → {}, sending packet", direction.getName(), newValue.name());
                    }
                }
            }
        }
    }



    private void updateButtonTooltip(GenericButtonWithTooltip button, Direction direction) {
        button.setTooltip(Tooltip.create(getTooltipText(direction)));
    }

    private void updateButtonTexture(GenericButtonWithTooltip button, Direction direction) {
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

        // 取得類型名稱小寫（input、output、both、disabled）
        String configType = type.name().toLowerCase();

        return Component.translatable("screen.magical_industry.configure_side", direction.getName())
                .append(" ")
                .append(Component.translatable("screen.magical_industry." + configType));
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
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int mouseX, int mouseY) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752);
    }

    private boolean renderButtonTooltips(GuiGraphics pGuiGraphics, int mouseX, int mouseY) {
        for (Object widget : this.renderables) {
            if (widget instanceof GenericButtonWithTooltip button) {
                if (button.isMouseOver(mouseX, mouseY)) {
                    Tooltip tooltip = button.getTooltip();
                    if (tooltip != null) {
                        List<FormattedCharSequence> formattedComponents = tooltip.toCharSequence(Minecraft.getInstance());
                        if (!formattedComponents.isEmpty()) {
                            pGuiGraphics.renderTooltip(Minecraft.getInstance().font, formattedComponents, mouseX, mouseY);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
