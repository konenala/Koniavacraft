package com.github.nalamodikk.common.screen.player;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ExtraEquipmentScreen extends AbstractContainerScreen<ExtraEquipmentMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/extra_equipment.png");

        public ExtraEquipmentScreen(ExtraEquipmentMenu menu, Inventory playerInventory, Component title) {
            super(menu, playerInventory, title);
            this.imageWidth = 176;
            this.imageHeight = 166;
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }

        @Override
        protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
            graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
            graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(graphics, mouseX, mouseY, partialTick);
            super.render(graphics, mouseX, mouseY, partialTick);
            this.renderTooltip(graphics, mouseX, mouseY);
        }
    }


