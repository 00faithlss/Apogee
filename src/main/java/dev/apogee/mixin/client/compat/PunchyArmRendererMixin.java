package dev.apogee.mixin.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.compat.punchy.PunchyCompat;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.mantis.MantisRig;
import dev.apogee.client.nanowire.NanowireRig;
import dev.apogee.mantis.MantisVariant;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "punchy.client.render.PunchyArmRenderer", remap = false)
public abstract class PunchyArmRendererMixin {
    @Shadow
    private static void applyArmMeshOffsets(PoseStack poses, boolean left) {
        throw new AssertionError();
    }

    @Shadow
    private static void applyFreezeShake(PoseStack poses, AbstractClientPlayer player, float partialTick) {
        throw new AssertionError();
    }

    @Inject(method = "renderFirstPerson", at = @At("HEAD"))
    private static void apogee$begin(ItemInHandRenderer renderer, LocalPlayer player, float partialTick,
                                     PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo ci) {
        PunchyCompat.begin(player, poses, buffers, partialTick, light);
    }

    @Inject(method = "renderFirstPerson", at = @At("RETURN"))
    private static void apogee$end(ItemInHandRenderer renderer, LocalPlayer player, float partialTick,
                                   PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo ci) {
        PunchyCompat.reset();
    }

    @Inject(method = "applyGeoArmToPlayer", at = @At("TAIL"))
    private static void apogee$pose(ModelPart part, ModelPart geoArm, Matrix4f root, LocalPlayer player,
                                    HumanoidArm arm, float partialTick, CallbackInfo ci) {
        if (geoArm == null || !PunchyCompat.needsPose(player)) {
            return;
        }
        boolean left = arm == HumanoidArm.LEFT;
        PoseStack sampled = new PoseStack();
        sampled.mulPose(Axis.ZP.rotationDegrees(180.0F));
        if (player.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM) {
            sampled.translate((left ? 1.0F : -1.0F) * 0.5F / 16.0F, 0.0F, 0.0F);
        }
        applyArmMeshOffsets(sampled, left);
        applyFreezeShake(sampled, player, partialTick);
        part.translateAndRotate(sampled);
        PunchyCompat.capture(player, arm, sampled.last().pose(), part.visible);
    }

    @Inject(method = {"renderArmArmor", "renderItemInHand"}, at = @At("HEAD"), cancellable = true)
    private static void apogee$hideItemsAndArmor(CallbackInfo ci) {
        if (PunchyCompat.rendered()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArm", at = @At("HEAD"), cancellable = true)
    private static void apogee$mantisArm(PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player,
                                         HumanoidArm arm, PoseStack poses, MultiBufferSource buffers, int light,
                                         ResourceLocation texture, boolean slim, float partialTick, CallbackInfo ci) {
        if (PunchyCompat.probe != null) {
            boolean probeLeft = arm == HumanoidArm.LEFT;
            PoseStack sampled = new PoseStack();
            if (slim) {
                sampled.translate((probeLeft ? 1.0F : -1.0F) * 0.5F / 16.0F, 0.0F, 0.0F);
            }
            applyArmMeshOffsets(sampled, probeLeft);
            applyFreezeShake(sampled, player, partialTick);
            (probeLeft ? model.leftArm : model.rightArm).translateAndRotate(sampled);
            PunchyCompat.probe.accept(probeLeft, new Matrix4f(sampled.last().pose()));
        }
        if (PunchyCompat.rendered()) {
            ci.cancel();
            return;
        }
        MantisVariant variant = MantisClient.variant(player);
        if (variant == null || !variant.isBlades()) {
            return;
        }
        boolean left = arm == HumanoidArm.LEFT;
        poses.pushPose();
        if (slim) {
            poses.translate((left ? 1.0F : -1.0F) * 0.5F / 16.0F, 0.0F, 0.0F);
        }
        applyArmMeshOffsets(poses, left);
        applyFreezeShake(poses, player, partialTick);
        MantisRig.firstPersonPass = true;
        try {
            MantisRig.drawOnArm(poses, buffers, light, player, left ? model.leftArm : model.rightArm, !left, partialTick);
        } finally {
            MantisRig.firstPersonPass = false;
            poses.popPose();
        }
        ci.cancel();
    }

    @Inject(method = "renderArm", at = @At("TAIL"))
    private static void apogee$nanowireArm(PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player,
                                           HumanoidArm arm, PoseStack poses, MultiBufferSource buffers, int light,
                                           ResourceLocation texture, boolean slim, float partialTick, CallbackInfo ci) {
        if (PunchyCompat.rendered() || (MantisClient.variant(player) == null || !MantisClient.variant(player).isNanowire())) {
            return;
        }
        boolean left = arm == HumanoidArm.LEFT;
        poses.pushPose();
        try {
            if (slim) {
                poses.translate((left ? 1.0F : -1.0F) * 0.5F / 16.0F, 0.0F, 0.0F);
            }
            applyArmMeshOffsets(poses, left);
            applyFreezeShake(poses, player, partialTick);
            NanowireRig.drawOnArm(poses, buffers, light, player, left ? model.leftArm : model.rightArm, !left, partialTick);
        } finally {
            poses.popPose();
        }
    }
}
