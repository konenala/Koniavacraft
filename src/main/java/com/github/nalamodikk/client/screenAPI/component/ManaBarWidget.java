package com.github.nalamodikk.client.screenAPI.component;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;
import java.util.function.IntSupplier;

public class ManaBarWidget extends VerticalBarWidget {
    
    // 背景已經畫在 GUI 上了，所以這裡是 null
    private static final ResourceLocation MANA_FULL = KoniavacraftMod.rl("textures/gui/mana_bar_full.png");

    public ManaBarWidget(int x, int y, IntSupplier value, IntSupplier max) {
        super(x, y, 14, 50, null, MANA_FULL, value, max);
    }

    @Override
    protected String getTooltipPrefix() {
        return "Mana"; // 建議用 Component.translatable("tooltip.koniava.mana").getString()
    }
}
