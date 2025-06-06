package com.github.nalamodikk.draft.screen3d;


import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.draft.screen3d.api.FloatingPanelManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class FloatingGuiRenderer {

    private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MagicalIndustryMod.MOD_ID, "textures/gui/modular_panel.png"
    );

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        FloatingPanelManager.renderAll(event); // ✅ 只要這行，畫全部
    }


    private static void putQuad(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float w, float h) {
        int color = 0xFFFFFFFF;
        int light = 0xF000F0;
        int overlay = 0;

        consumer.addVertex(pose, x,     y,     0).setColor(color).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, x + w, y,     0).setColor(color).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, x + w, y + h, 0).setColor(color).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, x,     y + h, 0).setColor(color).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
    }
    public static void toggleVisibility() {
        FloatingPanelManager.toggleExamplePanel();
    }
}
