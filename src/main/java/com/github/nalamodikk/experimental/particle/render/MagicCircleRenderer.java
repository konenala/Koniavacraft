package com.github.nalamodikk.experimental.particle.render;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MagicCircleRenderer {
    public static void drawMagicCircle(Level level, Vec3 center) {
        int points = 60;
        double radius = 2.5;

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            double y = center.y;

            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
        }
    }
}
