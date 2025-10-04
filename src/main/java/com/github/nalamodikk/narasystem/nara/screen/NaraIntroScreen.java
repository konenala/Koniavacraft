package com.github.nalamodikk.narasystem.nara.screen;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class NaraIntroScreen extends Screen {
    private static final ResourceLocation CIRCLE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_circle.png");

    private static final int INTRO_DURATION = 80;
    private static final int TEX_SIZE = 128;
    private static final float ROTATION_SPEED = 2F;

    private int ticksElapsed = 0;
    private float angle = 0F;

    public NaraIntroScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        Minecraft.getInstance().getTextureManager()
            .getTexture(CIRCLE_TEXTURE).setFilter(false, false);
    }

    @Override
    public void tick() {
        ticksElapsed++;
        angle += ROTATION_SPEED;
        if (angle >= 360F) angle -= 360F;

        if (ticksElapsed >= INTRO_DURATION) {
            Minecraft.getInstance().setScreen(new NaraInitScreen());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);

        float interpolatedAngle = angle + (ROTATION_SPEED * partialTick);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(this.width / 2F, this.height / 2F, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(interpolatedAngle));
        pose.translate(-TEX_SIZE / 2F, -TEX_SIZE / 2F, 0);
        graphics.blit(CIRCLE_TEXTURE, 0, 0, 0, 0, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
        pose.popPose();

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return true; }
}
