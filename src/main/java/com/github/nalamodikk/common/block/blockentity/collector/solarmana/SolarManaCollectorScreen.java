// 🔧 完整修復的 SolarManaCollectorScreen.java

package com.github.nalamodikk.common.block.blockentity.collector.solarmana;

import com.github.nalamodikk.client.screenAPI.component.button.TooltipButton;
import com.github.nalamodikk.client.screenAPI.TooltipSupplier;
import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.server.OpenUpgradeGuiPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SolarManaCollectorScreen extends AbstractContainerScreen<SolarManaCollectorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/solar_mana_collector_gui.png");
    private static final ResourceLocation MANA_BAR_FULL = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_bar_full.png");
    private static final int MANA_BAR_HEIGHT = 47;
    private static final int MANA_BAR_WIDTH = 7;
    private final int imageWidth = 176;
    private final int imageHeight = 166;

    public SolarManaCollectorScreen(SolarManaCollectorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    // 🔧 修復版：統一使用同步數據

    // 🔧 最簡單粗暴的解決方案 - 只改這一個方法

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 背景
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // 太陽圖示資料
        final int sunSrcX = 176;
        final int sunSrcY = 2;
        final int sunW = 39;
        final int sunH = 38;
        final int targetX = this.leftPos + 69;
        final int targetY = this.topPos + 24;

        // 🎯 最簡單的判斷：只看發電狀態
        boolean isGenerating = menu.isGenerating();

        // 🎨 簡單的顏色邏輯
        if (!isGenerating) {
            guiGraphics.setColor(0.5f, 0.5f, 0.5f, 1.0f);
        }

        // 繪製太陽圖示
        guiGraphics.blit(TEXTURE, targetX, targetY, sunSrcX, sunSrcY, sunW, sunH);

        // 恢復顏色
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        // 魔力條繪製
        drawManaBar(guiGraphics, 11, 19);
    }

    // 🔧 統一的太陽能狀態提示邏輯
    private Component getSolarTooltip() {
        boolean isDaytime = menu.isDaytime();
        boolean isGenerating = menu.isGenerating();

        // 🎯 統一的邏輯判斷
        if (isGenerating) {
            return Component.translatable("tooltip.koniava.solar.generating");
        } else if (!isDaytime) {
            return Component.translatable("tooltip.koniava.solar.nighttime");
        } else {
            // 是白天但不發電 = 被遮擋或下雨
            return Component.translatable("tooltip.koniava.solar.blocked");
        }
    }

    // 🆕 添加缺失的太陽圖示懸停檢測方法
    private boolean isHoveringSun(int mouseX, int mouseY) {
        final int targetX = this.leftPos + 69;
        final int targetY = this.topPos + 24;
        final int sunW = 39;
        final int sunH = 38;

        return mouseX >= targetX && mouseX <= targetX + sunW &&
                mouseY >= targetY && mouseY <= targetY + sunH;
    }

    private boolean isHoveringManaBar(int mouseX, int mouseY) {
        int manaBarX = this.leftPos + 11;
        int manaBarY = this.topPos + 19;
        return mouseX >= manaBarX && mouseX <= manaBarX + MANA_BAR_WIDTH &&
                mouseY >= manaBarY && mouseY <= manaBarY + MANA_BAR_HEIGHT;
    }

    private void drawManaBar(GuiGraphics pGuiGraphics, int xOffset, int yOffset) {
        int manaBarHeight = 47;
        int manaBarWidth = 8;
        int mana = this.menu.getManaStored(); // 從 containerData 中獲取魔力值
        int maxMana = this.menu.getMaxMana();

        if (maxMana > 0 && mana > 0) {
            int renderHeight = (int) (((float) mana / maxMana) * manaBarHeight); // 計算應該渲染的高度
            RenderSystem.setShaderTexture(0, MANA_BAR_FULL); // 設置魔力條的紋理
            pGuiGraphics.blit(MANA_BAR_FULL, this.leftPos + xOffset, this.topPos + yOffset + (manaBarHeight - renderHeight),
                    49, 11, manaBarWidth, renderHeight);
        }
    }

    @Override
    protected void init() {
        super.init();

        // 📌 升級按鈕 Tooltip（支援滑鼠座標）
        TooltipSupplier.Positioned tooltip = (mouseX, mouseY) -> List.of(
                Component.translatable("screen.koniava.upgrade_button.tooltip")
        );

        // 📌 升級按鈕元件
        this.addRenderableWidget(new TooltipButton(
                this.leftPos + 150, this.topPos + 5, // 位置
                18, 18, // 尺寸
                Component.empty(),
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/upgrade_button.png"),
                18, 18, // 紋理尺寸
                button -> {
                    // 傳送封包：打開 Upgrade GUI
                    BlockPos pos = this.menu.getBlockEntity().getBlockPos();
                    OpenUpgradeGuiPacket.sendToServer(pos);
                },
                tooltip
        ));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 背景畫面
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 🎯 獲取升級等級
        int speedLevel = menu.getSpeedLevel();
        int efficiencyLevel = menu.getEfficiencyLevel();

        // 🎨 根據升級數量決定顏色
        int speedColor = speedLevel > 0 ? 0xFFFFFF : 0x666666;
        int effColor = efficiencyLevel > 0 ? 0xFFFFFF : 0x666666;

        Component speedLabel = Component.translatable("screen.koniava.upgrade.speed", speedLevel);
        Component efficiencyLabel = Component.translatable("screen.koniava.upgrade.efficiency", efficiencyLevel);

        float scale = 0.8f;
        int drawX = leftPos + 22;
        int drawY1 = topPos + 20;
        int drawY2 = topPos + 30;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);

        guiGraphics.drawString(font, speedLabel,
                (int)(drawX / scale), (int)(drawY1 / scale), speedColor, false);
        guiGraphics.drawString(font, efficiencyLabel,
                (int)(drawX / scale), (int)(drawY2 / scale), effColor, false);

        guiGraphics.pose().popPose();

        // 工具提示
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // 🔧 魔力條提示
        if (isHoveringManaBar(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("tooltip.mana", this.menu.getManaStored(), this.menu.getMaxMana()),
                    mouseX, mouseY);
        }

        // 🆕 太陽圖示提示 - 完全基於服務器同步狀態
        if (isHoveringSun(mouseX, mouseY)) {
            Component tooltip = getSolarTooltip();
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public @Nullable Slot getSlotUnderMouse() {
        return super.getSlotUnderMouse();
    }
}
