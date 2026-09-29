package dev.apogee.client.model;

import dev.apogee.Apogee;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/**
 * Mantis Blade on one forearm, in arm-local space (origin at the shoulder pivot, +Y towards the
 * hand, which ends at y=10, and -Z out of the forearm's front face).
 *
 * <p>Separate components, all authored in texels (half a model pixel, see
 * {@link MantisMechanismMesh}): the housing clamped to the forearm, the lower and upper links, the
 * blade, whose edge slides out of its housing, and three flaps whose outsides are drawn with the
 * wearer's own skin. The forearm's skin rides on the lower link: folded, it closes over the links
 * and blade lying in a trough on the forearm; deploying, it rises with the link from the wrist
 * towards the elbow while a flap near the elbow lifts and a flap under the wrist drops open over
 * the cables. Then the blade swings forward over the hand and the edge slides out.
 */
public final class MantisArmModel {
    public static final ModelLayerLocation SKIN_LAYER = new ModelLayerLocation(Apogee.id("mantis_arm"), "skin");
    public static final ModelLayerLocation MECHANISM_LAYER = new ModelLayerLocation(Apogee.id("mantis_arm"), "mechanism");

    /** The meshes are authored in texels, half a model pixel each. */
    public static final float MECHANISM_SCALE = 0.5F;

    /**
     * Directions when open, measured from +Y towards -Z (180 degrees points at the elbow): the lower
     * link climbs steeply from the wrist, the upper link bends flatter towards the elbow, and the
     * blade points forward over the hand, rising away from the arm.
     */
    private static final float LOWER_OPEN = (float) Math.toRadians(130.0);
    private static final float UPPER_OPEN = (float) Math.toRadians(152.0);
    private static final float BLADE_OPEN = (float) Math.toRadians(12.0);
    /** How far the elbow flap lifts and the wrist flap underneath drops. */
    private static final float ELBOW_FLAP_OPEN = (float) Math.toRadians(35.0);
    private static final float UNDER_FLAP_OPEN = (float) Math.toRadians(40.0);
    /** How far the edge slides out of the housing, in texels. */
    private static final float EDGE_TRAVEL = 14.0F;
    private static final float EDGE_HOME = 14.0F;

    private final ModelPart[] skinFlaps = new ModelPart[2];
    /** Per arm: the skin on the lower link, the elbow flap and the flap under the wrist. */
    private final ModelPart[][] skinParts = new ModelPart[2][3];
    private final ModelPart housing;
    private final ModelPart elbowFlap;
    private final ModelPart underFlap;
    private final ModelPart lowerLink;
    private final ModelPart upperLink;
    private final ModelPart blade;
    private final ModelPart edge;

    public MantisArmModel(ModelPart skinRoot, ModelPart mechanismRoot) {
        for (int arm = 0; arm < 2; arm++) {
            skinFlaps[arm] = skinRoot.getChild(arm == 0 ? "right_arm" : "left_arm");
            skinParts[arm][0] = skinFlaps[arm].getChild("link_lower");
            skinParts[arm][1] = skinFlaps[arm].getChild("cover_elbow");
            skinParts[arm][2] = skinFlaps[arm].getChild("cover_under");
        }
        housing = mechanismRoot.getChild("housing");
        elbowFlap = housing.getChild("cover_elbow");
        underFlap = housing.getChild("cover_under");
        lowerLink = housing.getChild("link_lower");
        upperLink = lowerLink.getChild("link_upper");
        blade = upperLink.getChild("blade");
        edge = blade.getChild("edge");
    }

    /** Centre of the forearm on the X axis, in model pixels. */
    public static float centerX(boolean right, boolean slim) {
        float minX = right ? (slim ? -2.0F : -3.0F) : -1.0F;
        return minX + (slim ? 1.5F : 2.0F);
    }

    /** Slim arms are three pixels wide instead of four; every component narrows to match. */
    public static float widthScale(boolean slim) {
        return slim ? 0.75F : 1.0F;
    }

    public static LayerDefinition createSkinLayer() {
        MeshDefinition mesh = new MeshDefinition();
        MantisMechanismMesh.buildSkinCovers(mesh.getRoot());
        // Declared at twice the skin's size so one texel reads half a skin pixel.
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition createMechanismLayer() {
        MeshDefinition mesh = new MeshDefinition();
        MantisMechanismMesh.build(mesh.getRoot());
        return LayerDefinition.create(mesh, MantisMechanismMesh.TEXTURE_SIZE, MantisMechanismMesh.TEXTURE_SIZE);
    }

    public ModelPart skinFlaps(boolean right) {
        return skinFlaps[right ? 0 : 1];
    }

    public ModelPart mechanism() {
        return housing;
    }

    /**
     * Poses every component for an unfold progress from 0 (folded) to 1 (open): the links rise
     * with the forearm skin while the flaps open, then the blade swings forward and the edge
     * slides out.
     */
    public void setDeployment(float progress) {
        float rise = Mth.clamp(progress / 0.55F, 0.0F, 1.0F);
        float swing = Mth.clamp((progress - 0.25F) / 0.55F, 0.0F, 1.0F);
        float slide = Mth.clamp((progress - 0.55F) / 0.45F, 0.0F, 1.0F);

        // Direction angles are measured from +Y towards -Z; a positive xRot turns +Y towards +Z.
        float lower = Mth.lerp(rise, Mth.PI, LOWER_OPEN);
        float upper = Mth.lerp(rise, Mth.PI, UPPER_OPEN);
        lowerLink.xRot = -lower;
        upperLink.xRot = lower - upper;
        blade.xRot = upper - swing * BLADE_OPEN;
        edge.visible = slide > 0.0F;
        edge.y = EDGE_HOME - EDGE_TRAVEL * (1.0F - slide);

        // The elbow flap lifts its wrist-side edge; the flap under the wrist drops its far edge.
        elbowFlap.xRot = -rise * ELBOW_FLAP_OPEN;
        underFlap.xRot = rise * UNDER_FLAP_OPEN;
        for (ModelPart[] parts : skinParts) {
            parts[0].xRot = lowerLink.xRot;
            parts[1].xRot = elbowFlap.xRot;
            parts[2].xRot = underFlap.xRot;
        }
    }
}
