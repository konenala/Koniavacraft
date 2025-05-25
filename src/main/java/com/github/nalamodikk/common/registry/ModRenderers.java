package com.github.nalamodikk.common.registry;

import com.github.nalamodikk.client.renderer.managen.ManaGeneratorRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class ModRenderers {
    public static void registerBlockEntityRenderers() {
        BlockEntityRenderers.register(ModBlockEntities.MANA_GENERATOR_BE.get(), ManaGeneratorRenderer::new);

    }
}
