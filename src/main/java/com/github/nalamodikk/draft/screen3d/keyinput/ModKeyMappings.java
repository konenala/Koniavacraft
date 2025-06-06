package com.github.nalamodikk.draft.screen3d.keyinput;


import com.github.nalamodikk.MagicalIndustryMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)

public class ModKeyMappings {
    public static final KeyMapping OPEN_FLOATING_GUI = new KeyMapping(
            "key.magical_industry.open_floating_gui", // 對應 lang 的翻譯鍵
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.magical_industry" // 顯示在控制選單分組
                );
        @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_FLOATING_GUI);
    }


}

