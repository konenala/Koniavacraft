package com.github.nalamodikk.client.screenAPI.component;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;
import java.util.function.IntSupplier;

import net.minecraft.network.chat.Component;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

public class BurnProgressWidget extends VerticalBarWidget {
    
    private static final ResourceLocation FUEL_FULL = KoniavacraftMod.rl("textures/gui/fuel_bar.png");
    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");

    public BurnProgressWidget(int x, int y, IntSupplier value, IntSupplier max) {
        super(x, y, 14, 14, null, FUEL_FULL, value, max);
    }

    @Override
    public List<Component> getTooltip() {
        return Collections.singletonList(
            Component.translatable("jei.koniava.burn_time", 
                FORMAT.format(valueSupplier.getAsInt())
            )
        );
    }

    @Override
    protected String getTooltipPrefix() {
        return "";
    }
}
