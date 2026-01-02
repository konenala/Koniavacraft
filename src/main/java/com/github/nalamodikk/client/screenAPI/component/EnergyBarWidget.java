package com.github.nalamodikk.client.screenAPI.component;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;
import java.util.function.IntSupplier;

public class EnergyBarWidget extends VerticalBarWidget {
    
    // 背景已經畫在 GUI 上了，所以這裡是 null
    private static final ResourceLocation ENERGY_FULL = KoniavacraftMod.rl("textures/gui/energy_bar_full.png");

    public EnergyBarWidget(int x, int y, IntSupplier value, IntSupplier max) {
        // 假設能量條標準寬高是 12x48 或類似，這裡先設為 10x50，您可以在 new 的時候調整
        // 這裡我們暫定為 14x50 (基於常見設計)
        super(x, y, 14, 50, null, ENERGY_FULL, value, max);
    }

    @Override
    protected String getTooltipPrefix() {
        return "Energy"; // 建議用 Component.translatable("tooltip.koniava.energy").getString()
    }
}
