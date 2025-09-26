package com.github.nalamodikk.narasystem.nara.screen;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class NaraIntroScreen extends Screen {
    private static final ResourceLocation CIRCLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_circle.png");
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final int INTRO_DURATION = 80; // 播 4 秒（20 tick = 1 秒）
    private static final int TEX_SIZE = 128;
    private static final float ROTATION_SPEED = 2F;

    private int ticksElapsed = 0;
    private float currentAngle = 0F;

    // 優化：快取螢幕中心位置
    private float cachedCenterX = 0F;
    private float cachedCenterY = 0F;
    private boolean centerCached = false;

    public NaraIntroScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        Minecraft.getInstance().getTextureManager().getTexture(CIRCLE_TEXTURE).setFilter(false, false);
        centerCached = false;
    }

    @Override
    public void tick() {
        ticksElapsed++;
        currentAngle = (currentAngle + ROTATION_SPEED) % 360F;

        if (ticksElapsed >= INTRO_DURATION) {
            Minecraft.getInstance().setScreen(new NaraInitScreen());
        }
    }

    private void cacheCenter() {
        if (!centerCached) {
            cachedCenterX = this.width / 2F;
            cachedCenterY = this.height / 2F;
            centerCached = true;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        cacheCenter();

        graphics.fillGradient(0, 0, this.width, this.height, 0xD0000000, 0x90000000);

        float interpolatedAngle = currentAngle + (ROTATION_SPEED * partialTick);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cachedCenterX, cachedCenterY, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(interpolatedAngle));
        pose.translate(-TEX_SIZE / 2F, -TEX_SIZE / 2F, 0);
        graphics.blit(CIRCLE_TEXTURE, 0, 0, 0, 0, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
        pose.popPose();

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 背景已於 render 中透過 fillGradient 處理
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        centerCached = false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
