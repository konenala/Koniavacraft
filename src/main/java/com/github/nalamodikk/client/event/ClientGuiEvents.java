package com.github.nalamodikk.client.event;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.button.TooltipButton;
import com.github.nalamodikk.common.network.packet.server.player.gui.OpenExtraEquipmentPacket;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ClientGuiEvents {

    @SubscribeEvent
    public static void onInventoryInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        int x = screen.getGuiLeft();
        int y = screen.getGuiTop();

        TooltipButton button = new TooltipButton(
                x + 150, y + 5, 20, 20,
                Component.empty(), // 不需要文字，完全圖示
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/extra_equipment_button.png"),
                20, 40,
                btn -> OpenExtraEquipmentPacket.sendToServer(),
                () -> List.of(Component.translatable("tooltip.koniava.open_extra_equipment"))
        );

        event.addListener(button);
    }



    public static void register() {
        NeoForge.EVENT_BUS.register(ClientGuiEvents.class);
    }
}