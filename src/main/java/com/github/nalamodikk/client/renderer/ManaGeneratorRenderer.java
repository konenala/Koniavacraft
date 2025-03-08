package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.entity.ManaGenerator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.client.model.ManaGeneratorModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ManaGeneratorRenderer extends GeoBlockRenderer<ManaGeneratorBlockEntity> {
    public ManaGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        super(new ManaGeneratorModel());
    }
    @Override
    public ResourceLocation getTextureLocation(ManaGeneratorBlockEntity animatable) {
        if (animatable.getIsWorking()) {
            return new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/block/mana_generator_active.png");
        } else {
            return new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/block/mana_generator.png");
        }
    }

}