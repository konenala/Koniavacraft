package com.github.nalamodikk.register.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.collector.solarmana.SolarCollectorRenderer;
import com.github.nalamodikk.common.block.mana_generator.geckolib.ManaGeneratorRenderer;
import com.github.nalamodikk.register.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)

public class ModRenderLayers {

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MANA_GENERATOR_BE.get(), ManaGeneratorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), SolarCollectorRenderer::new);
//        event.registerBlockEntityRenderer(ModBlockEntities.ARCANE_CONDUIT_BE.get(), ArcaneConduitBlockEntityRenderer::new);
    }

}
