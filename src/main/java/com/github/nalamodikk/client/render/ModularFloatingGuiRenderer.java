package com.github.nalamodikk.client.render;


import com.github.nalamodikk.MagicalIndustryMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class ModularFloatingGuiRenderer {

    // TODO: Replace with your actual texture
    private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/modular_panel.png");

    private static boolean visible = false;

    public static void toggleVisibility() {
        visible = !visible;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (!visible || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        poseStack.pushPose();

        // 設定面板浮在玩家眼前 1.5 格
        Vec3 camPos = event.getCamera().getPosition();
        Vec3 lookVec = player.getLookAngle();

        double x = camPos.x + lookVec.x * 1.5;
        double y = camPos.y + lookVec.y * 1.5;
        double z = camPos.z + lookVec.z * 1.5;

        poseStack.translate(x - camPos.x, y - camPos.y, z - camPos.z);

        // 面板大小與朝向
        float scale = 0.8f;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

        // 繪製面板紋理（暫為靜態）
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(PANEL_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        float halfW = 1.0f;
        float halfH = 0.6f;

        // 座標：從中心向四角擴展，貼圖範圍 0~1


        vc.addVertex(pose, -1f,  0.6f, 0f).setColor(255,255,255,255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose,  1f,  0.6f, 0f).setColor(255,255,255,255).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose,  1f, -0.6f, 0f).setColor(255,255,255,255).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, -1f, -0.6f, 0f).setColor(255,255,255,255).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0, 0, -1);

        poseStack.popPose();
        buffer.endBatch();
    }
}
