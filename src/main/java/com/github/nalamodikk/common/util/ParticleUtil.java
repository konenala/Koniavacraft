package com.github.nalamodikk.common.util;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.particles.SimpleParticleType;

public class ParticleUtil {
    public static SimpleParticleType getById(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return ParticleTypes.HAPPY_VILLAGER;

        ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(rl);
        return (type instanceof SimpleParticleType s) ? s : ParticleTypes.HAPPY_VILLAGER;
    }
}
