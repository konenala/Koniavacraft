package com.github.nalamodikk.group.Particle;


import com.github.nalamodikk.core.ControllableParticleEffect;
import com.github.nalamodikk.group.ParticleGroup;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class RotatingHexStarGroup extends ParticleGroup {
    private final UUID groupId;
    private final Vec3 center;
    private float rotation = 0f;
    private int tick = 0;

    public RotatingHexStarGroup(UUID groupId, Vec3 center) {
        super(groupId);
        this.groupId = groupId;
        this.center = center;
    }

    @Override
    public void spawn(ClientLevel level) {
        rotation += 3f; // 每 tick 旋轉角度

        // 白色主圓
        drawCircle(level, 2.5, 60, 0xffffff);

        // 跟著主圓旋轉的六芒星
        drawHexagram(level, 2.0, 0xff66ff);
    }

    private void drawCircle(ClientLevel level, double radius, int points, int color) {
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            double y = center.y;

            level.addParticle(
                    new ControllableParticleEffect(groupId),
                    x, y, z,
                    0, 0, 0
            );
        }
    }

    private void drawHexagram(ClientLevel level, double radius, int color) {
        List<Vec3> points = new ArrayList<>();

        // 正三角形
        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(rotation + i * 120);
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            double y = center.y;
            points.add(new Vec3(x, y, z));
        }

        // 反三角形
        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(rotation + i * 120 + 60);
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            double y = center.y;
            points.add(new Vec3(x, y, z));
        }

        for (Vec3 point : points) {
            level.addParticle(
                    new ControllableParticleEffect(groupId),
                    point.x, point.y, point.z,
                    0, 0, 0
            );
        }
    }

    @Override
    protected Iterable<Vec3> generatePoints() {
        return Collections.emptyList();
    }
}
