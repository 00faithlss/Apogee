package dev.apogee.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.apogee.client.mantis.MantisRig;
import dev.apogee.client.nanowire.NanowireRig;
import dev.apogee.client.nanowire.NanowireWire;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Third-person arm cyberware. The Mantis Blades model replaces the whole arm (its closed skin
 * panels, hand and shoulder cap rebuild it), so the vanilla arms are hidden while it is worn;
 * the nanowire housings ride on the player's own arms.
 */
public final class MantisArmsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public MantisArmsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        MantisRig.drawOnArm(poseStack, buffers, light, player, model.rightArm, true, partialTick, true);
        MantisRig.drawOnArm(poseStack, buffers, light, player, model.leftArm, false, partialTick, true);
        // The nanowire keeps the player's own arms and rides on them.
        NanowireRig.drawOnArm(poseStack, buffers, light, player, model.rightArm, true, partialTick);
        NanowireRig.drawOnArm(poseStack, buffers, light, player, model.leftArm, false, partialTick);
        NanowireRig.drawWires(buffers, player, NanowireWire.Frame.of(poseStack.last(), true), partialTick);
    }
}
