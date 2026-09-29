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
 * {@link MantisMechanismMesh}): the housing clamped to the forearm, two covers whose tops are
 * drawn with the wearer's own skin, the lower and upper links and the blade, whose edge slides out
 * of its housing. Folded, the links and blade lie in a trough on the forearm under the closed
 * covers. Deploying, the covers swing open to the sides, the links rise from the wrist towards
 * the elbow, the blade swings forward over the hand and the edge slides out.
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
    /** How far each cover swings up and out to its side. */
    private static final float COVER_OPEN = (float) Math.toRadians(145.0);
    /** How far the edge slides out of the housing, in texels. */
    private static final float EDGE_TRAVEL = 14.0F;
    private static final float EDGE_HOME = 14.0F;

    private final ModelPart[] skinCovers = new ModelPart[2];
    private final ModelPart[][] skinCoverHalves = new ModelPart[2][2];
    private final ModelPart housing;
    private final ModelPart[] coverLinings = new ModelPart[2];
    private final ModelPart lowerLink;
    private final ModelPart upperLink;
    private final ModelPart blade;
    private final ModelPart edge;

    public MantisArmModel(ModelPart skinRoot, ModelPart mechanismRoot) {
        for (int arm = 0; arm < 2; arm++) {
            skinCovers[arm] = skinRoot.getChild(arm == 0 ? "right_arm" : "left_arm");
            skinCoverHalves[arm][0] = skinCovers[arm].getChild("cover_left");
            skinCoverHalves[arm][1] = skinCovers[arm].getChild("cover_right");
        }
        housing = mechanismRoot.getChild("housing");
        coverLinings[0] = housing.getChild("cover_left");
        coverLinings[1] = housing.getChild("cover_right");
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

    public ModelPart skinCovers(boolean right) {
        return skinCovers[right ? 0 : 1];
    }

    public ModelPart mechanism() {
        return housing;
    }

    /**
     * Poses every component for an unfold progress from 0 (folded) to 1 (open): the covers open
     * first, then the links rise, the blade swings forward and the edge slides out.
     */
    public void setDeployment(float progress) {
        float open = Mth.clamp(progress / 0.3F, 0.0F, 1.0F);
        float rise = Mth.clamp((progress - 0.2F) / 0.5F, 0.0F, 1.0F);
        float swing = Mth.clamp((progress - 0.35F) / 0.5F, 0.0F, 1.0F);
        float slide = Mth.clamp((progress - 0.6F) / 0.4F, 0.0F, 1.0F);

        // The left cover lies on the +X side of its hinge: a positive yRot lifts it up and out to -X.
        float cover = open * COVER_OPEN;
        coverLinings[0].yRot = cover;
        coverLinings[1].yRot = -cover;
        for (ModelPart[] halves : skinCoverHalves) {
            halves[0].yRot = cover;
            halves[1].yRot = -cover;
        }

        // Direction angles are measured from +Y towards -Z; a positive xRot turns +Y towards +Z.
        float lower = Mth.lerp(rise, Mth.PI, LOWER_OPEN);
        float upper = Mth.lerp(rise, Mth.PI, UPPER_OPEN);
        lowerLink.xRot = -lower;
        upperLink.xRot = lower - upper;
        blade.xRot = upper - swing * BLADE_OPEN;
        edge.visible = slide > 0.0F;
        edge.y = EDGE_HOME - EDGE_TRAVEL * (1.0F - slide);
    }
}
