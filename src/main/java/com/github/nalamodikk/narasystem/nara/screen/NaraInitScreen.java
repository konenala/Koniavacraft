package com.github.nalamodikk.narasystem.nara.screen;


import com.github.nalamodikk.client.screenAPI.GenericButtonWithTooltip;
import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.narasystem.nara.network.server.NaraBindRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.List;

public class NaraInitScreen extends Screen {

    private enum Stage {
        SHOWING_LINES, AWAITING_CONFIRM
    }
    private Stage currentStage = Stage.SHOWING_LINES;
    private int visibleLines = 0;

    private static final ResourceLocation overlayTexture = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/nara_overlay.png");

    ResourceLocation buttonTexture = ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "textures/gui/widget/nara_button.png");
    private int ticksElapsed = 0;

    private final Component title = Component.translatable("screen.magical_industry.nara.title");
    private final Component[] lines = new Component[] {
            Component.translatable("screen.magical_industry.nara.line1"),
            Component.translatable("screen.magical_industry.nara.line2"),
            Component.translatable("screen.magical_industry.nara.line3"),
            Component.translatable("screen.magical_industry.nara.line4"),
            Component.translatable("screen.magical_industry.nara.line5",
                    ModList.get().getModFileById(MagicalIndustryMod.MOD_ID)
                            .getMods().getFirst().getVersion().toString()
            ),
            Component.translatable("screen.magical_industry.nara.line6")
    };

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
                initButtons(); // 延後加入按鈕
            }
        }
    }
//LightningBoltRenderer
    // ✅ 玩家登入時已強制綁定，不需每次操作再次檢查 Nara 綁定狀態
    // 若未來允許跳過動畫或支援非強制模式，需補上 isBound() 檢查

    private void initButtons() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int texWidth = 90;
        int texHeight = 20;

        addRenderableWidget(new GenericButtonWithTooltip(
                centerX - 100, centerY + 60, 90, 20,
                Component.translatable("screen.magical_industry.nara.bind"),
                buttonTexture, texWidth, texHeight,
                btn -> {
                    NaraBindRequestPacket.send(true);
                    onClose();
                },
                () -> List.of(Component.translatable("tooltip.magical_industry.nara.bind"))
        ));

        addRenderableWidget(new GenericButtonWithTooltip(
                centerX + 10, centerY + 60, 90, 20,
                Component.translatable("screen.magical_industry.nara.cancel"),
                buttonTexture, texWidth, texHeight,
                btn -> {
                    NaraBindRequestPacket.send(false);

                    onClose();
                    // ❗中斷連線，顯示提示訊息
                    var connection = Minecraft.getInstance().getConnection();
                    if (connection != null) {
                        connection.disconnect(Component.translatable("message.magical_industry.nara.disconnect_message"));
                    }
                },
                () -> List.of(Component.translatable("tooltip.magical_industry.nara.cancel"))
        ));
    }


    @Override
    protected void init() {
        Minecraft.getInstance().getTextureManager()
                .getTexture(overlayTexture)
                .setFilter(false, false); // 關閉 linear filtering

    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // ❌ 不要呼叫 super，不要呼叫 renderBlurredBackground
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        this.rebuildWidgets();
        this.initButtons();
        // 🔁 重建 UI 按鈕
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.setColor(1F, 1F, 1F, 1F); // 保證透明正常

        // 背景遮罩
        graphics.fill(0, 0, this.width, this.height, 0xFF000000); // 自己畫純黑遮罩

        // 背景貼圖
        int bgWidth = 256;
        int bgHeight = 190;
        int bgX = (this.width - bgWidth) / 2;
        int bgY = (this.height - bgHeight) / 2;

        graphics.blit(overlayTexture, bgX, bgY, 0, 0, bgWidth, bgHeight, bgWidth, bgHeight);

        // 文字
        int centerX = this.width / 2;
        int startY = this.height / 2 - 50;

        graphics.drawCenteredString(this.font, title, centerX, startY, 0xFFFFFF);
        for (int i = 0; i < visibleLines; i++) {
            graphics.drawCenteredString(this.font, lines[i], centerX, startY + 20 + i * 12, 0xAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
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
