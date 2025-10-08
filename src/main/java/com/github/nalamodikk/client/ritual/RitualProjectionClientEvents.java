package com.github.nalamodikk.client.ritual;

import com.github.nalamodikk.KoniavacraftMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 客戶端事件：負責更新與繪製儀式投影。
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class RitualProjectionClientEvents {

    private RitualProjectionClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        RitualStructureProjectionManager.getInstance().tick();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            RitualStructureProjectionManager.getInstance().render(event);
        }
    }
}
