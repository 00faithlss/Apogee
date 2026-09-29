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
 * <p>Two layers: a skin plate per arm variant (right/left, wide/slim) that copies the wearer's own
 * forearm pixels, and the steel mechanism, built at twice the resolution (see
 * {@link MantisMechanismMesh}) and shared by every arm. Folded, the mechanism lies flat on the
 * forearm; deployed, the support rises from the wrist towards the elbow, the blade swings forward
 * over the hand and its edge slides out of the housing.
 */
public final class MantisArmModel {
    public static final ModelLayerLocation PLATE_LAYER = new ModelLayerLocation(Apogee.id("mantis_arm"), "plate");
    public static final ModelLayerLocation MECHANISM_LAYER = new ModelLayerLocation(Apogee.id("mantis_arm"), "mechanism");

    /** The mechanism mesh is authored in texels, half a model pixel each. */
    public static final float MECHANISM_SCALE = 0.5F;

    /** Direction of the support when open, measured from +Y towards -Z (180 degrees points at the elbow). */
    private static final float SUPPORT_OPEN = (float) Math.toRadians(140.0);
    /** Direction of the blade when open: forward over the hand, rising away from the arm. */
    private static final float BLADE_OPEN = (float) Math.toRadians(15.0);
    /** How far the edge slides out of the housing, in texels. */
    private static final float EDGE_TRAVEL = 14.0F;
    private static final float EDGE_HOME = 15.0F;

    private final ModelPart[] plates = new ModelPart[4];
    private final ModelPart mechanism;
    private final ModelPart support;
    private final ModelPart blade;
    private final ModelPart edge;

    public MantisArmModel(ModelPart plateRoot, ModelPart mechanismRoot) {
        for (int i = 0; i < 4; i++) {
            plates[i] = plateRoot.getChild("plate_" + key(i));
        }
        mechanism = mechanismRoot.getChild("mech");
        support = mechanism.getChild("support");
        blade = support.getChild("blade");
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
            // A depth-1 cube reads its front face at (u+1, v+1), so offset by one to land on it,
            // then skip the five rows of the upper arm.
            int u = (right ? 44 : 36) - 1;
            int v = (right ? 20 : 52) + 5 - 1;
            root.addOrReplaceChild("plate_" + key(i),
                    CubeListBuilder.create().texOffs(u, v).addBox(minX(right, slim), 3.0F, -3.0F, width(slim), 5.0F, 1.0F),
                    PartPose.ZERO);
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
     * Poses the mechanism for an unfold progress from 0 (flat on the forearm) to 1 (open). The
     * support rises first, the blade swings forward over it, and the edge slides out last.
     */
    public void setDeployment(float progress) {
        float rise = Mth.clamp(progress / 0.6F, 0.0F, 1.0F);
        float swing = Mth.clamp((progress - 0.2F) / 0.6F, 0.0F, 1.0F);
        float slide = Mth.clamp((progress - 0.5F) / 0.5F, 0.0F, 1.0F);
        // Direction angles are measured from +Y towards -Z; a positive xRot turns +Y towards +Z.
        float supportAngle = Mth.lerp(rise, Mth.PI, SUPPORT_OPEN);
        float bladeAngle = swing * BLADE_OPEN;
        support.xRot = -supportAngle;
        blade.xRot = supportAngle - bladeAngle;
        edge.visible = slide > 0.0F;
        edge.y = EDGE_HOME - EDGE_TRAVEL * (1.0F - slide);
    }
}
