package com.github.nalamodikk.client.event;

import com.github.nalamodikk.common.block.mana_generator.geckolib.ManaGeneratorRenderer;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class ClientRenderers {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MANA_GENERATOR_BE.get(), ManaGeneratorRenderer::new);


    }

}
