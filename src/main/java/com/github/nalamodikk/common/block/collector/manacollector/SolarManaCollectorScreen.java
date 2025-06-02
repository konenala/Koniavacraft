package com.github.nalamodikk.common.block.collector.manacollector;

import com.github.nalamodikk.client.screenAPI.GenericButtonWithTooltip;
import com.github.nalamodikk.client.screenAPI.TooltipSupplier;
import com.github.nalamodikk.MagicalIndustryMod;
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
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/solar_mana_collector_gui.png");
    private static final ResourceLocation MANA_BAR_FULL = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/mana_bar_full.png");
    private static final int MANA_BAR_HEIGHT = 47;
    private static final int MANA_BAR_WIDTH = 7;
    private final int imageWidth = 176;
    private final int imageHeight = 166;
    public SolarManaCollectorScreen(SolarManaCollectorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

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

        // 是否正在發電（同步值）
        boolean generating = menu.isGenerating();

        // 如果沒在發電 → 降低亮度（變灰）
        if (!generating) {
            guiGraphics.setColor(0.5f, 0.5f, 0.5f, 1.0f);
        }

        // 繪製太陽圖示
        guiGraphics.blit(TEXTURE, targetX, targetY, sunSrcX, sunSrcY, sunW, sunH);

        // 恢復顏色避免影響其他元素
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        // 魔力條繪製
        drawManaBar(guiGraphics, 11, 19); // 偏移位置可自行調整
    }



    private boolean isHoveringManaBar(int mouseX, int mouseY) {
        int manaBarX = this.leftPos + 11;
        int manaBarY = this.topPos + 19;
        return mouseX >= manaBarX && mouseX <= manaBarX + MANA_BAR_WIDTH && mouseY >= manaBarY && mouseY <= manaBarY + MANA_BAR_HEIGHT;
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
                Component.translatable("screen.magical_industry.upgrade_button.tooltip")
        );

        // 📌 升級按鈕元件
        this.addRenderableWidget(new GenericButtonWithTooltip(
                this.leftPos + 150, this.topPos + 5, // 位置
                18, 18, // 尺寸
                Component.empty(),
                ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/upgrade_button.png"),
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
        this.renderBackground(guiGraphics,mouseX ,mouseY ,partialTick);              // 灰暗背景
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY); // 你定義的 GUI 背景（紋理）

        super.render(guiGraphics, mouseX, mouseY, partialTick);  // 基礎 Slot + 按鈕處理

        this.renderTooltip(guiGraphics, mouseX, mouseY); // 工具提示（例如滑鼠移到物品上）
        if (isHoveringManaBar(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("tooltip.mana", this.menu.getManaStored(), this.menu.getMaxMana()), mouseX, mouseY);
        }
    }



    @Override
    public @Nullable Slot getSlotUnderMouse() {
        return super.getSlotUnderMouse();
    }
}
