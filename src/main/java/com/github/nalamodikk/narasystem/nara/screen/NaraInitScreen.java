package com.github.nalamodikk.narasystem.nara.screen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.button.TooltipButton;
import com.github.nalamodikk.narasystem.nara.network.server.NaraBindRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.List;

public class NaraInitScreen extends Screen {
    private enum Stage { SHOWING_LINES, AWAITING_CONFIRM }

    private static final ResourceLocation GRADIENT_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_gradient.png");
    private static final ResourceLocation OVERLAY_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_overlay.png");
    private static final ResourceLocation BUTTON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/nara_button.png");

    private static final List<Component> BIND_TOOLTIP =
        List.of(Component.translatable("tooltip.koniava.nara.bind"));
    private static final List<Component> CANCEL_TOOLTIP =
        List.of(Component.translatable("tooltip.koniava.nara.cancel"));

    private static final int BG_WIDTH = 256;
    private static final int BG_HEIGHT = 190;
    private static final int LINE_HEIGHT = 12;

    private static final String MOD_VERSION = ModList.get()
        .getModContainerById(KoniavacraftMod.MOD_ID)
        .map(c -> c.getModInfo().getVersion().toString())
        .orElse("dev");

    private static final Component TITLE = Component.translatable("screen.koniava.nara.title");
    private static final Component[] TEXT_LINES = {
        Component.translatable("screen.koniava.nara.line1"),
        Component.translatable("screen.koniava.nara.line2"),
        Component.translatable("screen.koniava.nara.line3"),
        Component.translatable("screen.koniava.nara.line4"),
        Component.translatable("screen.koniava.nara.line5", MOD_VERSION),
        Component.translatable("screen.koniava.nara.line6")
    };

    private Stage currentStage = Stage.SHOWING_LINES;
    private int visibleLines = 0;
    private int ticksElapsed = 0;

    private TooltipButton bindButton;
    private TooltipButton cancelButton;

    public NaraInitScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        Minecraft.getInstance().getTextureManager()
            .getTexture(OVERLAY_TEXTURE).setFilter(false, false);

        int centerX = this.width / 2;
        int buttonY = this.height / 2 + 60;

        bindButton = addRenderableWidget(new TooltipButton(
            centerX - 100, buttonY, 90, 20,
            Component.translatable("screen.koniava.nara.bind"),
            BUTTON_TEXTURE, 90, 20,
            button -> {
                NaraBindRequestPacket.send(true);
                onClose();
            },
            () -> BIND_TOOLTIP
        ));

        cancelButton = addRenderableWidget(new TooltipButton(
            centerX + 10, buttonY, 90, 20,
            Component.translatable("screen.koniava.nara.cancel"),
            BUTTON_TEXTURE, 90, 20,
            button -> {
                NaraBindRequestPacket.send(false);
                var connection = Minecraft.getInstance().getConnection();
                if (connection != null) {
                    connection.disconnect(Component.translatable("message.koniava.nara.disconnect_message"));
                }
                onClose();
            },
            () -> CANCEL_TOOLTIP
        ));

        bindButton.visible = false;
        cancelButton.visible = false;
    }

    @Override
    public void tick() {
        super.tick();
        ticksElapsed++;

        if (currentStage == Stage.SHOWING_LINES) {
            if (visibleLines < TEXT_LINES.length && ticksElapsed % 10 == 0) {
                visibleLines++;
            }
            if (visibleLines == TEXT_LINES.length && ticksElapsed >= TEXT_LINES.length * 10 + 20) {
                currentStage = Stage.AWAITING_CONFIRM;
                bindButton.visible = true;
                cancelButton.visible = true;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int bgX = (this.width - BG_WIDTH) / 2;
        int bgY = (this.height - BG_HEIGHT) / 2;

        graphics.blit(GRADIENT_TEXTURE, 0, 0, 0, 0, this.width, this.height, 1, 256);
        graphics.blit(OVERLAY_TEXTURE, bgX, bgY, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        int startY = centerY - 50;
        graphics.drawCenteredString(this.font, TITLE, centerX, startY, 0xFFFFFF);

        for (int i = 0; i < visibleLines; i++) {
            graphics.drawCenteredString(this.font, TEXT_LINES[i],
                centerX, startY + 20 + i * LINE_HEIGHT, 0xAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return true; }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
