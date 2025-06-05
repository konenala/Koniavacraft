package com.github.nalamodikk.client.keyinput;


import com.github.nalamodikk.client.render.ModularFloatingGuiRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class ModKeyMappings {
    public static final KeyMapping OPEN_FLOATING_GUI = new KeyMapping(
            "key.magical_industry.open_floating_gui", // 對應 lang 的翻譯鍵
            GLFW.GLFW_KEY_G,
            "key.categories.magical_industry" // 顯示在控制選單分組
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_FLOATING_GUI);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_FLOATING_GUI.consumeClick()) {
            ModularFloatingGuiRenderer.toggleVisibility();
        }
    }
}
