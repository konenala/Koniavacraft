package com.github.nalamodikk.group.Particle;

import com.github.nalamodikk.group.ParticleGroup;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Star5Group extends ParticleGroup {
    public Star5Group(UUID id) {
        super(id);
    }

    @Override
    protected Iterable<Vec3> generatePoints() {
        List<Vec3> points = new ArrayList<>();
        int vertices = 5;
        double rOuter = 1.5;
        double rInner = 0.6;

        for (int i = 0; i < vertices * 2; i++) {
            double angle = Math.PI / vertices * i;
            double r = (i % 2 == 0) ? rOuter : rInner;
            double x = r * Math.cos(angle);
            double z = r * Math.sin(angle);
            points.add(new Vec3(x, 0, z));
        }

        return points;
    }
}
