package com.github.nalamodikk.narasystem.nara.screen;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;

public class NaraIntroScreen extends Screen {

    private static final ResourceLocation CIRCLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_circle.png");
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int INTRO_DURATION = 80;
    private static final int TEX_SIZE = 128;
    private static final float ROTATION_SPEED = 2F;
    private static final int FRAME_COUNT = 180;
    private static final float FRAME_STEP_DEGREE = 360F / FRAME_COUNT;
    private static final String FRAME_CACHE_PATH_TEMPLATE = "dynamic/nara_intro/frame_%03d";

    private static ResourceLocation[] bakedFrames;
    private static DynamicTexture[] frameTextures;
    private static boolean frameCacheFailed = false;

    private int ticksElapsed = 0;
    private float accumulatedAngle = 0F;

    private float cachedCenterX = 0F;
    private float cachedCenterY = 0F;
    private boolean centerCached = false;

    public NaraIntroScreen() {
        super(Component.empty());
    }

    /**
     * 初始化貼圖快取以及畫面中心，確保後續渲染使用預先烘焙的序列貼圖。
     */
    @Override
    protected void init() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().getTexture(CIRCLE_TEXTURE).setFilter(false, false);
        ensureFrameCache(minecraft);
        centerCached = false;
    }

    /**
     * 更新動畫累積角度與播放進度，到期後切換至初始化畫面。
     */
    @Override
    public void tick() {
        ticksElapsed++;
        accumulatedAngle += ROTATION_SPEED;
        if (accumulatedAngle >= 360F) {
            accumulatedAngle -= 360F;
        }

        if (ticksElapsed >= INTRO_DURATION) {
            Minecraft.getInstance().setScreen(new NaraInitScreen());
        }
    }

    /**
     * 重新計算螢幕中心，避免視窗尺寸變更時造成座標飄移。
     */
    private void cacheCenter() {
        if (!centerCached) {
            cachedCenterX = this.width / 2F;
            cachedCenterY = this.height / 2F;
            centerCached = true;
        }
    }

    /**
     * 主要渲染流程：先填上全黑遮罩，再使用烘焙序列或備援矩陣繪製旋轉圈。
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        cacheCenter();
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);

        if (!frameCacheFailed && bakedFrames != null) {
            renderBaked(graphics, partialTick);
        } else {
            renderFallback(graphics, partialTick);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * 使用預先烘焙的序列貼圖，以插值方式維持原動畫的平順度。
     */
    private void renderBaked(GuiGraphics graphics, float partialTick) {
        float interpolatedAngle = accumulatedAngle + (ROTATION_SPEED * partialTick);
        if (interpolatedAngle >= 360F) {
            interpolatedAngle -= 360F;
        }

        float framePosition = interpolatedAngle / FRAME_STEP_DEGREE;
        int baseFrame = Mth.floor(framePosition) % FRAME_COUNT;
        float blend = framePosition - baseFrame;
        int nextFrame = (baseFrame + 1) % FRAME_COUNT;

        int drawX = Mth.floor(cachedCenterX - TEX_SIZE / 2F);
        int drawY = Mth.floor(cachedCenterY - TEX_SIZE / 2F);

        graphics.setColor(1F, 1F, 1F, 1F);
        graphics.blit(bakedFrames[baseFrame], drawX, drawY, 0, 0, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);

        if (blend > 1.0E-3F) {
            graphics.setColor(1F, 1F, 1F, blend);
            graphics.blit(bakedFrames[nextFrame], drawX, drawY, 0, 0, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
            graphics.setColor(1F, 1F, 1F, 1F);
        }
    }

    /**
     * 在序列貼圖建置失敗時的備援邏輯，保留舊有矩陣旋轉行為。
     */
    private void renderFallback(GuiGraphics graphics, float partialTick) {
        float interpolatedAngle = accumulatedAngle + (ROTATION_SPEED * partialTick);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cachedCenterX, cachedCenterY, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(interpolatedAngle));
        pose.translate(-TEX_SIZE / 2F, -TEX_SIZE / 2F, 0);
        graphics.blit(CIRCLE_TEXTURE, 0, 0, 0, 0, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
        pose.popPose();
    }

    /**
     * 確保序列貼圖存在，若尚未建立則立刻透過 CPU 烘焙所有幀資料。
     */
    private static void ensureFrameCache(Minecraft minecraft) {
        if (frameCacheFailed || bakedFrames != null) {
            return;
        }

        Optional<Resource> resourceOptional = minecraft.getResourceManager().getResource(CIRCLE_TEXTURE);
        if (resourceOptional.isEmpty()) {
            frameCacheFailed = true;
            LOGGER.error("無法載入 NaraIntro 圖像資源：{}", CIRCLE_TEXTURE);
            return;
        }

        Resource resource = resourceOptional.get();
        try (InputStream inputStream = resource.open(); NativeImage source = NativeImage.read(inputStream)) {
            bakeFrames(minecraft, source);
        } catch (IOException exception) {
            frameCacheFailed = true;
            LOGGER.error("建立 NaraIntro 序列貼圖失敗", exception);
        }
    }

    /**
     * 將原始貼圖烘焙為 180 幀序列，並註冊為動態貼圖供 GUI 使用。
     */
    private static void bakeFrames(Minecraft minecraft, NativeImage source) {
        bakedFrames = new ResourceLocation[FRAME_COUNT];
        frameTextures = new DynamicTexture[FRAME_COUNT];

        for (int index = 0; index < FRAME_COUNT; index++) {
            float radians = (float) Math.toRadians(index * FRAME_STEP_DEGREE);
            NativeImage rotated = new NativeImage(TEX_SIZE, TEX_SIZE, true);
            bakeInto(source, rotated, radians);

            DynamicTexture dynamicTexture = new DynamicTexture(rotated);
            dynamicTexture.setFilter(false, false);

            ResourceLocation frameLocation = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, String.format(Locale.ROOT, FRAME_CACHE_PATH_TEMPLATE, index));
            minecraft.getTextureManager().register(frameLocation, dynamicTexture);
            dynamicTexture.upload();

            bakedFrames[index] = frameLocation;
            frameTextures[index] = dynamicTexture;
        }
    }

    /**
     * 依指定弧度將原圖像素映射至目標貼圖，使用最近鄰取樣維持透明度。
     */
    private static void bakeInto(NativeImage source, NativeImage target, float radians) {
        float sin = Mth.sin(radians);
        float cos = Mth.cos(radians);
        float center = TEX_SIZE / 2F;

        for (int y = 0; y < TEX_SIZE; y++) {
            for (int x = 0; x < TEX_SIZE; x++) {
                float dx = x - center;
                float dy = y - center;

                float sampleX = cos * dx + sin * dy + center;
                float sampleY = -sin * dx + cos * dy + center;

                int sourceX = Mth.floor(sampleX + 0.5F);
                int sourceY = Mth.floor(sampleY + 0.5F);

                int color = 0;
                if (sourceX >= 0 && sourceX < TEX_SIZE && sourceY >= 0 && sourceY < TEX_SIZE) {
                    color = source.getPixelRGBA(sourceX, sourceY);
                }

                target.setPixelRGBA(x, y, color);
            }
        }
    }

    /**
     * 背景已於主渲染邏輯處理，避免重複繪製。
     */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 已由 render 方法完成背景填充。
    }

    /**
     * 視窗尺寸改變時重置中心快取，確保後續位置重新計算。
     */
    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        centerCached = false;
    }

    /**
     * 禁止 ESC 快速關閉，確保玩家看完引導動畫。
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
