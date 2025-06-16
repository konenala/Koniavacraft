package com.github.nalamodikk.experimental.screen3d.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.experimental.screen3d.api.FloatingPanelManager;
import com.github.nalamodikk.experimental.screen3d.api.FloatingScreen;
import com.github.nalamodikk.experimental.screen3d.keyinput.ModKeyMappings;
import com.github.nalamodikk.experimental.screen3d.screen.ExampleFloatingScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * 負責登錄鍵位（MOD BUS）與遊戲中執行邏輯（EVENT BUS）
 */

public class ClientInitcore {

    private static boolean initialized = false;
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * ✅ 鍵位註冊用：只能在 MOD BUS 上執行，不能與下面的 runtime 混在一起
     */
    @EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            NeoForge.EVENT_BUS.register(ClientInitcore.RuntimeEvents.class); // ✅ 一次就好

            LOGGER.info("✅ Registered RuntimeEvents");
        }



    }

    /**
     * ✅ Client 遊戲階段事件：必須單獨放進 NeoForge.EVENT_BUS
     */
    public static class RuntimeEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();

            if (!initialized && mc.level != null && mc.player != null) {
                BlockPos playerPos = mc.player.blockPosition().above(2);
                FloatingScreen screen = new ExampleFloatingScreen(playerPos, 5, 3);
                FloatingPanelManager.register("ExampleFloatingPanel", screen);
                FloatingPanelManager.show(screen);
                initialized = true;
            }
            FloatingPanelManager.tickAll(); // ✅ ← 這是你漏掉的關鍵

            while (ModKeyMappings.OPEN_FLOATING_GUI.consumeClick()) {
                FloatingPanelManager.toggleExamplePanel();
            }
        }
    }
}
