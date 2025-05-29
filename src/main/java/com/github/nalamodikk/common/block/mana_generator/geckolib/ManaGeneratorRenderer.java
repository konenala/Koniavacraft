package com.github.nalamodikk.common.block.mana_generator.geckolib;

import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;


public class ManaGeneratorRenderer extends GeoBlockRenderer<ManaGeneratorBlockEntity> {
    public ManaGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        super(new ManaGeneratorModel());
    }
}

