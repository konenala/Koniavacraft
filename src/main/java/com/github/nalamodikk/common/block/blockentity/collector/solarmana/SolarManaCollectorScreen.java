package com.github.nalamodikk.common.block.blockentity.collector.solarmana;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ImageWidget;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import com.github.nalamodikk.client.screenAPI.framework.ButtonWidget;
import com.github.nalamodikk.client.screenAPI.framework.ModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import com.github.nalamodikk.common.network.packet.server.OpenUpgradeGuiPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class SolarManaCollectorScreen extends ModularScreen<SolarManaCollectorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/solar_mana_collector_gui.png");
    private static final ResourceLocation WIDGET_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/upgrade_button.png");

    public SolarManaCollectorScreen(SolarManaCollectorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void buildGui(Panel root) {
        // 1. 魔力條 (11, 19)
        root.add(new ManaBarWidget(11, 19, menu::getManaStored, menu::getMaxMana)
                .setSize(7, 47)
                .setDrawBackground(false)); // 關閉 Widget 背景

        // 2. 太陽圖示 (69, 24)
        root.add(new ImageWidget(69, 24, 39, 38, TEXTURE, 176, 2) {
            @Override
            public List<Component> getTooltip() {
                boolean isDaytime = menu.isDaytime();
                boolean isGenerating = menu.isGenerating();

                if (isGenerating) {
                    return List.of(Component.translatable("tooltip.koniava.solar.generating"));
                } else if (!isDaytime) {
                    return List.of(Component.translatable("tooltip.koniava.solar.nighttime"));
                } else {
                    return List.of(Component.translatable("tooltip.koniava.solar.blocked"));
                }
            }

            @Override
            protected void renderWidget(GuiGraphics graphics, int localX, int localY, int mouseX, int mouseY) {
                if (!menu.isGenerating()) {
                    this.setColor(0.5f, 0.5f, 0.5f, 1.0f);
                } else {
                    this.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                }
                super.renderWidget(graphics, localX, localY, mouseX, mouseY);
            }
        });

        // 3. 升級資訊文字 (22, 20)
        root.add(new AbstractWidget(22, 20, 100, 20) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int localX, int localY, int screenX, int screenY) {
                int speedLevel = menu.getSpeedLevel();
                int efficiencyLevel = menu.getEfficiencyLevel();

                int speedColor = speedLevel > 0 ? 0xFFFFFF : 0x666666;
                int effColor = efficiencyLevel > 0 ? 0xFFFFFF : 0x666666;

                Component speedLabel = Component.translatable("screen.koniava.upgrade.speed", speedLevel);
                Component efficiencyLabel = Component.translatable("screen.koniava.upgrade.efficiency", efficiencyLevel);

                float scale = 0.8f;
                graphics.pose().pushPose();
                graphics.pose().scale(scale, scale, 1.0f);
                
                graphics.drawString(font, speedLabel, 0, 0, speedColor, false);
                graphics.drawString(font, efficiencyLabel, 0, (int)(10 / scale), effColor, false);

                graphics.pose().popPose();
            }
        });
        
        // 4. 升級按鈕 (150, 5)
        root.add(new ButtonWidget(150, 5, 18, 18, WIDGET_TEXTURE, 18, 18, btn -> {
            BlockPos pos = this.menu.getBlockEntity().getBlockPos();
            OpenUpgradeGuiPacket.sendToServer(pos);
        }).setTooltip(() -> List.of(
            Component.translatable("screen.koniava.upgrade_button.tooltip")
        )));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        super.renderBg(graphics, partialTick, mouseX, mouseY);
    }
}
