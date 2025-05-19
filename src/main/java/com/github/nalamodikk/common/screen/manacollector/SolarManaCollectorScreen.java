package com.github.nalamodikk.common.screen.manacollector;

import com.github.nalamodikk.client.screenAPI.UniversalTexturedButton;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.ToggleModePacket;
import com.github.nalamodikk.common.register.handler.RegisterNetworkHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SolarManaCollectorScreen extends AbstractContainerScreen<SolarManaCollectorMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/solar_mana_collector_gui.png");
    private final int imageWidth = 176;
    private final int imageHeight = 166;
    public SolarManaCollectorScreen(SolarManaCollectorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 主背景（blit: 切割圖片指定區域貼圖）
        guiGraphics.blit(TEXTURE, x, y,
                0, 0, // 紋理內起始座標
                imageWidth, imageHeight, // 要切出來的區塊大小
                256, 256); // 整張圖片的尺寸
    }



}
