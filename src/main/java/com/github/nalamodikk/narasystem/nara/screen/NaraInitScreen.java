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

    private enum Stage {
        SHOWING_LINES, AWAITING_CONFIRM
    }
    private Stage currentStage = Stage.SHOWING_LINES;
    private int visibleLines = 0;

    private static final ResourceLocation overlayTexture = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_overlay.png");

    ResourceLocation buttonTexture = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/nara_button.png");
    private int ticksElapsed = 0;

    // 優化：快取渲染相關數據
    private int cachedCenterX = 0;
    private int cachedCenterY = 0;
    private int cachedBgX = 0;
    private int cachedBgY = 0;
    private boolean layoutCached = false;
    private static final int BG_WIDTH = 256;
    private static final int BG_HEIGHT = 190;

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

        addRenderableWidget(new TooltipButton(
                centerX - 100, centerY + 60, 90, 20,
                Component.translatable("screen.koniava.nara.bind"),
                buttonTexture, texWidth, texHeight,
                btn -> {
                    NaraBindRequestPacket.send(true);
                    onClose();
                },
                () -> List.of(Component.translatable("tooltip.koniava.nara.bind"))
        ));

        addRenderableWidget(new TooltipButton(
                centerX + 10, centerY + 60, 90, 20,
                Component.translatable("screen.koniava.nara.cancel"),
                buttonTexture, texWidth, texHeight,
                btn -> {
                    NaraBindRequestPacket.send(false);

                    onClose();
                    // ❗中斷連線，顯示提示訊息
                    var connection = Minecraft.getInstance().getConnection();
                    if (connection != null) {
                        connection.disconnect(Component.translatable("message.koniava.nara.disconnect_message"));
                    }
                },
                () -> List.of(Component.translatable("tooltip.koniava.nara.cancel"))
        ));
    }


    @Override
    protected void init() {
        Minecraft.getInstance().getTextureManager()
                .getTexture(overlayTexture)
                .setFilter(false, false); // 關閉 linear filtering

        // 優化：計算並快取佈局數據
        updateLayoutCache();
    }

    // 優化：快取佈局計算
    private void updateLayoutCache() {
        cachedCenterX = this.width / 2;
        cachedCenterY = this.height / 2;
        cachedBgX = (this.width - BG_WIDTH) / 2;
        cachedBgY = (this.height - BG_HEIGHT) / 2;
        layoutCached = true;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 優化：高效的背景渲染
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        // 優化：重新計算快取
        updateLayoutCache();
        this.rebuildWidgets();
        this.initButtons();
        // 🔁 重建 UI 按鈕
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 優化：檢查快取是否有效
        if (!layoutCached) {
            updateLayoutCache();
        }

        graphics.setColor(1F, 1F, 1F, 1F); // 保證透明正常

        // 優化：使用更高效的背景渲染
        renderBackground(graphics, mouseX, mouseY, partialTick);

        // 優化：使用快取的背景位置
        graphics.blit(overlayTexture, cachedBgX, cachedBgY, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        // 優化：快取文字起始位置
        int startY = cachedCenterY - 50;

        graphics.drawCenteredString(this.font, title, cachedCenterX, startY, 0xFFFFFF);

        // 優化：只渲染可見的文字行，減少繪製調用
        if (visibleLines > 0) {
            for (int i = 0; i < visibleLines; i++) {
                graphics.drawCenteredString(this.font, lines[i], cachedCenterX, startY + 20 + i * 12, 0xAAAAAA);
            }
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
