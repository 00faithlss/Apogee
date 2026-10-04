package dev.apogee.client.compat.punchy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.mantis.ImplantHandRenderer;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.mantis.MantisHandPose;
import dev.apogee.client.mantis.MantisRig;
import dev.apogee.client.nanowire.NanowireRig;
import dev.apogee.mantis.MantisVariant;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Matrix4f;
import org.slf4j.LoggerFactory;

public final class PunchyCompat {
    private static Method enabled;
    private static Method blacklisted;
    private static Method explicitTransform;
    private static Method setExplicitTransform;
    private static boolean resolved;
    private static Frame frame;
    private static boolean rendering;
    /** Test hook: told Punchy's pose of each first-person arm (true for the left), implants or not. */
    public static java.util.function.BiConsumer<Boolean, Matrix4f> probe;
    /** Test hook: told where each implant arm ends up in view space, after the implant's own offsets. */
    public static java.util.function.BiConsumer<Boolean, Matrix4f> finalProbe;

    private static final class Frame {
        final LocalPlayer player;
        final PoseStack poses;
        final MultiBufferSource buffers;
        final float partialTick;
        final int light;
        final Matrix4f[] arms = new Matrix4f[2];
        final boolean[] visible = new boolean[2];
        boolean nanowireRendered;
        boolean rendered;

        Frame(LocalPlayer player, PoseStack poses, MultiBufferSource buffers, float partialTick, int light) {
            this.player = player;
            this.poses = poses;
            this.buffers = buffers;
            this.partialTick = partialTick;
            this.light = light;
        }
    }

    private PunchyCompat() {
    }

    public static boolean active(LocalPlayer player) {
        if (!resolved) {
            resolved = true;
            if (FabricLoader.getInstance().isModLoaded("punchy")) {
                try {
                    Class<?> config = Class.forName("punchy.config.PunchyConfig");
                    enabled = config.getMethod("isModEnabled");
                    blacklisted = config.getMethod("isHandBlacklisted", Player.class, InteractionHand.class);
                    Class<?> part = Class.forName("punchy.client.access.TransformablePart");
                    explicitTransform = part.getMethod("punchy$getExplicitTransform");
                    setExplicitTransform = part.getMethod("punchy$setExplicitTransform", Matrix4f.class);
                } catch (ReflectiveOperationException exception) {
                    enabled = null;
                    LoggerFactory.getLogger("Apogee").warn("Punchy hand integration unavailable", exception);
                }
            }
        }
        if (enabled == null || blacklisted == null || player == null) {
            return false;
        }
        try {
            return (boolean) enabled.invoke(null)
                    && !(boolean) blacklisted.invoke(null, player, InteractionHand.MAIN_HAND)
                    && !(boolean) blacklisted.invoke(null, player, InteractionHand.OFF_HAND);
        } catch (ReflectiveOperationException exception) {
            enabled = null;
            LoggerFactory.getLogger("Apogee").warn("Punchy hand integration disabled", exception);
            return false;
        }
    }

    public static void begin(LocalPlayer player, PoseStack poses, MultiBufferSource buffers, float partialTick, int light) {
        reset();
        MantisVariant variant = MantisClient.variant(player);
        if ((buffers instanceof MultiBufferSource.BufferSource || variant != null && variant.isNanowire())
                && active(player) && !player.isScoping()
                && (MantisClient.implantViewReady(player) || MantisHandPose.holdFolded)) {
            frame = new Frame(player, poses, buffers, partialTick, light);
        }
    }

    public static boolean needsPose(LocalPlayer player) {
        return frame != null && frame.player == player && !frame.rendered && !nativeNanowireView(player);
    }

    /** Nanowire keeps Punchy's own skin renderer for the whole deploy/retract cycle. */
    public static boolean nativeNanowireView(LocalPlayer player) {
        MantisVariant variant = MantisClient.variant(player);
        return frame != null && frame.player == player && variant != null && variant.isNanowire();
    }

    /** The first native arm call draws both arms; the other call must not draw them again. */
    public static boolean claimNanowireArms(LocalPlayer player) {
        if (!nativeNanowireView(player) || frame.nanowireRendered) {
            return false;
        }
        frame.nanowireRendered = true;
        return true;
    }

    public static void capture(LocalPlayer player, HumanoidArm arm, Matrix4f matrix, boolean visible) {
        Frame current = frame;
        if (current == null || current.player != player || current.rendered) {
            return;
        }
        current.arms[arm == HumanoidArm.RIGHT ? 0 : 1] = new Matrix4f(matrix);
        current.visible[arm == HumanoidArm.RIGHT ? 0 : 1] = visible;
        if (current.arms[0] == null || current.arms[1] == null) {
            return;
        }
        current.poses.pushPose();
        current.poses.mulPose(Axis.ZP.rotationDegrees(180.0F));
        rendering = true;
        try {
            ((ImplantHandRenderer) Minecraft.getInstance().gameRenderer.itemInHandRenderer)
                    .apogee$renderImplants(current.partialTick, current.poses,
                            (MultiBufferSource.BufferSource) current.buffers, player, current.light);
            current.rendered = true;
        } finally {
            rendering = false;
            current.poses.popPose();
        }
    }

    public static boolean rendered() {
        return frame != null && frame.rendered;
    }

    public static boolean rendering() {
        return rendering;
    }

    public static boolean lowerOffArm(LocalPlayer player) {
        return !rendering || frame == null || !frame.visible[player.getMainArm() == HumanoidArm.RIGHT ? 1 : 0];
    }

    public static void reset() {
        frame = null;
        rendering = false;
    }

    private static Matrix4f explicitPose(ModelPart part) {
        try {
            return (Matrix4f) explicitTransform.invoke(part);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read Punchy's arm transform", exception);
        }
    }

    private static void explicitPose(ModelPart part, Matrix4f pose) {
        try {
            setExplicitTransform.invoke(part, new Object[]{pose});
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot restore Punchy's arm transform", exception);
        }
    }

    public static boolean renderArm(PoseStack poses, MultiBufferSource buffers, int light, LocalPlayer player,
                                     boolean right, float partialTick) {
        if (!rendering || frame == null || frame.player != player) {
            return false;
        }
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return false;
        }
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        ModelPart arm = right ? model.rightArm : model.leftArm;
        ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
        PartPose armPose = arm.storePose();
        PartPose sleevePose = sleeve.storePose();
        float ax = arm.xScale, ay = arm.yScale, az = arm.zScale;
        float sx = sleeve.xScale, sy = sleeve.yScale, sz = sleeve.zScale;
        boolean armVisible = arm.visible, sleeveVisible = sleeve.visible;
        Matrix4f armExplicit = explicitPose(arm);
        Matrix4f sleeveExplicit = explicitPose(sleeve);
        poses.pushPose();
        poses.mulPose(frame.arms[right ? 0 : 1]);
        try {
            // The captured matrix already includes Punchy's explicit transform. PartPose.ZERO
            // alone does not clear it: ModelPart would apply it again to skin and housing.
            explicitPose(arm, null);
            explicitPose(sleeve, null);
            arm.loadPose(PartPose.ZERO);
            sleeve.loadPose(PartPose.ZERO);
            arm.xScale = arm.yScale = arm.zScale = 1.0F;
            sleeve.xScale = sleeve.yScale = sleeve.zScale = 1.0F;
            arm.visible = sleeve.visible = true;
            if (finalProbe != null) {
                // Measure the mesh's actual pose too, so a second ModelPart transform is
                // visible to movement regressions rather than only measuring its parent.
                poses.pushPose();
                try {
                    arm.translateAndRotate(poses);
                    finalProbe.accept(!right, new Matrix4f(poses.last().pose()));
                } finally {
                    poses.popPose();
                }
            }
            MantisVariant variant = MantisClient.variant(player);
            if (variant != null && variant.isBlades()) {
                MantisRig.firstPersonPass = true;
                try {
                    MantisRig.drawOnArm(poses, buffers, light, player, arm, right, partialTick);
                } finally {
                    MantisRig.firstPersonPass = false;
                }
            } else {
                var consumer = buffers.getBuffer(RenderType.entitySolid(player.getSkin().texture()));
                arm.render(poses, consumer, light, OverlayTexture.NO_OVERLAY);
                if (player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE)) {
                    sleeve.render(poses, buffers.getBuffer(RenderType.entityTranslucent(player.getSkin().texture())),
                            light, OverlayTexture.NO_OVERLAY);
                }
                NanowireRig.drawOnArm(poses, buffers, light, player, arm, right, partialTick);
            }
        } finally {
            explicitPose(arm, armExplicit);
            explicitPose(sleeve, sleeveExplicit);
            arm.loadPose(armPose);
            sleeve.loadPose(sleevePose);
            arm.xScale = ax;
            arm.yScale = ay;
            arm.zScale = az;
            sleeve.xScale = sx;
            sleeve.yScale = sy;
            sleeve.zScale = sz;
            arm.visible = armVisible;
            sleeve.visible = sleeveVisible;
            poses.popPose();
        }
        return true;
    }
}
