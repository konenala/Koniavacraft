package com.github.nalamodikk.common.block.mana_generator.geckolib;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ManaGeneratorModel extends GeoModel<ManaGeneratorBlockEntity> {
    @Override
    public ResourceLocation getModelResource(ManaGeneratorBlockEntity animatable) {
        return MagicalIndustryMod.rl("geo/mana_generator.geo.json");
    }

    @Override
    public ResourceLocation getAnimationResource(ManaGeneratorBlockEntity animatable) {
        return MagicalIndustryMod.rl("animations/mana_generator.animation.json");
    }

    @Override
    public ResourceLocation getTextureResource(ManaGeneratorBlockEntity animatable) {
        return MagicalIndustryMod.rl("textures/block/mana_generator.png");
    }
}
