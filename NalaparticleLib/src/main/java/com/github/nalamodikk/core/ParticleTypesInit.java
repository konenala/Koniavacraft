package com.github.nalamodikk.core;

import com.github.NalaParticleLib;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ParticleTypesInit {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "nalaparticlelib");
    public static final RegistryObject<ParticleType<ControllableParticleEffect>> CONTROLLABLE =
            PARTICLE_TYPES.register("controllable", () ->
                    new ParticleType<ControllableParticleEffect>(false, ControllableParticleEffect.DESERIALIZER) {
                        @Override
                        public Codec<ControllableParticleEffect> codec() {
                            return ControllableParticleEffect.CODEC;
                        }
                    }
            );

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
        NalaParticleLib.LOGGER.info("✅ 註冊粒子 ParticleTypesInit 完成");


    }
}
