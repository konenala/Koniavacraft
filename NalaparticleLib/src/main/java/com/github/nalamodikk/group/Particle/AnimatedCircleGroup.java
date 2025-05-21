package com.github.nalamodikk.group.Particle;


import com.github.nalamodikk.core.ControllableParticleEffect;
import com.github.nalamodikk.group.ParticleGroup;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class AnimatedCircleGroup extends ParticleGroup {
    private final UUID bindPlayerUUID;
    private int tick = 0;
    private int anTick = 0;
    private final int anMaxTick = 30;
    private final int maxTick = 80;
    private double currentScale = 0.01;

    public AnimatedCircleGroup(UUID groupId, UUID bindPlayerUUID) {
        super(groupId);
        this.bindPlayerUUID = bindPlayerUUID;
    }

    @Override
    public void spawn(ClientLevel level) {
        if (tick++ >= maxTick) return;

        if (anTick < anMaxTick) {
            currentScale += 1.0 / anMaxTick;
            anTick++;
        } else if (tick >= maxTick - anMaxTick) {
            currentScale -= 1.0 / anMaxTick;
        }

        Vec3 centerPos = level.getPlayerByUUID(bindPlayerUUID) != null
                ? level.getPlayerByUUID(bindPlayerUUID).position().add(0, 1.5, 0)
                : new Vec3(0, 0, 0);

        int points = 48;
        double radius = 2.5 * currentScale;
        float rotation = (tick * 8f) % 360;

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points + Math.toRadians(rotation);
            double x = centerPos.x + radius * Math.cos(angle);
            double z = centerPos.z + radius * Math.sin(angle);
            double y = centerPos.y;

            level.addParticle(
                    new ControllableParticleEffect(groupId),
                    x, y, z,
                    0.0, 0.0, 0.0
            );
        }
    }

    @Override
    protected Iterable<Vec3> generatePoints() {
        return Collections.emptyList();
    }

}
