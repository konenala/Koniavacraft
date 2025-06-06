package com.github.nalamodikk.common.block.mana_generator.geckolib;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ManaGeneratorModel extends GeoModel<ManaGeneratorBlockEntity> {
    @Override
    public ResourceLocation getModelResource(ManaGeneratorBlockEntity animatable) {
        return KoniavacraftMod.rl("geo/mana_generator.geo.json");
    }

    @Override
    public ResourceLocation getAnimationResource(ManaGeneratorBlockEntity animatable) {
        return KoniavacraftMod.rl("animations/mana_generator.animation.json");
    }

    @Override
    public ResourceLocation getTextureResource(ManaGeneratorBlockEntity animatable) {
        return KoniavacraftMod.rl("textures/block/mana_generator_texture.png");
    }
}
