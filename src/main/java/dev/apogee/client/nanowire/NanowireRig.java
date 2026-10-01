package dev.apogee.client.nanowire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisVariant;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.function.IntPredicate;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Both nanowire housings of every player in view: their animation state and renderers, and the
 * transform that seats each export on a player arm. The arm itself stays the player's own, drawn
 * by vanilla or by whichever mod poses the hands; the housing just rides on it.
 */
public final class NanowireRig {
    private static final Int2ObjectOpenHashMap<NanowireRig> RIGS = new Int2ObjectOpenHashMap<>();

    /*
     * Seating, from an export's arm space (the pack's editor space in pixels: +X to the wearer's
     * right, +Y up, the arm's axis along Z with the fingertips at z = -11.5, the housing on the
     * inner side) to a player arm part (+Y down the arm to the hand, +X to the wearer's left, the
     * body's side at +X for the right arm and -X for the left). A quarter turn about X takes -Z
     * down the arm, and a half turn about the arm then puts the inner side towards the body and
     * the forearm's top on the arm's front, which faces up when the arm is held forward.
     */
    private static final float FINGERTIPS = 11.5F;
    /** Distance from an arm part's pivot to the end of its hand, in model pixels. */
    private static final float ARM_END = 10.0F;
    /**
     * Sideways offset of the export's arm axis from the arm part's pivot, in model pixels. The
     * housing is modelled on a 4-pixel-wide arm; this puts it flush on the inner face, which sits
     * one pixel from the pivot on both the wide and the slim arm.
     */
    private static final float AXIS_OFFSET = 1.0F;

    private final NanowireArm[] arms = {new NanowireArm(true), new NanowireArm(false)};
    private final NanowireArmGeoRenderer[] renderers = {new NanowireArmGeoRenderer(), new NanowireArmGeoRenderer()};
    /** Where each wire's lead runs, captured as each housing is drawn. */
    private final NanowireWire.Anchors[] anchors = {new NanowireWire.Anchors(), new NanowireWire.Anchors()};

    private NanowireRig() {
    }

    public static NanowireRig of(int entityId) {
        return RIGS.computeIfAbsent(entityId, id -> new NanowireRig());
    }

    public static void clear() {
        RIGS.clear();
    }

    /** Drops the rigs of players no longer in the level. */
    public static void prune(IntPredicate present) {
        RIGS.int2ObjectEntrySet().removeIf(entry -> !present.test(entry.getIntKey()));
    }

    /**
     * Draws both of the wearer's wires, once both housings have been drawn in this pass; {@code frame}
     * is the wearer's frame in the same render space.
     */
    public static void drawWires(MultiBufferSource buffers, AbstractClientPlayer player, NanowireWire.Frame frame, float partialTick) {
        NanowireRig rig = RIGS.get(player.getId());
        if (rig != null) {
            NanowireWire.draw(buffers, player, rig.anchors, frame, partialTick);
        }
    }

    /** A lash by one arm: both housings play the clip, each moving its own wire. */
    public void attack(int arm) {
        for (NanowireArm each : arms) {
            each.attack(arm);
        }
    }

    /** The two-wire lash. */
    public void heavy() {
        for (NanowireArm each : arms) {
            each.heavy();
        }
    }

    /**
     * Draws the nanowire housing on one of a player's arms, if they wear the nanowire: seats the
     * export on the arm part's current pose and brings its animation state up to date.
     */
    public static void drawOnArm(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                 ModelPart armPart, boolean right, float partialTick) {
        if (MantisClient.variant(player) != MantisVariant.NANOWIRE) {
            return;
        }
        NanowireRig rig = of(player.getId());
        float progress = MantisClient.deployProgress(player, partialTick);
        boolean deployed = MantisClient.isDeployed(player);
        NanowireArm arm = rig.arms[right ? 0 : 1];
        // A change first seen once its travel is over (a player coming into view with the wire
        // out) skips the transition nobody saw.
        arm.setDeployed(deployed, deployed ? progress >= 1.0F : progress <= 0.0F);
        poseStack.pushPose();
        armPart.translateAndRotate(poseStack);
        NanowireGrip.render(poseStack, buffers, light, player, right, progress);
        poseStack.translate((right ? -AXIS_OFFSET : AXIS_OFFSET) / 16.0F, (ARM_END - FINGERTIPS) / 16.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        rig.renderers[right ? 0 : 1].renderInto(poseStack, arm, buffers, light, partialTick);
        rig.anchors[right ? 0 : 1].capture(poseStack.last(), right);
        poseStack.popPose();
    }
}
