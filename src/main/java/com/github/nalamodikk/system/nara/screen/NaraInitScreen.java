package com.github.nalamodikk.system.nara.screen;


import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

public class NaraInitScreen extends Screen {
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
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        addRenderableWidget(Button.builder(Component.translatable("screen.magical_industry.nara.bind"), btn -> {
            NaraBindRequestPacket.send(true);
            onClose();
        }).bounds(new ScreenRectangle(centerX - 100, centerY + 60, 90, 20)).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.magical_industry.nara.cancel"), btn -> {
            NaraBindRequestPacket.send(false);
            onClose();
        }).bounds(new ScreenRectangle(centerX + 10, centerY + 60, 90, 20)).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics,mouseX,mouseY,partialTick);
        int centerX = this.width / 2;
        int startY = this.height / 2 - 50;

        graphics.drawCenteredString(this.font, title, centerX, startY, 0xFFFFFF);
        for (int i = 0; i < lines.length; i++) {
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
