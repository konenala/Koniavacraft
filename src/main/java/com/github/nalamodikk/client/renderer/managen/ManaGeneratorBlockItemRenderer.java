package com.github.nalamodikk.client.renderer.managen;

import com.github.nalamodikk.client.model.managen.ManaGeneratorBlockItemModel;
import com.github.nalamodikk.common.block.blocks.blockitem.ManaGeneratorBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ManaGeneratorBlockItemRenderer extends GeoItemRenderer<ManaGeneratorBlockItem> {
    public ManaGeneratorBlockItemRenderer() {
        super(new ManaGeneratorBlockItemModel());
    }
}