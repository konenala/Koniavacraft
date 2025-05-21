package com.github.nalamodikk.group;

import com.github.nalamodikk.core.ControllableParticle;
import com.github.nalamodikk.core.ControllableParticleEffect;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class ParticleGroupProvider {
    private static final Map<UUID, Function<UUID, ParticleGroup>> GROUPS = new HashMap<>();

    // 註冊一個粒子群組建構器
    public static void register(UUID id, Function<UUID, ParticleGroup> constructor) {
        GROUPS.put(id, constructor);
    }

    // 根據 UUID 建立粒子群組
    public static ParticleGroup create(UUID id) {
        Function<UUID, ParticleGroup> constructor = GROUPS.get(id);
        return constructor != null ? constructor.apply(id) : null;
    }


    public static class Provider implements ParticleProvider<ControllableParticleEffect> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(ControllableParticleEffect type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            ControllableParticle particle = new ControllableParticle(
                    level, x, y, z, new Vec3(dx, dy, dz), type.controlId
            );
            particle.pickSprite(this.sprite);
            return particle;
        }
    }

}



