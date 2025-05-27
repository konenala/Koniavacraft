package com.github.nalamodikk.client.screenAPI.gui;
/**
 * 用來創建升級按鈕並綁定封包邏輯
 * 當時設計目的是讓某個機器 GUI 可以呼叫這個按鈕以開啟升級畫面
 * 尚未接入任何實際畫面
 */

import com.github.nalamodikk.client.screenAPI.GenericButtonWithTooltip;
import com.github.nalamodikk.client.screenAPI.TooltipSupplier;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.OpenUpgradeGuiPacket;
import com.github.nalamodikk.common.registry.handler.RegisterNetworkHandler;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import java.util.List;

public class UpgradeButtonFactory {

    public static AbstractWidget create(int guiLeft, int guiTop, BlockPos pos) {
        TooltipSupplier.Positioned tooltip = (mouseX, mouseY) -> List.of(
                Component.translatable("screen.magical_industry.upgrade_button.tooltip")
        );

        return new GenericButtonWithTooltip(
                guiLeft + 150, guiTop + 5,
                18, 18,
                Component.empty(),
                new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/upgrade_button.png"),
                18, 18,
                button -> RegisterNetworkHandler.NETWORK_CHANNEL.sendToServer(new OpenUpgradeGuiPacket(pos)),
                tooltip
        );
    }
}
