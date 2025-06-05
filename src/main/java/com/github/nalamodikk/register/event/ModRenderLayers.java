package com.github.nalamodikk.register.event;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.common.block.mana_generator.geckolib.ManaGeneratorRenderer;
import com.github.nalamodikk.register.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)

public class ModRenderLayers {

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MANA_GENERATOR_BE.get(), ManaGeneratorRenderer::new);
    }



}
