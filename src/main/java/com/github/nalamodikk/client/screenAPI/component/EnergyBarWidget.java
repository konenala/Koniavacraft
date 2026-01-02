package com.github.nalamodikk.client.screenAPI.component;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;
import java.util.function.IntSupplier;

import net.minecraft.network.chat.Component;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

public class EnergyBarWidget extends VerticalBarWidget {
    
    private static final ResourceLocation BAR_EMPTY = KoniavacraftMod.rl("textures/gui/widget/bar_empty.png");
    private static final ResourceLocation ENERGY_FULL = KoniavacraftMod.rl("textures/gui/energy_bar_full.png");
    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");

    public EnergyBarWidget(int x, int y, IntSupplier value, IntSupplier max) {
        // 恢復預設背景，保證通用性
        super(x, y, 14, 50, BAR_EMPTY, ENERGY_FULL, value, max);
    }

    @Override
    public List<Component> getTooltip() {
        return Collections.singletonList(
            Component.translatable("tooltip.energy", 
                FORMAT.format(valueSupplier.getAsInt()), 
                FORMAT.format(maxSupplier.getAsInt())
            )
        );
    }

    @Override
    protected String getTooltipPrefix() {
        return ""; // 不再使用，因為已覆寫 getTooltip
    }
}
