package com.github.nalamodikk.group.Particle;

import com.github.nalamodikk.group.ParticleGroup;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Star6Group extends ParticleGroup {
    public Star6Group(UUID id) {
        super(id);
    }

    @Override
    protected Iterable<Vec3> generatePoints() {
        List<Vec3> points = new ArrayList<>();
        int trianglePoints = 3;
        double radius = 1.2;

        // 正三角
        for (int i = 0; i < trianglePoints; i++) {
            double angle = 2 * Math.PI * i / trianglePoints;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            points.add(new Vec3(x, 0, z));
        }

        // 反三角
        for (int i = 0; i < trianglePoints; i++) {
            double angle = 2 * Math.PI * (i + 0.5) / trianglePoints;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            points.add(new Vec3(x, 0, z));
        }

        return points;
    }
}
