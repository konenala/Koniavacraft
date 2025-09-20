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
    private int ticksElapsed = 0;

    // 優化：預計算旋轉步長，避免每frame計算
    private static final float ROTATION_SPEED = 2F;
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


    }

    @Override
    public void tick() {
        ticksElapsed++;
        // 優化：只在tick時更新角度，而非每frame
        currentAngle += ROTATION_SPEED;
        if (currentAngle >= 360F) {
            currentAngle -= 360F;
        }

        if (ticksElapsed >= INTRO_DURATION) {
            Minecraft.getInstance().setScreen(new NaraInitScreen());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 優化：快取中心位置計算
        if (!centerCached || cachedCenterX != this.width / 2F || cachedCenterY != this.height / 2F) {
            cachedCenterX = this.width / 2F;
            cachedCenterY = this.height / 2F;
            centerCached = true;
        }

        // 優化：使用更高效的背景清除方式
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int texSize = 128;
        // 優化：平滑插值角度，減少計算
        float interpolatedAngle = currentAngle + (ROTATION_SPEED * partialTick);

        PoseStack pose = graphics.pose();
        pose.pushPose();

        // 優化：使用快取的中心位置
        pose.translate(cachedCenterX, cachedCenterY, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(interpolatedAngle));
        pose.translate(-texSize / 2F, -texSize / 2F, 0);

        graphics.blit(CIRCLE_TEXTURE, 0, 0, 0, 0, texSize, texSize, texSize, texSize);

        pose.popPose();

        super.render(graphics, mouseX, mouseY, partialTick);
    }


    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 優化：使用更高效的背景渲染
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
