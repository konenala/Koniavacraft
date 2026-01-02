package com.github.nalamodikk.client.screenAPI.component;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;
import java.util.function.IntSupplier;

public class ArrowProgressWidget extends HorizontalBarWidget {
    
    private static final ResourceLocation ARROW_EMPTY = KoniavacraftMod.rl("textures/gui/widget/empty-arrow.png");
    private static final ResourceLocation ARROW_FULL = KoniavacraftMod.rl("textures/gui/widget/full-arrow.png");

    public ArrowProgressWidget(int x, int y, IntSupplier progress, IntSupplier max) {
        // 預設大小 (通常為 24x17)
        this(x, y, 24, 17, progress, max);
    }

    public ArrowProgressWidget(int x, int y, int width, int height, IntSupplier progress, IntSupplier max) {
        super(x, y, width, height, ARROW_EMPTY, ARROW_FULL, progress, max);
    }
}
