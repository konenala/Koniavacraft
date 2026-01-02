package com.github.nalamodikk.common.block.blockentity.ore_grinder;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ArrowProgressWidget;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.ModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * ⚙️ 粉碎機 GUI 界面 (模組化版本)
 */
public class OreGrinderScreen extends ModularScreen<OreGrinderMenu> {

    // GUI 材質位置 (大圖)
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/ore_grinder_gui.png");

    public OreGrinderScreen(OreGrinderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void buildGui(Panel root) {
        // 1. 進度條 (箭頭)
        // 位置 (79, 35)，尺寸 24x17 (基於之前 ArrowProgressWidget 的預設)
        root.add(new ArrowProgressWidget(79, 35, 
            menu::getProgress, 
            menu::getMaxProgress
        ));

        // 2. 魔力條
        // 位置 (9, 17)，尺寸由 ManaBarWidget 決定 (預設 14x50)
        // 如果您的背景槽位尺寸不同，可以在建構子中調整
        root.add(new ManaBarWidget(9, 17, 
            menu::getCurrentMana, 
            menu::getMaxMana
        ));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 繪製大背景圖 (包含 Slot 框框)
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        
        // 繪製 Widget
        super.renderBg(graphics, partialTick, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // 這裡會自動繪製 rootPanel (箭頭與魔力條) 和 Tooltip
    }
}