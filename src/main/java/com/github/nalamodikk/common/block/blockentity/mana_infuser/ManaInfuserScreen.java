package com.github.nalamodikk.common.block.blockentity.mana_infuser;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import com.github.nalamodikk.client.screenAPI.framework.ModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Collections;
import java.util.List;

/**
 * 🔮 魔力注入機 GUI 界面 (模組化版本)
 */
public class ManaInfuserScreen extends ModularScreen<ManaInfuserMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_infuser_gui.png");

    public ManaInfuserScreen(ManaInfuserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void buildGui(Panel root) {
        // 1. 魔力條 (9, 17) - 10x48
        root.add(new ManaBarWidget(9, 17, menu::getCurrentMana, menu::getMaxMana)
                .setSize(10, 48)); // 根據原版代碼調整大小

        // 2. 進度條 (72, 40) - 34x11 (往下延伸兩格像素)
        // 使用匿名 Widget 直接繪製大圖上的進度條
        root.add(new AbstractWidget(72, 40, 34, 11) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int localX, int localY, int screenX, int screenY) {
                if (menu.isWorking()) {
                    int progress = menu.getProgressPercentage();
                    // 計算像素寬度
                    int fillWidth = (int) ((float) progress / 100 * width);
                    
                    if (fillWidth > 0) {
                        graphics.blit(TEXTURE, 
                            0, 0,           // 螢幕相對座標
                            176, 54,        // UV 起點 (從大圖右側截取)
                            fillWidth, height // 繪製大小
                        );
                    }
                }
            }

            @Override
            public List<Component> getTooltip() {
                if (menu.isWorking()) {
                    return List.of(Component.translatable("gui.koniava.mana_infuser.progress", menu.getProgressPercentage()));
                } else {
                    return List.of(Component.translatable("gui.koniava.mana_infuser.status.idle"));
                }
            }
        });
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 繪製背景圖
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        
        // 繪製 Widget
        super.renderBg(graphics, partialTick, mouseX, mouseY);
    }
}
