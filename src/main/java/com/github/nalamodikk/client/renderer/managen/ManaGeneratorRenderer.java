package com.github.nalamodikk.client.renderer.managen;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.TileEntity.ManaGenerator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.client.model.managen.ManaGeneratorModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ManaGeneratorRenderer extends GeoBlockRenderer<ManaGeneratorBlockEntity> {
    public ManaGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        super(new ManaGeneratorModel());
    }
    @Override
    public ResourceLocation getTextureLocation(ManaGeneratorBlockEntity animatable) {
        return new ResourceLocation(MagicalIndustryMod.MOD_ID,
                animatable.getIsWorking() ? "textures/block/mana_generator_active.png" : "textures/block/mana_generator_texture.png"
        );
    }


}