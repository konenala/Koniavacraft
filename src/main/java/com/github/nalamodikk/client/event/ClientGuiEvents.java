package com.github.nalamodikk.client.event;


import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ClientGuiEvents {

    @SubscribeEvent
    public static void onInventoryInit(ScreenEvent.Init.Post event) {
        // 確保當前畫面是原版背包（InventoryScreen）
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        int x = screen.getGuiLeft(); // GUI 左上角 X
        int y = screen.getGuiTop();  // GUI 左上角 Y

        // 建立一個本地化按鈕，點下去後會開啟自訂介面
        Button button = Button.builder(
                        Component.translatable("screen.inventory.koniava.extra_equipment"), // 本地化鍵
                        btn -> Minecraft.getInstance().setScreen(new ExtraEquipmentScreen()) // 點擊行為
                )
                .bounds(x + 150, y + 5, 40, 20) // 位置與大小
                .build();

        event.addListener(button); // 加到畫面上
    }


    public static void register() {
        NeoForge.EVENT_BUS.register(ClientGuiEvents.class);
    }
}