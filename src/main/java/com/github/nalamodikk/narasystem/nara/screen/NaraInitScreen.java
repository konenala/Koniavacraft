package com.github.nalamodikk.narasystem.nara.screen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.button.TooltipButton;
import com.github.nalamodikk.narasystem.nara.network.server.NaraBindRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.ModList;

import java.util.List;

public class NaraInitScreen extends Screen {

    private enum Stage {
        SHOWING_LINES, AWAITING_CONFIRM
    }

    private Stage currentStage = Stage.SHOWING_LINES;
    private int visibleLines = 0;
    private int ticksElapsed = 0;

    private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_overlay.png");
    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/nara_button.png");
    private static final List<Component> BIND_TOOLTIP = List.of(Component.translatable("tooltip.koniava.nara.bind"));
    private static final List<Component> CANCEL_TOOLTIP = List.of(Component.translatable("tooltip.koniava.nara.cancel"));

    // 佈局快取
    private static final int BG_WIDTH = 256;
    private static final int BG_HEIGHT = 190;
    private int cachedCenterX = 0;
    private int cachedCenterY = 0;
    private int cachedBgX = 0;
    private int cachedBgY = 0;
    private boolean layoutCached = false;

    private final Component title = Component.translatable("screen.koniava.nara.title");
    private final Component[] lines = new Component[] {
            Component.translatable("screen.koniava.nara.line1"),
            Component.translatable("screen.koniava.nara.line2"),
            Component.translatable("screen.koniava.nara.line3"),
            Component.translatable("screen.koniava.nara.line4"),
            Component.translatable("screen.koniava.nara.line5",
                    ModList.get().getModFileById(KoniavacraftMod.MOD_ID)
                            .getMods().getFirst().getVersion().toString()
            ),
            Component.translatable("screen.koniava.nara.line6")
    };
    private final FormattedCharSequence[] cachedLineSequences = new FormattedCharSequence[lines.length];
    private final int[] lineWidths = new int[lines.length];
    private final int[] cachedLineStartX = new int[lines.length];
    private boolean textCacheDirty = true;

    public NaraInitScreen() {
        super(Component.empty());
    }

    @Override
    public void tick() {
        super.tick();
        ticksElapsed++;

        if (currentStage == Stage.SHOWING_LINES) {
            if (visibleLines < lines.length && ticksElapsed % 10 == 0) {
                visibleLines++;
            }
            if (visibleLines == lines.length && ticksElapsed >= lines.length * 10 + 20) {
                currentStage = Stage.AWAITING_CONFIRM;
                if (this.children().isEmpty()) {
                    initButtons();
                }
            }
        }
    }

    private void initButtons() {
        this.clearWidgets();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int texWidth = 90;
        int texHeight = 20;

        addRenderableWidget(new TooltipButton(
                centerX - 100, centerY + 60, 90, 20,
                Component.translatable("screen.koniava.nara.bind"),
                BUTTON_TEXTURE, texWidth, texHeight,
                btn -> {
                    NaraBindRequestPacket.send(true);
                    onClose();
                },
                () -> BIND_TOOLTIP
        ));

        addRenderableWidget(new TooltipButton(
                centerX + 10, centerY + 60, 90, 20,
                Component.translatable("screen.koniava.nara.cancel"),
                BUTTON_TEXTURE, texWidth, texHeight,
                btn -> {
                    NaraBindRequestPacket.send(false);
                    onClose();
                    var connection = Minecraft.getInstance().getConnection();
                    if (connection != null) {
                        connection.disconnect(Component.translatable("message.koniava.nara.disconnect_message"));
                    }
                },
                () -> CANCEL_TOOLTIP
        ));
    }

    @Override
    protected void init() {
        Minecraft.getInstance().getTextureManager()
                .getTexture(OVERLAY_TEXTURE)
                .setFilter(false, false);
        updateLayoutCache();
    }

    private void updateLayoutCache() {
        cachedCenterX = this.width / 2;
        cachedCenterY = this.height / 2;
        cachedBgX = (this.width - BG_WIDTH) / 2;
        cachedBgY = (this.height - BG_HEIGHT) / 2;
        layoutCached = true;
        textCacheDirty = true;
    }

    private void refreshTextCache() {
        if (!textCacheDirty || this.font == null) {
            return;
        }
        for (int i = 0; i < lines.length; i++) {
            cachedLineSequences[i] = lines[i].getVisualOrderText();
            lineWidths[i] = this.font.width(cachedLineSequences[i]);
            cachedLineStartX[i] = cachedCenterX - lineWidths[i] / 2;
        }
        textCacheDirty = false;
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        layoutCached = false;
        textCacheDirty = true;
        if (currentStage == Stage.AWAITING_CONFIRM) {
            initButtons();
        } else {
            this.clearWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!layoutCached) {
            updateLayoutCache();
        }
        refreshTextCache();

        graphics.setColor(1F, 1F, 1F, 1F);
        graphics.fillGradient(cachedBgX - 12, cachedBgY - 12, cachedBgX + BG_WIDTH + 12, cachedBgY + BG_HEIGHT + 12, 0xA0000000, 0x70000000);
        graphics.blit(OVERLAY_TEXTURE, cachedBgX, cachedBgY, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        int startY = cachedCenterY - 50;
        graphics.drawCenteredString(this.font, title, cachedCenterX, startY, 0xFFFFFF);

        if (visibleLines > 0) {
            int linesToDraw = Math.min(visibleLines, cachedLineSequences.length);
            for (int i = 0; i < linesToDraw; i++) {
                graphics.drawString(this.font, cachedLineSequences[i], cachedLineStartX[i], startY + 20 + i * 12, 0xAAAAAA);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 由 render 方法進行背景渲染
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
