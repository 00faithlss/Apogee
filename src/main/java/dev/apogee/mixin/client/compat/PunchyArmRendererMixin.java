package dev.apogee.mixin.client.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.compat.punchy.PunchyCompat;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.client.mantis.MantisRig;
import dev.apogee.client.nanowire.NanowireRig;
import dev.apogee.client.nanowire.NanowireHands;
import dev.apogee.client.nanowire.NanowireWire;
import net.minecraft.client.Minecraft;
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

    // Punchy 2.8a applies this AFTER applyGeoArmToPlayer. An empty offhand normally gets
    // rotated 90 degrees down, even though our nanowire needs that arm in its native guard.
    @Inject(method = "applyLoweredOffhandPose", at = @At("HEAD"), cancellable = true, require = 0)
    private static void apogee$keepNanowireOffhand(LocalPlayer player, PlayerModel<AbstractClientPlayer> model,
                                                   HumanoidArm arm, CallbackInfo ci) {
        if (PunchyCompat.nativeNanowireView(player)) {
            ci.cancel();
        }
    }

    @Inject(method = {"renderArmArmor", "renderItemInHand"}, at = @At("HEAD"), cancellable = true)
    private static void apogee$hideItemsAndArmor(CallbackInfo ci) {
        if (PunchyCompat.rendered() || PunchyCompat.nativeNanowireView(Minecraft.getInstance().player)) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "renderFirstPersonInternal", at = @At(value = "INVOKE",
            target = "Lpunchy/client/render/PunchyArmRenderer;renderArm"))
    private static void apogee$nativeNanowireHands(PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player,
                                                   HumanoidArm arm, PoseStack poses, MultiBufferSource buffers, int light,
                                                   ResourceLocation texture, boolean slim, float partialTick,
                                                   Operation<Void> original) {
        if (!(player instanceof LocalPlayer local) || !PunchyCompat.nativeNanowireView(local)) {
            original.call(model, player, arm, poses, buffers, light, texture, slim, partialTick);
            return;
        }
        if (!PunchyCompat.claimNanowireArms(local)) {
            return;
        }
        // Keep Punchy's skin/sleeve renderer and BOTH of its fully animated model parts.
        // Only the attacks and the offhand's visibility transition belong to the implant.
        NanowireWire.Frame wireFrame = NanowireWire.Frame.of(poses.last(), true);
        float deploy = MantisClient.deployProgress(local, partialTick);
        for (HumanoidArm nativeArm : new HumanoidArm[]{HumanoidArm.RIGHT, HumanoidArm.LEFT}) {
            boolean right = nativeArm == HumanoidArm.RIGHT;
            ModelPart part = right ? model.rightArm : model.leftArm;
            ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
            boolean visible = part.visible;
            boolean sleeveVisible = sleeve.visible;
            poses.pushPose();
            try {
                // Punchy's outer pose is model space (+Y down). Apply implant motions in
                // view space, then return to the native space before it draws the mesh.
                poses.mulPose(Axis.ZP.rotationDegrees(180.0F));
                poses.translate(0.0F, -MantisClient.HANDS_DROP * (1.0F - MantisClient.implantLift(local, partialTick)), 0.0F);
                if (nativeArm != local.getMainArm() && !visible) {
                    poses.translate(0.0F, -0.9F * (1.0F - deploy), 0.0F);
                }
                NanowireHands.applyAttack(poses, local, right, partialTick);
                poses.mulPose(Axis.ZP.rotationDegrees(180.0F));
                part.visible = true;
                sleeve.visible = local.isModelPartShown(right
                        ? net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE
                        : net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE);
                original.call(model, player, nativeArm, poses, buffers, light, texture, slim, partialTick);
            } finally {
                part.visible = visible;
                sleeve.visible = sleeveVisible;
                poses.popPose();
            }
        }
        NanowireRig.drawWires(buffers, local, wireFrame, partialTick);
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
        if (PunchyCompat.finalProbe != null && player instanceof LocalPlayer local
                && PunchyCompat.nativeNanowireView(local)) {
            boolean left = arm == HumanoidArm.LEFT;
            PoseStack sampled = new PoseStack();
            sampled.mulPose(poses.last().pose());
            if (slim) {
                sampled.translate((left ? 1.0F : -1.0F) * 0.5F / 16.0F, 0.0F, 0.0F);
            }
            applyArmMeshOffsets(sampled, left);
            applyFreezeShake(sampled, player, partialTick);
            (left ? model.leftArm : model.rightArm).translateAndRotate(sampled);
            PunchyCompat.finalProbe.accept(left, new Matrix4f(sampled.last().pose()));
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
