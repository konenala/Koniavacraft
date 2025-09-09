package com.github.nalamodikk.register.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarCollectorRenderer;
import com.github.nalamodikk.common.block.blockentity.mana_generator.render.ManaGeneratorRenderer;
import com.github.nalamodikk.common.block.blockentity.ritual.render.ArcanePedestalRenderer;
import com.github.nalamodikk.common.block.blockentity.ritual.render.ManaPylonRenderer;
import com.github.nalamodikk.register.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)

public class ModRenderLayers {

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MANA_GENERATOR_BE.get(), ManaGeneratorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), SolarCollectorRenderer::new);
        // 儀式系統渲染器
        event.registerBlockEntityRenderer(ModBlockEntities.ARCANE_PEDESTAL.get(), ArcanePedestalRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MANA_PYLON.get(), ManaPylonRenderer::new);
//        event.registerBlockEntityRenderer(ModBlockEntities.ARCANE_CONDUIT_BE.get(), ArcaneConduitBlockEntityRenderer::new);
    }

}
