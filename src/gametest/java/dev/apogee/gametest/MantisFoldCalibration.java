package dev.apogee.gametest;

import com.mojang.blaze3d.platform.NativeImage;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.mantis.MantisHandPose;
import dev.apogee.network.ApogeeNetworking.ToggleMantis;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketItem;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fits {@link MantisHandPose#FOLDED}: folds the blades, keeps a frame of the hand that takes over
 * afterwards (vanilla's, or another mod's) once it has settled, then holds the folded implant on
 * screen and moves
 * it at random, keeping each move that raises the overlap of the two silhouettes. Logs the best
 * pose and saves both frames. Run with -Dapogee.mantisCalibrate=true.
 */
final class MantisFoldCalibration {
    private static final Logger LOGGER = LoggerFactory.getLogger("MantisCalibrate");
    private static final float[] START_STEPS = {0.03F, 0.03F, 0.03F, 8.0F, 8.0F, 8.0F};
    private static final float[] MIN_STEPS = {0.0005F, 0.0005F, 0.0005F, 0.1F, 0.1F, 0.1F};
    private static final int MAX_EVALS = 500;
    /** Ticks after the fold at which the other hand has settled. */
    private static final int REST = 40;

    private static int foldedTick = -1;
    private static int[] reference;
    private static float[] best = new float[6];
    private static float[] steps = START_STEPS.clone();
    private static double bestScore = -1.0;
    private static final java.util.Random RANDOM = new java.util.Random(1L);
    private static final int RESTARTS = 4;
    private static final double ROTATION_COST = 0.0005;
    private static int restart;
    private static float[] overall = new float[6];
    private static final float[] SHIPPED = MantisHandPose.FOLDED.clone();
    private static double overallScore = -1.0;
    private static float[] trial;
    private static int evals;
    private static boolean done;

    private MantisFoldCalibration() {
    }

    static void tick(Minecraft mc, int t) {
        if (done) {
            return;
        }
        if (t == 0) {
            mc.options.bobView().set(false);
            MantisMotionStudy.server(mc, (level, player) -> {
                player.teleportTo(0.5, -60.0, 0.5);
                player.setYRot(0.0F);
                player.setXRot(10.0F);
                TrinketItem.equipItem(player, new ItemStack(ApogeeItems.MANTIS_BLADES));
            });
        }
        if (t == 5) {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(10.0F);
            ClientPlayNetworking.send(ToggleMantis.INSTANCE);
        }
        if (t == 45) {
            ClientPlayNetworking.send(ToggleMantis.INSTANCE);
        }
        if (t > 45 && foldedTick < 0 && !MantisClient.isOut(mc.player)) {
            foldedTick = t;
        }
        if (foldedTick < 0) {
            return;
        }
        int since = t - foldedTick;
        if (since >= 1 && since <= 8) {
            // The frames rendered after the fold's last tick: the other hand coming in.
            grab(mc, "calibrate-after-fold-" + since).close();
            return;
        }
        if (since == REST) {
            // Its settled pose, which does not depend on when the fold ended.
            NativeImage frame = grab(mc, "calibrate-rest");
            reference = pixels(frame);
            frame.close();
            MantisHandPose.holdFolded = true;
            // Start from the shipped pose: the first score is how well it fits.
            best = MantisHandPose.FOLDED.clone();
            overall = best.clone();
            trial = best.clone();
            setPose(trial);
            return;
        }
        if (since <= REST) {
            return;
        }
        NativeImage frame = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        // A small cost per degree keeps the fit on the plainest pose that looks the same: the arm
        // is a box, and far rotations can match it almost as well.
        double score = similarity(reference, pixels(frame)) - ROTATION_COST * (Math.abs(trial[3]) + Math.abs(trial[4]) + Math.abs(trial[5]));
        frame.close();
        evals++;
        if (bestScore < 0.0) {
            bestScore = score;
            LOGGER.info("calibrate start score={}", score);
        } else if (score > bestScore) {
            // (1+1) evolution strategy with the 1/5 success rule: widen the search on success.
            bestScore = score;
            best = trial.clone();
            scale(1.5F);
        } else {
            scale(0.975F);
        }
        if (evals >= MAX_EVALS || steps[0] < MIN_STEPS[0]) {
            LOGGER.info("calibrate restart {} score={} pose={}", restart, bestScore, Arrays.toString(best));
            if (bestScore > overallScore) {
                overallScore = bestScore;
                overall = best.clone();
            }
            restart++;
            if (restart == RESTARTS) {
                bestScore = overallScore;
                best = overall;
                finish(mc);
                return;
            }
            // Start again from another pose near the shipped one.
            evals = 0;
            steps = START_STEPS.clone();
            best = SHIPPED.clone();
            for (int i = 0; i < 6; i++) {
                best[i] += (float) RANDOM.nextGaussian() * START_STEPS[i];
            }
            bestScore = -1.0;
            trial = best.clone();
            setPose(trial);
            return;
        }
        trial = best.clone();
        for (int i = 0; i < 6; i++) {
            trial[i] += (float) RANDOM.nextGaussian() * steps[i];
        }
        setPose(trial);
    }

    private static void scale(float factor) {
        for (int i = 0; i < 6; i++) {
            steps[i] *= factor;
        }
    }

    private static void setPose(float[] pose) {
        System.arraycopy(pose, 0, MantisHandPose.FOLDED, 0, 6);
    }

    private static void finish(Minecraft mc) {
        done = true;
        setPose(best);
        LOGGER.info("calibrate best score={} evals={} pose={}", bestScore, evals, Arrays.toString(best));
        mc.execute(() -> {
            NativeImage frame = grab(mc, "calibrate-fitted");
            frame.close();
            LOGGER.info("Mantis motion study done");
            mc.stop();
        });
    }

    private static NativeImage grab(Minecraft mc, String name) {
        NativeImage frame = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        try {
            File dir = new File(mc.gameDirectory, "screenshots");
            dir.mkdirs();
            frame.writeToFile(new File(dir, name + ".png"));
        } catch (IOException e) {
            LOGGER.warn("could not save {}", name, e);
        }
        return frame;
    }

    /** The lower right of the frame, clear of the HUD, at half resolution; -1 where masked out. */
    private static int[] pixels(NativeImage frame) {
        int w = frame.getWidth();
        int h = frame.getHeight();
        int[] out = new int[(w / 2) * (h / 2)];
        java.util.Arrays.fill(out, -1);
        for (int y = (int) (h * 0.35F); y < h - 1; y += 2) {
            for (int x = w / 2; x < w - 1; x += 2) {
                if (y > h * 0.86F && x < w * 0.7F) {
                    continue;
                }
                out[(y / 2) * (w / 2) + x / 2] = frame.getPixelRGBA(x, y) & 0xFFFFFF;
            }
        }
        return out;
    }

    /**
     * One minus the mean colour difference (0 to 1) over the pixels where either frame shows a
     * hand: the faces and shading must match, not only the outline.
     */
    private static double similarity(int[] a, int[] b) {
        long diff = 0;
        int count = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < 0 || b[i] < 0 || (!hand(a[i]) && !hand(b[i]))) {
                continue;
            }
            diff += Math.abs((a[i] & 0xFF) - (b[i] & 0xFF)) + Math.abs(((a[i] >> 8) & 0xFF) - ((b[i] >> 8) & 0xFF))
                    + Math.abs(((a[i] >> 16) & 0xFF) - ((b[i] >> 16) & 0xFF));
            count++;
        }
        return count == 0 ? 0.0 : 1.0 - diff / (count * 765.0);
    }

    /** Neither grass green nor sky blue (pixels are ABGR). */
    private static boolean hand(int abgr) {
        int r = abgr & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int b = (abgr >> 16) & 0xFF;
        boolean grass = g > r + 12 && g > b + 12;
        boolean sky = b > r + 25 && b >= g;
        return !grass && !sky;
    }
}
