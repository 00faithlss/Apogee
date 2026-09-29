package dev.apogee.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.apogee.client.mantis.MantisClient;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Third-person Mantis Blades on both forearms of any player who has them out. */
public final class MantisArmsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public MantisArmsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        float progress = MantisClient.deployProgress(player, partialTick);
        if (progress <= 0.0F || player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        poseStack.pushPose();
        model.rightArm.translateAndRotate(poseStack);
        MantisArmRenderer.render(poseStack, buffers, light, player, true, progress);
        poseStack.popPose();
        poseStack.pushPose();
        model.leftArm.translateAndRotate(poseStack);
        MantisArmRenderer.render(poseStack, buffers, light, player, false, progress);
        poseStack.popPose();
    }
}
