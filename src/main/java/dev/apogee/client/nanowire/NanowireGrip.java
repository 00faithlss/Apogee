package dev.apogee.client.nanowire;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.PlayerModelPart;

final class NanowireGrip {
    private static final ModelPart RIGHT = thumb(true, false);
    private static final ModelPart LEFT = thumb(false, false);
    private static final ModelPart RIGHT_LAYER = thumb(true, true);
    private static final ModelPart LEFT_LAYER = thumb(false, true);

    private NanowireGrip() {
    }

    private static ModelPart thumb(boolean right, boolean layer) {
        MeshDefinition mesh = new MeshDefinition();
        int u = right ? 42 : 34;
        int v = right ? 26 : 58;
        if (layer) {
            if (right) {
                v += 16;
            } else {
                u += 16;
            }
        }
        mesh.getRoot().addOrReplaceChild("thumb", CubeListBuilder.create()
                .texOffs(u, v)
                .addBox(right ? 0.9F : -2.2F, 7.1F, -1.45F, 1.3F, 2.1F, 1.6F,
                        new CubeDeformation(layer ? 0.04F : 0.0F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("thumb");
    }

    static void render(PoseStack poses, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       boolean right, float deploy) {
        if (deploy <= 0.0F) {
            return;
        }
        poses.pushPose();
        poses.translate((right ? -1 : 1) * (1.0F - deploy) * 1.25F / 16.0F, 0.0F, 0.0F);
        (right ? RIGHT : LEFT).render(poses, buffers.getBuffer(RenderType.entitySolid(player.getSkin().texture())),
                light, OverlayTexture.NO_OVERLAY);
        if (player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE)) {
            (right ? RIGHT_LAYER : LEFT_LAYER).render(poses,
                    buffers.getBuffer(RenderType.entityTranslucent(player.getSkin().texture())), light, OverlayTexture.NO_OVERLAY);
        }
        poses.popPose();
    }
}
