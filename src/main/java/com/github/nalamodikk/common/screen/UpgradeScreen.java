package com.github.nalamodikk.common.screen;

import com.github.nalamodikk.client.screenAPI.DynamicTooltip;
import com.github.nalamodikk.client.screenAPI.GenericButtonWithTooltip;
import com.github.nalamodikk.client.screenAPI.TooltipSupplier;
import com.github.nalamodikk.client.screenAPI.UniversalTexturedButton;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.OpenUpgradeGuiPacket;
import com.github.nalamodikk.common.register.handler.RegisterNetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class UpgradeScreen extends AbstractContainerScreen<UpgradeMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/upgrade_gui.png");
    private UniversalTexturedButton upgradeButton;

    public UpgradeScreen(UpgradeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }





    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
    }
}
