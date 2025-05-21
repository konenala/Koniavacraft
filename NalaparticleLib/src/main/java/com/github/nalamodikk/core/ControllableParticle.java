package com.github.nalamodikk.core;

import com.github.NalaParticleLib;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ControllableParticle extends TextureSheetParticle {
    protected final UUID controlId;

    public ControllableParticle(ClientLevel world, double x, double y, double z, Vec3 velocity, UUID controlId) {
        super(world, x, y, z, velocity.x, velocity.y, velocity.z);
        this.controlId = controlId;
        this.hasPhysics = false;
    }

    public UUID getControlId() {
        return controlId;
    }

    // 禁止 override tick() → 使用預先註冊的行為
    @Override public final void tick() {
        if (onTickAction != null) onTickAction.run();
        super.tick();
        if (age == 1) {
            NalaParticleLib.LOGGER.info("⏱️ 粒子開始 tick，UUID={}", controlId);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    protected Runnable onTickAction;

    public void setOnTick(Runnable runnable) {
        this.onTickAction = runnable;
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
            NalaParticleLib.LOGGER.info("🎇 建立粒子：位置=({}, {}, {}), UUID={}", x, y, z, type.controlId);

            particle.pickSprite(this.sprite);
            return particle;
        }
    }

}
