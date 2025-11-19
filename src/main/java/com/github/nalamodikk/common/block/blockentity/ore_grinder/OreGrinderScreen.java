package com.github.nalamodikk.common.block.blockentity.ore_grinder;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * ⚙️ 粉碎機 GUI 界面
 *
 * 簡單設計：
 * - 背景材質
 * - 進度條
 * - 魔力條
 * - 6 個物品槽位（自動渲染）
 */
public class OreGrinderScreen extends AbstractContainerScreen<OreGrinderMenu> {

    // GUI 材質位置
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/ore_grinder_gui.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 222;

    // 進度條位置和尺寸
    private static final int PROGRESS_BAR_X = 79;
    private static final int PROGRESS_BAR_Y = 35;
    private static final int PROGRESS_BAR_WIDTH = 26;
    private static final int PROGRESS_BAR_HEIGHT = 16;

    // 魔力條位置和尺寸
    private static final int MANA_BAR_X = 9;
    private static final int MANA_BAR_Y = 17;
    private static final int MANA_BAR_WIDTH = 10;
    private static final int MANA_BAR_HEIGHT = 48;

    public OreGrinderScreen(OreGrinderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // 如果需要的話，在這裡添加按鈕或其他小工具
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

            // 從材質中的進度條部分截取 (UV 座標)
            guiGraphics.blit(TEXTURE,
                    this.leftPos + PROGRESS_BAR_X,
                    this.topPos + PROGRESS_BAR_Y,
                    176, 0,  // UV 座標 (材質右側的進度條圖案)
                    progressPixels,
                    PROGRESS_BAR_HEIGHT);
        }
    }

    /**
     * ⚡ 繪製魔力條
     */
    private void renderManaBar(GuiGraphics guiGraphics) {
        int currentMana = menu.getCurrentMana();
        int maxMana = menu.getMaxMana();

        if (maxMana > 0) {
            int manaPixels = (currentMana * MANA_BAR_HEIGHT) / maxMana;

            // 繪製魔力條背景
            guiGraphics.fill(
                    this.leftPos + MANA_BAR_X,
                    this.topPos + MANA_BAR_Y,
                    this.leftPos + MANA_BAR_X + MANA_BAR_WIDTH,
                    this.topPos + MANA_BAR_Y + MANA_BAR_HEIGHT,
                    0xFF1A1A2E
            );

            // 繪製魔力條填充 (從上到下)
            guiGraphics.fill(
                    this.leftPos + MANA_BAR_X,
                    this.topPos + MANA_BAR_Y + (MANA_BAR_HEIGHT - manaPixels),
                    this.leftPos + MANA_BAR_X + MANA_BAR_WIDTH,
                    this.topPos + MANA_BAR_Y + MANA_BAR_HEIGHT,
                    0xFF6A5AFF
            );
        }
    }
}
