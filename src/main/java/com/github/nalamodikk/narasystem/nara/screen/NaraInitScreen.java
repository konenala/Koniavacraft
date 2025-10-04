package com.github.nalamodikk.narasystem.nara.screen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.button.TooltipButton;
import com.github.nalamodikk.narasystem.nara.network.server.NaraBindRequestPacket;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public class NaraInitScreen extends Screen {

    private enum Stage {
        SHOWING_LINES, AWAITING_CONFIRM
    }

    private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_overlay.png");
    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/nara_button.png");
    private static final List<Component> BIND_TOOLTIP = List.of(Component.translatable("tooltip.koniava.nara.bind"));
    private static final List<Component> CANCEL_TOOLTIP = List.of(Component.translatable("tooltip.koniava.nara.cancel"));

    private static final int BG_WIDTH = 256;
    private static final int BG_HEIGHT = 190;
    private static final int GRADIENT_PADDING = 12;
    private static final int LINE_HEIGHT = 12;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_VERSION = resolveModVersion();
    private static final Component TITLE = Component.translatable("screen.koniava.nara.title");
    private static final Component[] TEXT_LINES = buildLines();

    private static ResourceLocation gradientTexture;
    private static DynamicTexture gradientDynamic;

    private Stage currentStage = Stage.SHOWING_LINES;
    private int visibleLines = 0;
    private int ticksElapsed = 0;

    private int cachedCenterX = 0;
    private int cachedCenterY = 0;
    private int cachedBgX = 0;
    private int cachedBgY = 0;
    private boolean layoutCached = false;

    private final FormattedCharSequence[] cachedLineSequences = new FormattedCharSequence[TEXT_LINES.length];
    private final int[] lineWidths = new int[TEXT_LINES.length];
    private final int[] cachedLineStartX = new int[TEXT_LINES.length];
    private boolean textCacheDirty = true;

    private TooltipButton bindButton;
    private TooltipButton cancelButton;

    public NaraInitScreen() {
        super(Component.empty());
    }

    /**
     * 初始化貼圖快取與佈局資訊，並確保按鈕預設為隱藏狀態。
     */
    @Override
    protected void init() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().getTexture(OVERLAY_TEXTURE).setFilter(false, false);
        ensureGradientTexture(minecraft);
        layoutCached = false;
        textCacheDirty = true;
        hideButtons();
    }

    /**
     * 控制文字逐行顯示節奏，並在完成後啟用互動按鈕。
     */
    @Override
    public void tick() {
        super.tick();
        ticksElapsed++;

        if (currentStage == Stage.SHOWING_LINES) {
            if (visibleLines < TEXT_LINES.length && ticksElapsed % 10 == 0) {
                visibleLines++;
                textCacheDirty = true;
            }
            if (visibleLines == TEXT_LINES.length && ticksElapsed >= TEXT_LINES.length * 10 + 20) {
                currentStage = Stage.AWAITING_CONFIRM;
                showButtons();
            }
        }
    }

    /**
     * 建立或更新背景緩存貼圖，避免每幀重新填漸層。
     */
    private static void ensureGradientTexture(Minecraft minecraft) {
        if (gradientTexture != null) {
            return;
        }

        NativeImage gradientImage = new NativeImage(BG_WIDTH + GRADIENT_PADDING * 2, BG_HEIGHT + GRADIENT_PADDING * 2, true);
        int height = gradientImage.getHeight();
        for (int y = 0; y < height; y++) {
            float t = height <= 1 ? 0F : (float) y / (float) (height - 1);
            int alpha = Mth.floor(Mth.lerp(t, 0xA0, 0x70));
            int color = (alpha << 24);
            for (int x = 0; x < gradientImage.getWidth(); x++) {
                gradientImage.setPixelRGBA(x, y, color);
            }
        }

        gradientDynamic = new DynamicTexture(gradientImage);
        gradientDynamic.setFilter(false, false);
        ResourceLocation gradientLocation = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "dynamic/nara_init/gradient");
        minecraft.getTextureManager().register(gradientLocation, gradientDynamic);
        gradientDynamic.upload();
        gradientTexture = gradientLocation;
    }

    /**
     * 解析模組版本資訊，提供畫面顯示用字串。
     */
    private static String resolveModVersion() {
        try {
            return ModList.get().getModContainerById(KoniavacraftMod.MOD_ID)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse("dev");
        } catch (Exception exception) {
            LOGGER.warn("無法取得模組版本，改用 dev 標記", exception);
            return "dev";
        }
    }

    /**
     * 組裝靜態敘述行陣列，避免重複產生 Component。
     */
    private static Component[] buildLines() {
        return new Component[] {
                Component.translatable("screen.koniava.nara.line1"),
                Component.translatable("screen.koniava.nara.line2"),
                Component.translatable("screen.koniava.nara.line3"),
                Component.translatable("screen.koniava.nara.line4"),
                Component.translatable("screen.koniava.nara.line5", MOD_VERSION),
                Component.translatable("screen.koniava.nara.line6")
        };
    }

    /**
     * 更新背景與中央座標，並標記文字快取為失效。
     */
    private void updateLayoutCache() {
        cachedCenterX = this.width / 2;
        cachedCenterY = this.height / 2;
        cachedBgX = (this.width - BG_WIDTH) / 2;
        cachedBgY = (this.height - BG_HEIGHT) / 2;
        layoutCached = true;
        textCacheDirty = true;
        positionButtons();
    }

    /**
     * 在字型可用時重建顯示序列與對齊資訊。
     */
    private void refreshTextCache() {
        if (!textCacheDirty || this.font == null) {
            return;
        }
        for (int index = 0; index < TEXT_LINES.length; index++) {
            cachedLineSequences[index] = TEXT_LINES[index].getVisualOrderText();
            lineWidths[index] = this.font.width(cachedLineSequences[index]);
            cachedLineStartX[index] = cachedCenterX - lineWidths[index] / 2;
        }
        textCacheDirty = false;
    }

    /**
     * 確保按鈕只建立一次並加入渲染隊列。
     */
    private void ensureButtons() {
        if (bindButton == null) {
            bindButton = new TooltipButton(
                    0, 0, 90, 20,
                    Component.translatable("screen.koniava.nara.bind"),
                    BUTTON_TEXTURE, 90, 20,
                    button -> {
                        NaraBindRequestPacket.send(true);
                        onClose();
                    },
                    () -> BIND_TOOLTIP
            );
        }
        if (cancelButton == null) {
            cancelButton = new TooltipButton(
                    0, 0, 90, 20,
                    Component.translatable("screen.koniava.nara.cancel"),
                    BUTTON_TEXTURE, 90, 20,
                    button -> {
                        NaraBindRequestPacket.send(false);
                        onClose();
                        var connection = Minecraft.getInstance().getConnection();
                        if (connection != null) {
                            connection.disconnect(Component.translatable("message.koniava.nara.disconnect_message"));
                        }
                    },
                    () -> CANCEL_TOOLTIP
            );
        }
        if (!this.children().contains(bindButton)) {
            addRenderableWidget(bindButton);
        }
        if (!this.children().contains(cancelButton)) {
            addRenderableWidget(cancelButton);
        }
        positionButtons();
    }

    /**
     * 依最新佈局更新按鈕位置與顯示狀態。
     */
    private void positionButtons() {
        if (bindButton == null || cancelButton == null || !layoutCached) {
            return;
        }
        int buttonY = cachedCenterY + 60;
        bindButton.setPosition(cachedCenterX - 100, buttonY);
        cancelButton.setPosition(cachedCenterX + 10, buttonY);
    }

    /**
     * 顯示並啟用互動按鈕。
     */
    private void showButtons() {
        ensureButtons();
        bindButton.visible = true;
        bindButton.active = true;
        cancelButton.visible = true;
        cancelButton.active = true;
    }

    /**
     * 隱藏並停用按鈕，避免未到確認階段即可操作。
     */
    private void hideButtons() {
        if (bindButton != null) {
            bindButton.visible = false;
            bindButton.active = false;
        }
        if (cancelButton != null) {
            cancelButton.visible = false;
            cancelButton.active = false;
        }
    }

    /**
     * 調整畫面時清除快取並更新按鈕位置。
     */
    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        layoutCached = false;
        textCacheDirty = true;
        if (currentStage == Stage.AWAITING_CONFIRM) {
            showButtons();
        } else {
            hideButtons();
        }
    }

    /**
     * 渲染背景與文字內容，並交給父類別處理按鈕等子元件。
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!layoutCached) {
            updateLayoutCache();
        }
        refreshTextCache();

        graphics.fillGradient(0, 0, this.width, this.height, 0xD0000000, 0x90000000);
        if (gradientTexture != null) {
            graphics.blit(
                    gradientTexture,
                    cachedBgX - GRADIENT_PADDING,
                    cachedBgY - GRADIENT_PADDING,
                    0,
                    0,
                    BG_WIDTH + GRADIENT_PADDING * 2,
                    BG_HEIGHT + GRADIENT_PADDING * 2,
                    BG_WIDTH + GRADIENT_PADDING * 2,
                    BG_HEIGHT + GRADIENT_PADDING * 2
            );
        }
        graphics.blit(OVERLAY_TEXTURE, cachedBgX, cachedBgY, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        int startY = cachedCenterY - 50;
        graphics.drawCenteredString(this.font, TITLE, cachedCenterX, startY, 0xFFFFFF);

        if (visibleLines > 0) {
            int linesToDraw = Math.min(visibleLines, cachedLineSequences.length);
            for (int index = 0; index < linesToDraw; index++) {
                graphics.drawString(this.font, cachedLineSequences[index], cachedLineStartX[index], startY + 20 + index * LINE_HEIGHT, 0xAAAAAA);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * 背景繪製統一在 render 中處理，避免重複呼叫。
     */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 背景已於 render 內完成。
    }

    /**
     * 禁止 ESC 關閉畫面，避免玩家跳過綁定程序。
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /**
     * 關閉畫面時恢復為無 GUI 狀態。
     */
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
