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
 * Mantis Blade on one forearm, in arm-local space (origin at the shoulder pivot, +Y towards the
 * hand, which ends at y=10, and -Z out of the forearm's front face).
 *
 * <p>Two layers. The skin layer, one per arm variant (right/left, wide/slim), copies the wearer's
 * own forearm pixels: a plate under the housing and two wrist covers that swing open to the sides
 * to let the mechanism out. The steel mechanism is built at twice the resolution (see
 * {@link MantisMechanismMesh}) and shared by every arm. Folded, it lies flat on the forearm;
 * deploying, the wrist covers open, the two links rise from the wrist towards the elbow, the blade
 * swings forward over the hand and its edge slides out of the housing.
 */
public final class MantisArmModel {
    public static final ModelLayerLocation PLATE_LAYER = new ModelLayerLocation(Apogee.id("mantis_arm"), "plate");
    public static final ModelLayerLocation MECHANISM_LAYER = new ModelLayerLocation(Apogee.id("mantis_arm"), "mechanism");

    /** The mechanism mesh is authored in texels, half a model pixel each. */
    public static final float MECHANISM_SCALE = 0.5F;

    /**
     * Directions when open, measured from +Y towards -Z (180 degrees points at the elbow): the lower
     * link climbs steeply from the wrist, the upper link bends flatter towards the elbow, and the
     * blade points forward over the hand, rising away from the arm.
     */
    private static final float LOWER_OPEN = (float) Math.toRadians(130.0);
    private static final float UPPER_OPEN = (float) Math.toRadians(152.0);
    private static final float BLADE_OPEN = (float) Math.toRadians(12.0);
    /** How far each wrist cover swings out to its side. */
    private static final float COVER_OPEN = (float) Math.toRadians(115.0);
    /** How far the edge slides out of the housing, in texels. */
    private static final float EDGE_TRAVEL = 14.0F;
    private static final float EDGE_HOME = 15.0F;

    private final ModelPart[] plates = new ModelPart[4];
    private final ModelPart[] leftCovers = new ModelPart[4];
    private final ModelPart[] rightCovers = new ModelPart[4];
    private final ModelPart mechanism;
    private final ModelPart lowerLink;
    private final ModelPart upperLink;
    private final ModelPart blade;
    private final ModelPart edge;

    public MantisArmModel(ModelPart plateRoot, ModelPart mechanismRoot) {
        for (int i = 0; i < 4; i++) {
            plates[i] = plateRoot.getChild("plate_" + key(i));
            leftCovers[i] = plates[i].getChild("cover_left");
            rightCovers[i] = plates[i].getChild("cover_right");
        }
        mechanism = mechanismRoot.getChild("mech");
        lowerLink = mechanism.getChild("link_lower");
        upperLink = lowerLink.getChild("link_upper");
        blade = upperLink.getChild("blade");
        edge = blade.getChild("edge");
    }

    private static int index(boolean right, boolean slim) {
        return (right ? 0 : 2) + (slim ? 1 : 0);
    }

    private static String key(int index) {
        return (index < 2 ? "right" : "left") + (index % 2 == 1 ? "_slim" : "_wide");
    }

    private static float minX(boolean right, boolean slim) {
        return right ? (slim ? -2.0F : -3.0F) : -1.0F;
    }

    private static float width(boolean slim) {
        return slim ? 3.0F : 4.0F;
    }

    /** Centre of the forearm on the X axis, in model pixels. */
    public static float centerX(boolean right, boolean slim) {
        return minX(right, slim) + width(slim) / 2.0F;
    }

    /** Slim arms are three pixels wide instead of four; the mechanism narrows to match. */
    public static float widthScale(boolean slim) {
        return slim ? 0.75F : 1.0F;
    }

    public static LayerDefinition createPlateLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < 4; i++) {
            boolean right = i < 2;
            boolean slim = i % 2 == 1;
            // Front face of the forearm in the 64x64 skin: right arm box at (40,16), left at (32,48).
            // A cube of depth d reads its front face at (u+d, v+d); offset by d to land on the arm's
            // own pixels, then skip the rows above the cube's top (the arm starts at y=-2).
            int faceU = right ? 44 : 36;
            int faceV = right ? 20 : 52;
            float minX = minX(right, slim);
            float width = width(slim);
            float half = width / 2.0F;
            PartDefinition plate = root.addOrReplaceChild("plate_" + key(i),
                    CubeListBuilder.create().texOffs(faceU - 1, faceV + 5 - 1).addBox(minX, 3.0F, -3.0F, width, 4.0F, 1.0F),
                    PartPose.ZERO);
            // Wrist covers, two pixels thick, hinged on their outer bottom edges.
            plate.addOrReplaceChild("cover_left",
                    CubeListBuilder.create().texOffs(faceU - 2, faceV + 9 - 2).addBox(0.0F, 0.0F, -2.0F, half, 2.0F, 2.0F),
                    PartPose.offset(minX, 7.0F, -2.0F));
            plate.addOrReplaceChild("cover_right",
                    CubeListBuilder.create().texOffs(faceU - 2 + (int) half, faceV + 9 - 2).addBox(-half, 0.0F, -2.0F, half, 2.0F, 2.0F),
                    PartPose.offset(minX + width, 7.0F, -2.0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createMechanismLayer() {
        MeshDefinition mesh = new MeshDefinition();
        MantisMechanismMesh.build(mesh.getRoot());
        return LayerDefinition.create(mesh, MantisMechanismMesh.TEXTURE_SIZE, MantisMechanismMesh.TEXTURE_SIZE);
    }

    public ModelPart plate(boolean right, boolean slim) {
        return plates[index(right, slim)];
    }

    public ModelPart mechanism() {
        return mechanism;
    }

    /**
     * Poses the arm for an unfold progress from 0 (folded flat on the forearm) to 1 (open). The
     * wrist covers open first, the links rise, the blade swings forward and the edge slides out.
     */
    public void setDeployment(boolean right, boolean slim, float progress) {
        float open = Mth.clamp(progress / 0.3F, 0.0F, 1.0F);
        float rise = Mth.clamp((progress - 0.2F) / 0.5F, 0.0F, 1.0F);
        float swing = Mth.clamp((progress - 0.35F) / 0.5F, 0.0F, 1.0F);
        float slide = Mth.clamp((progress - 0.6F) / 0.4F, 0.0F, 1.0F);
        int i = index(right, slim);
        // The left cover lies on the +X side of its hinge: a positive yRot lifts it up and out to -X.
        leftCovers[i].yRot = open * COVER_OPEN;
        rightCovers[i].yRot = -open * COVER_OPEN;
        // Direction angles are measured from +Y towards -Z; a positive xRot turns +Y towards +Z.
        float lower = Mth.lerp(rise, Mth.PI, LOWER_OPEN);
        float upper = Mth.lerp(rise, Mth.PI, UPPER_OPEN);
        float bladeAngle = swing * BLADE_OPEN;
        lowerLink.xRot = -lower;
        upperLink.xRot = lower - upper;
        blade.xRot = upper - bladeAngle;
        edge.visible = slide > 0.0F;
        edge.y = EDGE_HOME - EDGE_TRAVEL * (1.0F - slide);
    }
}
