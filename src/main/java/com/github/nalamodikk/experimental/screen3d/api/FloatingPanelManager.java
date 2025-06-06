package com.github.nalamodikk.experimental.screen3d.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FloatingPanelManager {
    private static final List<FloatingScreen> screens = new ArrayList<>();
    private static final Map<String, FloatingScreen> panels = new HashMap<>();

    public static void show(FloatingScreen screen) {
        if (!screens.contains(screen)) {
            screens.add(screen);
        }
    }



    public static void toggleExamplePanel() {
        FloatingScreen panel = panels.get("ExampleFloatingPanel");
        if (panel != null) {
            panel.setVisible(!panel.isVisible());

        }
    }

    public static void register(String id, FloatingScreen panel) {
        panels.put(id, panel);
    }
    public static void cleanupInvisible() {
        screens.removeIf(screen -> !screen.isVisible());
    }

    public static void tickAll() {
        for (FloatingScreen screen : screens) {
            if (screen.isVisible()) {
                screen.tick();
            }
        }
    }


    public static void renderAll(RenderLevelStageEvent event) {
        PoseStack stack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        Camera camera = event.getCamera();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true); // ✅ 正確取得 partialTick

        for (FloatingScreen screen : screens) {
            if (screen.isVisible()) {

                screen.render(stack, buffer, camera, partialTick); // ✅ 傳入正確的 float
            }
        }

        buffer.endBatch(); // ✅ 注意：buffer 必須是 BufferSource 類別才有 endBatch()
    }

}
