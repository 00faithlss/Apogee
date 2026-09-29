package dev.apogee.client.mantis;

import dev.apogee.mantis.MantisVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Heat coming off open thermal blades: red and white-hot specks rising from the blade, and short
 * horizontal streaks of specks like signal noise. Spawned along the blade as it was last drawn.
 */
public final class MantisBladeFx {
    private static final DustParticleOptions HOT = new DustParticleOptions(new Vector3f(1.0F, 0.18F, 0.10F), 0.55F);
    private static final DustParticleOptions WHITE_HOT = new DustParticleOptions(new Vector3f(1.0F, 0.62F, 0.50F), 0.4F);
    private static final DustParticleOptions NOISE = new DustParticleOptions(new Vector3f(1.0F, 0.08F, 0.06F), 0.35F);

    private MantisBladeFx() {
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            return;
        }
        long now = MantisClient.clientTicks();
        RandomSource random = level.random;
        MantisRig.forEachArm((entityId, arm) -> {
            if (arm.variant() != MantisVariant.THERMAL || arm.bladeSegmentCount == 0 || now - arm.bladeTick > 1) {
                return;
            }
            Entity entity = level.getEntity(entityId);
            if (entity == null || !MantisClient.isDeployed(entity) || MantisClient.deployProgress(entity, 1.0F) < 0.95F) {
                return;
            }
            // Fewer in first person, where they pass right in front of the camera.
            int count = arm.bladeFirstPerson ? 1 : 2;
            for (int i = 0; i < count; i++) {
                Vec3 at = pointOnBlade(arm, random);
                double rise = 0.02 + random.nextDouble() * 0.03;
                level.addParticle(heatParticle(random), at.x, at.y, at.z, 0.0, rise * 10.0, 0.0);
            }
            if (random.nextInt(arm.bladeFirstPerson ? 10 : 5) == 0) {
                // A glitch streak: a short row of specks across the blade, at eye level horizontal.
                Vec3 at = pointOnBlade(arm, random);
                Vec3 across = Vec3.directionFromRotation(0.0F, mc.gameRenderer.getMainCamera().getYRot() + 90.0F);
                double offset = (random.nextDouble() - 0.5) * 0.12;
                for (int dot = 0; dot < 4; dot++) {
                    double d = offset + (dot - 1.5) * 0.045;
                    level.addParticle(NOISE, at.x + across.x * d, at.y, at.z + across.z * d, 0.0, 0.0, 0.0);
                }
            }
        });
    }

    /** A red-hot speck, now and then a white-hot one. */
    public static ParticleOptions heatParticle(RandomSource random) {
        return random.nextInt(4) == 0 ? WHITE_HOT : HOT;
    }

    private static Vec3 pointOnBlade(MantisArm arm, RandomSource random) {
        int at = random.nextInt(arm.bladeSegmentCount) * 6;
        double t = random.nextDouble();
        double[] s = arm.bladeSegments;
        return new Vec3(s[at] + (s[at + 3] - s[at]) * t, s[at + 1] + (s[at + 4] - s[at + 1]) * t, s[at + 2] + (s[at + 5] - s[at + 2]) * t);
    }
}
