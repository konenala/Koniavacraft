package com.github.nalamodikk.group.Particle;

import com.github.nalamodikk.group.ParticleGroup;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MagicCircleGroup extends ParticleGroup {
    public static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    public MagicCircleGroup(UUID groupId) {
        super(groupId);
    }

    @Override
    protected Iterable<Vec3> generatePoints() {
        List<Vec3> points = new ArrayList<>();
        double radius = 1.5;
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            points.add(new Vec3(x, 0, z));
        }
        return points;
    }
}
