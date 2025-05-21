package com.github.nalamodikk.group;

import com.github.nalamodikk.core.ControllableParticleEffect;
import com.github.nalamodikk.core.ParticleTypesInit;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public abstract class ParticleGroup {
    protected final UUID groupId;
    protected Vec3 center = Vec3.ZERO;
    protected float rotation = 0f;

    public ParticleGroup(UUID groupId) {
        this.groupId = groupId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setCenter(Vec3 center) {
        this.center = center;
    }

    public void setRotation(float degrees) {
        this.rotation = degrees;
    }

    public void spawn(ClientLevel level) {
        for (Vec3 point : generatePoints()) {
            Vec3 worldPos = center.add(point); // 或直接用 point
            level.addParticle(
                    new ControllableParticleEffect(groupId),
                    worldPos.x, worldPos.y, worldPos.z,
                    0.0, 0.0, 0.0
            );
        }

    }

    protected Vec3 rotateAroundY(Vec3 point, float angleDeg) {
        double angle = Math.toRadians(angleDeg);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = point.x * cos - point.z * sin;
        double z = point.x * sin + point.z * cos;
        return new Vec3(x, point.y, z);
    }

    protected abstract Iterable<Vec3> generatePoints();

    protected ParticleType<ControllableParticleEffect> getParticle() {
        return ParticleTypesInit.CONTROLLABLE.get();
    }

}
