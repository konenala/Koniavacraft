package com.github.nalamodikk.common.block.blockentity.mana_infuser;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 🔮 魔力注入機 GUI 界面
 */
public class ManaInfuserScreen extends AbstractContainerScreen<ManaInfuserMenu> {

    // GUI 材質位置
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_infuser_gui.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 進度條位置和尺寸
    private static final int PROGRESS_BAR_X = 78;
    private static final int PROGRESS_BAR_Y = 35;
    private static final int PROGRESS_BAR_WIDTH = 34;
    private static final int PROGRESS_BAR_HEIGHT = 11;

    // 魔力條位置和尺寸
    private static final int MANA_BAR_X = 8;
    private static final int MANA_BAR_Y = 18;
    private static final int MANA_BAR_WIDTH = 10;
    private static final int MANA_BAR_HEIGHT = 48;

    public ManaInfuserScreen(ManaInfuserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 繪製背景
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // 繪製進度條
        renderProgressBar(guiGraphics);

        // 繪製魔力條
        renderManaBar(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 繪製工具提示
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * 🔄 繪製進度條
     */
    private void renderProgressBar(GuiGraphics guiGraphics) {
        if (menu.isWorking()) {
            int progress = menu.getProgressPercentage();
            int progressPixels = (progress * PROGRESS_BAR_WIDTH) / 100;

            // 繪製進度條填充 (材質中進度條部分的UV座標)
            guiGraphics.blit(TEXTURE,
                    this.leftPos + PROGRESS_BAR_X,
                    this.topPos + PROGRESS_BAR_Y,
                    176, 52, // UV座標 (材質右側的進度條圖案)
                    progressPixels,
                    PROGRESS_BAR_HEIGHT);
        }
    }

    /**
     * ⚡ 繪製魔力條
     */
    private void renderManaBar(GuiGraphics guiGraphics) {
        int manaPercentage = menu.getManaPercentage();
        int manaPixels = (manaPercentage * MANA_BAR_HEIGHT) / 100;

        // 從底部開始填充魔力條
        if (manaPixels > 0) {
            guiGraphics.blit(TEXTURE,
                    this.leftPos + MANA_BAR_X,
                    this.topPos + MANA_BAR_Y + (MANA_BAR_HEIGHT - manaPixels),
                    176, 1, // UV座標 (材質中魔力條的圖案)
                    MANA_BAR_WIDTH,
                    manaPixels);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);

        // 魔力條工具提示
        if (isHovering(MANA_BAR_X, MANA_BAR_Y, MANA_BAR_WIDTH, MANA_BAR_HEIGHT, x, y)) {
            Component manaTooltip = Component.translatable("gui.koniava.mana_infuser.mana",
                    menu.getCurrentMana(), menu.getMaxMana());
            guiGraphics.renderTooltip(this.font, manaTooltip, x, y);
        }

        // 進度條工具提示
        if (isHovering(PROGRESS_BAR_X, PROGRESS_BAR_Y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, x, y)) {
            if (menu.isWorking()) {
                Component progressTooltip = Component.translatable("gui.koniava.mana_infuser.progress",
                        menu.getProgressPercentage());
                guiGraphics.renderTooltip(this.font, progressTooltip, x, y);
            } else {
                Component statusTooltip = Component.translatable("gui.koniava.mana_infuser.status.idle");
                guiGraphics.renderTooltip(this.font, statusTooltip, x, y);
            }
        }
    }

    /**
     * 🖱️ 檢查滑鼠是否懸停在指定區域
     */
    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + width &&
                mouseY >= this.topPos + y && mouseY < this.topPos + y + height;
    }
}