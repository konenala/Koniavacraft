package com.github.nalamodikk.screen.ManaCrafting;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.block.entity.mana_crafting.AdvancedManaCraftingTableBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AdvancedManaCraftingTableScreen extends AbstractContainerScreen<AdvancedManaCraftingTableMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/advanced_mana_crafting_table_gui.png");
    private static final ResourceLocation MANA_BAR_FULL = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_bar_full.png");
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/custom_button.png");

    public AdvancedManaCraftingTableScreen(AdvancedManaCraftingTableMenu container, Inventory inv, Component title) {
        super(container, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // 添加自定义纹理按钮
        this.addRenderableWidget(new ImageButton(this.leftPos + 140, this.topPos + 20, 20, 20, 0, 0, 20, BUTTON_TEXTURE, 64, 64, button -> onCustomButtonPressed()));
    }

    private void onCustomButtonPressed() {
        // 自定义按钮按下时的逻辑
        this.menu.toggleAutoCrafting();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // 渲染背景纹理
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        renderManaBar(guiGraphics, partialTicks);
    }

    private void renderManaBar(GuiGraphics guiGraphics, float partialTicks) {
        int manaBarX = this.leftPos + 11;
        int manaBarY = this.topPos + 19;
        int manaBarWidth = 7;
        int manaBarHeight = 47;

        int manaStored = this.menu.getManaStored();
        int maxMana = AdvancedManaCraftingTableBlockEntity.MAX_MANA;
        float manaPercentage = (float) manaStored / maxMana;
        int manaHeight = Math.round(manaPercentage * manaBarHeight);

        if (manaHeight > 0) {
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(MANA_BAR_FULL, manaBarX, manaBarY + (manaBarHeight - manaHeight), 0, 0, manaBarWidth, manaHeight);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderManaTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderManaTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int manaBarX = this.leftPos + 11;
        int manaBarY = this.topPos + 19;
        int manaBarWidth = 7;
        int manaBarHeight = 47;

        if (mouseX >= manaBarX && mouseX <= manaBarX + manaBarWidth && mouseY >= manaBarY && mouseY <= manaBarY + manaBarHeight) {
            int manaStored = this.menu.getManaStored();
            int maxMana = AdvancedManaCraftingTableBlockEntity.MAX_MANA;

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("tooltip.magical_industry.mana_stored", manaStored, maxMana));

            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title.getString(), this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle.getString(), 8, this.imageHeight - 94, 4210752, false);
    }
}
