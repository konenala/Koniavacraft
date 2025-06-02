package com.github.nalamodikk.system.nara.screen;

import com.github.nalamodikk.MagicalIndustryMod;
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
    private static final ResourceLocation CIRCLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/nara_circle.png");
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final int INTRO_DURATION = 80; // 播 4 秒（20 tick = 1 秒）
    private int ticksElapsed = 0;

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
        if (ticksElapsed >= INTRO_DURATION) {
            Minecraft.getInstance().setScreen(new NaraInitScreen());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 背景全黑
        graphics.setColor(1F, 1F, 1F, 1F); // 確保 alpha 正常（非必要，但推薦）

        graphics.fill(0, 0, this.width, this.height, 0xFF000000); // 自己畫純黑遮罩

        float centerX = this.width / 2F;
        float centerY = this.height / 2F;
        int texSize = 128;

        float angle = (ticksElapsed + partialTick) * 2F;

        PoseStack pose = graphics.pose();
        pose.pushPose();

        pose.translate(centerX, centerY, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-texSize / 2F, -texSize / 2F, 0);

        graphics.blit(CIRCLE_TEXTURE, 0, 0, 0, 0, texSize, texSize, texSize, texSize);

        pose.popPose();

        super.render(graphics, mouseX, mouseY, partialTick);
    }


    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // ❌ 不要呼叫 super，不要呼叫 renderBlurredBackground
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
