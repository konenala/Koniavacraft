package com.github;

import com.github.nalamodikk.core.ControllableParticle;
import com.github.nalamodikk.core.ParticleTypesInit;
import com.github.nalamodikk.group.Particle.MagicCircleGroup;
import com.github.nalamodikk.group.ParticleGroupProvider;
import com.github.nalamodikk.net.NetworkHandler;
import com.github.nalamodikk.register.ModCreativeModTabs;
import com.github.nalamodikk.register.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(NalaParticleLib.MOD_ID)
public class NalaParticleLib {

    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "nalaparticlelib";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public NalaParticleLib() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ParticleTypesInit.register(modBus);
        NetworkHandler.register();
        ModItems.register(modBus);

        ModCreativeModTabs.register(modBus);
        LOGGER.debug("這是 Debug 訊息");
        LOGGER.info("這是 Info 訊息");
        LOGGER.warn("這是 Warn 訊息");
        LOGGER.error("這是 Error 訊息");
        // Client particle register
        modBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ParticleEngine engine = Minecraft.getInstance().particleEngine;
            engine.register(
                    ParticleTypesInit.CONTROLLABLE.get(),
                    spriteSet -> new ControllableParticle.Provider(spriteSet)
            );

            ParticleGroupProvider.register(MagicCircleGroup.ID, MagicCircleGroup::new);
        });
    }
}
