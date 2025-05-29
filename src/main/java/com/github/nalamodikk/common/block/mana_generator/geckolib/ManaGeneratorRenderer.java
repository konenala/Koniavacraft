package com.github.nalamodikk.common.block.mana_generator.geckolib;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;


public class ManaGeneratorRenderer extends GeoBlockRenderer<ManaGeneratorBlockEntity> {
    public ManaGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        super(new ManaGeneratorModel());
    }

    @Override
    public ResourceLocation getTextureLocation(ManaGeneratorBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID,
                animatable.isWorking() ? "textures/block/mana_generator_active.png" : "textures/block/mana_generator_texture.png"
        );
    }
}

