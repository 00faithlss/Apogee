package dev.apogee.client.model;

import dev.apogee.Apogee;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Mantis Blade mechanism for one forearm, in arm-local model pixels (origin at the shoulder
 * pivot, +Y towards the hand, -Z out of the back of the forearm).
 *
 * <p>Each variant (right/left arm, wide/slim skin) has a skin-textured plate that copies the
 * wearer's own forearm pixels, and a steel mechanism that folds out into a Z: a rail on the
 * forearm, a segment hinged at the wrist that swings back towards the elbow, and a blade hinged
 * at its tip that points forward again over the hand.
 */
public final class MantisArmModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Apogee.id("mantis_arm"), "main");

    /*
     * Proportions read off the official arm renders: a stretched, shallow Z that hugs the arm.
     * The support leaves the rail at the wrist and rises only about 21 degrees towards the elbow;
     * the blade starts over the forearm, climbs about 15 degrees and ends a little past the hand.
     */
    private static final float WRIST_Y = 10.0F;
    private static final float HINGE_Z = -3.6F;
    private static final float SEGMENT_LENGTH = 3.2F;
    private static final float BLADE_LENGTH = 9.6F;
    private static final float BLADE_TIP_LENGTH = 1.6F;
    /** Angle of the support off the forearm axis when open, and of the blade past parallel. */
    private static final float OPEN_SEGMENT = (float) Math.toRadians(21.0);
    private static final float OPEN_BLADE = (float) Math.toRadians(15.0);

    private final ModelPart[] plates = new ModelPart[4];
    private final ModelPart[] mechanisms = new ModelPart[4];
    private final ModelPart[] segments = new ModelPart[4];
    private final ModelPart[] blades = new ModelPart[4];

    public MantisArmModel(ModelPart root) {
        for (int i = 0; i < 4; i++) {
            String key = key(i);
            plates[i] = root.getChild("plate_" + key);
            mechanisms[i] = root.getChild("mech_" + key);
            segments[i] = mechanisms[i].getChild("segment");
            blades[i] = segments[i].getChild("blade");
        }
    }

    private static int index(boolean right, boolean slim) {
        return (right ? 0 : 2) + (slim ? 1 : 0);
    }

    private static String key(int index) {
        return (index < 2 ? "right" : "left") + (index % 2 == 1 ? "_slim" : "_wide");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < 4; i++) {
            boolean right = i < 2;
            boolean slim = i % 2 == 1;
            float width = slim ? 3.0F : 4.0F;
            float minX = right ? (slim ? -2.0F : -3.0F) : -1.0F;
            float centerX = minX + width / 2.0F;
            // Forearm pixels of the standard 64x64 skin: right arm at (40,16), left arm at (32,48).
            int u = right ? 40 : 32;
            int v = right ? 16 : 48;
            String key = key(i);

            // A one pixel plate over the lower forearm. With depth 1 the front face reads the
            // arm's own front face, so the plate takes the colours of the wearer's skin.
            root.addOrReplaceChild("plate_" + key,
                    CubeListBuilder.create().texOffs(u + 3, v + 8).addBox(minX, 5.0F, -3.0F, width, 6.0F, 1.0F),
                    PartPose.ZERO);

            PartDefinition mech = root.addOrReplaceChild("mech_" + key,
                    CubeListBuilder.create()
                            // rail along the forearm
                            .texOffs(0, 0).addBox(centerX - 0.75F, 3.0F, -3.6F, 1.5F, 7.5F, 0.6F)
                            // exposed muscle on both sides of the plate
                            .texOffs(32, 0).addBox(minX - 0.5F, 5.0F, -3.0F, 0.5F, 6.0F, 1.0F)
                            .texOffs(32, 0).addBox(minX + width, 5.0F, -3.0F, 0.5F, 6.0F, 1.0F),
                    PartPose.ZERO);

            PartDefinition segment = mech.addOrReplaceChild("segment",
                    CubeListBuilder.create()
                            .texOffs(0, 32).addBox(-1.0F, -0.8F, -0.8F, 2.0F, 1.6F, 1.6F)
                            .texOffs(0, 0).addBox(-0.8F, 0.0F, -0.4F, 1.6F, SEGMENT_LENGTH, 0.8F),
                    PartPose.offset(centerX, WRIST_Y, HINGE_Z));

            segment.addOrReplaceChild("blade",
                    CubeListBuilder.create()
                            .texOffs(0, 32).addBox(-1.0F, -0.9F, -0.9F, 2.0F, 1.8F, 1.8F)
                            .texOffs(0, 16).addBox(-0.3F, 0.0F, -0.9F, 0.6F, BLADE_LENGTH, 1.5F)
                            .texOffs(0, 16).addBox(-0.3F, BLADE_LENGTH, -0.5F, 0.6F, BLADE_TIP_LENGTH, 0.9F),
                    PartPose.offset(0.0F, SEGMENT_LENGTH, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    public ModelPart plate(boolean right, boolean slim) {
        return plates[index(right, slim)];
    }

    public ModelPart mechanism(boolean right, boolean slim) {
        return mechanisms[index(right, slim)];
    }

    /**
     * Folds or unfolds the mechanism. The segment opens first and the blade follows; both angles
     * are measured in the YZ plane, so a positive X rotation turns +Y towards +Z.
     */
    public void setDeployment(boolean right, boolean slim, float progress) {
        int i = index(right, slim);
        float segmentOpen = Mth.clamp(progress / 0.6F, 0.0F, 1.0F);
        float bladeOpen = Mth.clamp((progress - 0.3F) / 0.7F, 0.0F, 1.0F);
        // Direction angles measured from +Y towards -Z: 180 degrees points at the elbow.
        float segmentAngle = Mth.PI - segmentOpen * OPEN_SEGMENT;
        float bladeAngle = bladeOpen * OPEN_BLADE;
        segments[i].xRot = -segmentAngle;
        blades[i].xRot = segmentAngle - bladeAngle;
    }
}
