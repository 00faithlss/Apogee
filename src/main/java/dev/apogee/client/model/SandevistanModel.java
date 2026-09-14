package dev.apogee.client.model;

import dev.apogee.Apogee;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Spine implant geometry adapted from Sandevistan (Cyperpunk) 1.2.0 by Samuelchr. Cube sizes,
 * positions and 128x128 UV layout are kept identical; only the loader wiring changed. The same
 * baked parts are used on the player, in item form and inside the tooltip.
 */
public class SandevistanModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Apogee.id("sandevistan"), "main");

    /** Scale the original applies to the body part so the spine fits a player's back. */
    public static final float SPINE_X_SCALE = 0.38F;
    public static final float SPINE_Y_SCALE = 0.64F;
    public static final float SPINE_Z_SCALE = 0.55F;
    /** Extra push away from the back, in model pixels, so it sits over chestplates. */
    public static final float BACK_OFFSET = 0.65F;
    /** Geometric centre of the spine in model pixels, used to frame it in items and tooltips. */
    public static final float CENTER_Y = 6.75F;
    public static final float CENTER_Z = 3.65F;

    public SandevistanModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

        body.addOrReplaceChild("spine_core", CubeListBuilder.create().texOffs(0, 64).addBox(-0.7F, -3.0F, 2.35F, 1.4F, 18.6F, 0.9F), PartPose.ZERO);
        body.addOrReplaceChild("inner_cable", CubeListBuilder.create().texOffs(0, 96).addBox(-0.32F, -2.6F, 3.2F, 0.64F, 18.0F, 0.6F), PartPose.ZERO);
        body.addOrReplaceChild("neck_anchor", CubeListBuilder.create().texOffs(0, 64).addBox(-2.7F, -4.7F, 3.05F, 5.4F, 2.6F, 1.15F), PartPose.ZERO);
        body.addOrReplaceChild("upper_round_link", CubeListBuilder.create().texOffs(96, 0).addBox(-1.15F, -2.15F, 4.05F, 2.3F, 2.45F, 0.75F), PartPose.ZERO);
        body.addOrReplaceChild("upper_round_slot", CubeListBuilder.create().texOffs(0, 96).addBox(-0.18F, -1.95F, 4.82F, 0.36F, 2.05F, 0.14F), PartPose.ZERO);
        addEnergyModule(body, "upper", -0.2F);
        addEnergyModule(body, "middle", 4.65F);
        addEnergyModule(body, "lower", 9.5F);
        body.addOrReplaceChild("lower_tail", CubeListBuilder.create().texOffs(0, 64).addBox(-1.7F, 14.1F, 2.55F, 3.4F, 3.2F, 0.95F), PartPose.ZERO);
        body.addOrReplaceChild("tail_green_window", CubeListBuilder.create().texOffs(64, 0).addBox(-1.15F, 15.55F, 3.48F, 2.3F, 0.48F, 0.28F), PartPose.ZERO);
        body.addOrReplaceChild("tail_socket", CubeListBuilder.create().texOffs(96, 0).addBox(-0.55F, 17.15F, 3.0F, 1.1F, 1.05F, 0.6F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addEnergyModule(PartDefinition body, String name, float y) {
        body.addOrReplaceChild(name + "_shadow_plate", CubeListBuilder.create().texOffs(0, 64).addBox(-3.15F, y + 0.25F, 2.55F, 6.3F, 3.65F, 0.68F), PartPose.ZERO);
        body.addOrReplaceChild(name + "_main_shell", CubeListBuilder.create().texOffs(0, 0).addBox(-2.42F, y, 3.05F, 4.84F, 3.72F, 0.9F), PartPose.ZERO);
        body.addOrReplaceChild(name + "_raised_center", CubeListBuilder.create().texOffs(32, 0).addBox(-1.45F, y + 0.35F, 3.92F, 2.9F, 2.05F, 0.52F), PartPose.ZERO);
        body.addOrReplaceChild(name + "_left_bevel", CubeListBuilder.create().texOffs(0, 64).addBox(-2.78F, y + 0.52F, 3.88F, 0.72F, 2.55F, 0.48F), PartPose.ZERO);
        body.addOrReplaceChild(name + "_right_bevel", CubeListBuilder.create().texOffs(0, 64).addBox(2.06F, y + 0.52F, 3.88F, 0.72F, 2.55F, 0.48F), PartPose.ZERO);
        body.addOrReplaceChild(name + "_green_cell", CubeListBuilder.create().texOffs(64, 0).addBox(-1.75F, y + 2.52F, 4.46F, 3.5F, 0.72F, 0.34F), PartPose.ZERO);
        body.addOrReplaceChild(name + "_left_bracket", CubeListBuilder.create().texOffs(0, 96).addBox(-5.2F, y + 1.28F, 2.48F, 2.32F, 1.18F, 0.72F), PartPose.ZERO);
        body.addOrReplaceChild(name + "_right_bracket", CubeListBuilder.create().texOffs(0, 96).addBox(2.88F, y + 1.28F, 2.48F, 2.32F, 1.18F, 0.72F), PartPose.ZERO);
        body.addOrReplaceChild(name + "_left_screw", CubeListBuilder.create().texOffs(96, 0).addBox(-4.65F, y + 1.08F, 3.16F, 1.12F, 1.12F, 0.48F), PartPose.ZERO);
        body.addOrReplaceChild(name + "_right_screw", CubeListBuilder.create().texOffs(96, 0).addBox(3.53F, y + 1.08F, 3.16F, 1.12F, 1.12F, 0.48F), PartPose.ZERO);
        body.addOrReplaceChild(name + "_left_screw_slot", CubeListBuilder.create().texOffs(0, 96).addBox(-4.38F, y + 1.52F, 3.65F, 0.6F, 0.18F, 0.12F), PartPose.ZERO);
        body.addOrReplaceChild(name + "_right_screw_slot", CubeListBuilder.create().texOffs(0, 96).addBox(3.8F, y + 1.52F, 3.65F, 0.6F, 0.18F, 0.12F), PartPose.ZERO);
    }
}
