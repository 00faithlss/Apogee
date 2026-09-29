package dev.apogee.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.ApogeeClientState;
import dev.apogee.client.config.ApogeeClientConfig;
import dev.apogee.mixin.client.LivingEntityRendererAccessor;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;

/**
 * Rainbow silhouettes left behind by active wearers, drawn with the wearer's pose, skin and
 * equipment. Every client records them locally from the synced wearer set, so the wearer and
 * all nearby players see them; count, range, fade and opacity are local preferences.
 */
public final class AfterimageRenderer {
    private static final float HUE_SHIFT_PER_IMAGE = 0.08F;
    private static final Map<UUID, Trail> TRAILS = new HashMap<>();
    private static boolean rendering;

    private AfterimageRenderer() {
    }

    private record Snapshot(double x, double y, double z, float bodyYaw, long tick, float hue, PartPose[] parts) {
    }

    private static final class Trail {
        final Deque<Snapshot> snapshots = new ArrayDeque<>();
        float nextHue;
    }

    private record PartPose(float x, float y, float z, float xRot, float yRot, float zRot,
                            float xScale, float yScale, float zScale, boolean visible) {
        static PartPose of(ModelPart part) {
            return new PartPose(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
                    part.xScale, part.yScale, part.zScale, part.visible);
        }

        void apply(ModelPart part) {
            part.x = x;
            part.y = y;
            part.z = z;
            part.xRot = xRot;
            part.yRot = yRot;
            part.zRot = zRot;
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
            part.visible = visible;
        }
    }

    /** True while afterimages are being drawn. */
    public static boolean isRendering() {
        return rendering;
    }

    public static void reset() {
        TRAILS.clear();
    }

    public static void tick(long clientTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ApogeeClientConfig config = ApogeeClientConfig.get();
        if (level == null || !config.afterimages || config.afterimageSilhouettes == 0) {
            TRAILS.clear();
            return;
        }
        long maxAge = Math.round(config.afterimageFadeSeconds * 20.0);
        TRAILS.values().forEach(trail -> trail.snapshots.removeIf(snapshot -> clientTick - snapshot.tick > maxAge));
        TRAILS.values().removeIf(trail -> trail.snapshots.isEmpty());

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        double range = config.afterimageRangeBlocks;
        for (int id : ApogeeClientState.currentWearers()) {
            if (!(level.getEntity(id) instanceof AbstractClientPlayer player) || !player.isAlive()) {
                continue;
            }
            if (player.position().distanceToSqr(camera) > range * range) {
                continue;
            }
            Trail trail = TRAILS.computeIfAbsent(player.getUUID(), ignored -> new Trail());
            Snapshot last = trail.snapshots.peekLast();
            double spacing = config.afterimageSpacingBlocks;
            if (last != null && player.position().distanceToSqr(last.x, last.y, last.z) < spacing * spacing) {
                continue;
            }
            PartPose[] pose = capturePose(player);
            if (pose == null) {
                continue;
            }
            trail.snapshots.addLast(new Snapshot(player.getX(), player.getY(), player.getZ(), player.yBodyRot, clientTick, trail.nextHue, pose));
            trail.nextHue = (trail.nextHue + HUE_SHIFT_PER_IMAGE) % 1.0F;
            while (trail.snapshots.size() > config.afterimageSilhouettes) {
                trail.snapshots.removeFirst();
            }
        }
    }

    public static void render(WorldRenderContext context, long clientTick) {
        if (rendering || TRAILS.isEmpty() || context.consumers() == null || context.matrixStack() == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        ApogeeClientConfig config = ApogeeClientConfig.get();
        Vec3 camera = context.camera().getPosition();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        double minFirstPerson = config.afterimageFirstPersonMinDistance;
        float maxAge = (float) (config.afterimageFadeSeconds * 20.0);
        float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
        PoseStack poseStack = context.matrixStack();
        MultiBufferSource buffers = context.consumers();
        TintedBufferSource tinted = new TintedBufferSource(buffers);

        rendering = true;
        try {
            Iterator<Map.Entry<UUID, Trail>> iterator = TRAILS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Trail> entry = iterator.next();
                if (!(level.getPlayerByUUID(entry.getKey()) instanceof AbstractClientPlayer player)) {
                    iterator.remove();
                    continue;
                }
                PlayerRenderer renderer = playerRenderer(player);
                if (renderer == null) {
                    continue;
                }
                PlayerModel<AbstractClientPlayer> model = renderer.getModel();
                ModelPart[] parts = parts(model);
                PartPose[] restore = snapshot(parts);
                try {
                    for (Snapshot snapshot : entry.getValue().snapshots) {
                        float age = clientTick - snapshot.tick + partialTick;
                        float fade = 1.0F - Mth.clamp(age / maxAge, 0.0F, 1.0F);
                        float alpha = config.afterimageOpacity * fade;
                        if (alpha <= 0.015F) {
                            continue;
                        }
                        double distanceSqr = camera.distanceToSqr(snapshot.x, snapshot.y + player.getBbHeight() * 0.5, snapshot.z);
                        if (distanceSqr > config.afterimageRangeBlocks * config.afterimageRangeBlocks) {
                            continue;
                        }
                        if (firstPerson && distanceSqr < minFirstPerson * minFirstPerson) {
                            continue;
                        }
                        int rgb = Mth.hsvToRgb(snapshot.hue, 0.95F, 1.0F);
                        int argb = Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24 | rgb & 0xFFFFFF;
                        for (int i = 0; i < parts.length; i++) {
                            snapshot.parts[i].apply(parts[i]);
                        }
                        drawSnapshot(player, renderer, model, snapshot, argb, alpha, camera, poseStack, buffers, tinted);
                    }
                } finally {
                    for (int i = 0; i < parts.length; i++) {
                        restore[i].apply(parts[i]);
                    }
                }
            }
        } finally {
            rendering = false;
        }
    }

    private static void drawSnapshot(AbstractClientPlayer player, PlayerRenderer renderer, PlayerModel<AbstractClientPlayer> model,
                                     Snapshot snapshot, int argb, float alpha, Vec3 camera, PoseStack poseStack,
                                     MultiBufferSource buffers, TintedBufferSource tinted) {
        int light = LevelRenderer.getLightColor(player.level(), BlockPos.containing(snapshot.x, snapshot.y + 1.0, snapshot.z));
        poseStack.pushPose();
        poseStack.translate(snapshot.x - camera.x, snapshot.y - camera.y, snapshot.z - camera.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - snapshot.bodyYaw));
        poseStack.scale(-0.9375F, -0.9375F, 0.9375F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(player.getSkin().texture())),
                light, OverlayTexture.NO_OVERLAY, argb);
        // Equipment layers mostly use cutout render types that ignore alpha, so they drop out
        // before the silhouette has fully faded instead of popping at the very end.
        if (alpha > 0.2F) {
            tinted.setTint(argb);
            List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> layers =
                    ((LivingEntityRendererAccessor<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) renderer).apogee$layers();
            float ageInTicks = player.tickCount;
            for (RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> layer : layers) {
                layer.render(poseStack, tinted, light, player, 0.0F, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
            }
        }
        poseStack.popPose();
    }

    private static PartPose[] capturePose(AbstractClientPlayer player) {
        PlayerRenderer renderer = playerRenderer(player);
        if (renderer == null) {
            return null;
        }
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        ModelPart[] parts = parts(model);
        PartPose[] restore = snapshot(parts);
        try {
            model.setAllVisible(true);
            model.hat.visible = player.isModelPartShown(PlayerModelPart.HAT);
            model.jacket.visible = player.isModelPartShown(PlayerModelPart.JACKET);
            model.leftPants.visible = player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
            model.rightPants.visible = player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
            model.leftSleeve.visible = player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
            model.rightSleeve.visible = player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
            model.crouching = player.isCrouching();
            HumanoidModel.ArmPose main = player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            HumanoidModel.ArmPose off = player.getItemInHand(InteractionHand.OFF_HAND).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            model.rightArmPose = player.getMainArm() == HumanoidArm.RIGHT ? main : off;
            model.leftArmPose = player.getMainArm() == HumanoidArm.RIGHT ? off : main;
            boolean riding = player.isPassenger();
            model.attackTime = player.getAttackAnim(1.0F);
            model.riding = riding;
            model.young = player.isBaby();
            float netHeadYaw = Mth.wrapDegrees(player.yHeadRot - player.yBodyRot);
            float limbSwingAmount = riding ? 0.0F : Math.min(1.0F, player.walkAnimation.speed(1.0F));
            float limbSwing = riding ? 0.0F : player.walkAnimation.position(1.0F);
            model.prepareMobModel(player, limbSwing, limbSwingAmount, 1.0F);
            model.setupAnim(player, limbSwing, limbSwingAmount, player.tickCount + 1.0F, netHeadYaw, player.getXRot());
            return snapshot(parts);
        } finally {
            for (int i = 0; i < parts.length; i++) {
                restore[i].apply(parts[i]);
            }
        }
    }

    private static PlayerRenderer playerRenderer(AbstractClientPlayer player) {
        EntityRenderer<? super AbstractClientPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        return renderer instanceof PlayerRenderer playerRenderer ? playerRenderer : null;
    }

    private static ModelPart[] parts(PlayerModel<?> model) {
        return new ModelPart[]{model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg,
                model.leftSleeve, model.rightSleeve, model.leftPants, model.rightPants, model.jacket};
    }

    private static PartPose[] snapshot(ModelPart[] parts) {
        PartPose[] poses = new PartPose[parts.length];
        for (int i = 0; i < parts.length; i++) {
            poses[i] = PartPose.of(parts[i]);
        }
        return poses;
    }
}
