package dev.apogee.client.bloodpump;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.mantis.MantisClient;
import dev.apogee.mantis.MantisVariant;
import dev.apogee.registry.ApogeeItems;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** The Blood Pump on the wearer's right forearm, seen in third person. */
public final class BloodPumpArmRenderer implements TrinketRenderer {
    public static final BloodPumpArmRenderer INSTANCE = new BloodPumpArmRenderer();
    private static final Int2ObjectOpenHashMap<BloodPumpArm> ARMS = new Int2ObjectOpenHashMap<>();

    /** Nanowire: the housing rides on the player's own arm, so the pump needs only a hair of clearance. */
    private static final float OUT_PIXELS = 0.5F;
    /** The Mantis housing is as wide as the arm, so the pump only needs a hair of clearance. */
    private static final float MANTIS_OUT_PIXELS = 0.5F;

    private final BloodPumpGeoRenderer renderer = new BloodPumpGeoRenderer();

    private BloodPumpArmRenderer() {
    }

    public static void clear() {
        ARMS.clear();
    }

    /** Whether the wearer's right arm is the Mantis Blades' model, which the pump then follows. */
    public static boolean ridesMantis(LivingEntity entity) {
        MantisVariant variant = MantisClient.variant(entity);
        return variant != null && variant.isBlades();
    }

    /** Whether this entity has the Blood Pump fitted, as far as the client can tell. */
    public static boolean worn(LivingEntity entity) {
        return TrinketsApi.getTrinketComponent(entity).map(component -> component.isEquipped(ApogeeItems.BLOOD_PUMP)).orElse(false);
    }

    /**
     * Draws the pump on the Mantis Blades' housing. {@code poseStack} is at the rig's model space
     * (the seat on the arm) and {@code chain} lists the bones from the root down to the housing, so
     * the pump opens and folds with the arm instead of staying on the vanilla arm's pose.
     * {@code rigFrame} is that seat, relative to the arm part.
     */
    public void renderOnMantis(PoseStack poseStack, AbstractClientPlayer player, MultiBufferSource buffers, int light,
                               float tickDelta, List<GeoBone> chain, Matrix4f rigFrame) {
        BloodPumpArm arm = ARMS.computeIfAbsent(player.getId(), id -> new BloodPumpArm());
        arm.follow(BloodPumpClient.elapsed(player, tickDelta));
        float armor = player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ? 0.0F : 1.0F;
        Matrix4f onArm = new Matrix4f()
                .translate(-(armor + MANTIS_OUT_PIXELS) / 16.0F, 0.0F, 0.0F)
                .translate(5.0F / 16.0F, 22.0F / 16.0F, 0.0F)
                .scale(-1.0F, -1.0F, -1.0F);
        poseStack.pushPose();
        for (GeoBone bone : chain) {
            RenderUtil.prepMatrixForBone(poseStack, bone);
        }
        poseStack.mulPose(new Matrix4f(rigFrame).invert().mul(onArm));
        renderer.renderInto(poseStack, arm, buffers, light, tickDelta);
        poseStack.popPose();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntity> contextModel,
                       PoseStack poseStack, MultiBufferSource buffers, int light, LivingEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (!(contextModel instanceof HumanoidModel humanoid) || entity.isInvisible()) {
            return;
        }
        if (ridesMantis(entity)) {
            // The Mantis Blades replace the arm: MantisRig draws the pump on their housing instead.
            return;
        }
        BloodPumpArm arm = ARMS.computeIfAbsent(entity.getId(), id -> new BloodPumpArm());
        arm.follow(BloodPumpClient.elapsed(entity, tickDelta));
        poseStack.pushPose();
        float armor = entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ? 0.0F : 1.0F;
        humanoid.rightArm.translateAndRotate(poseStack);
        // Outside the armor sleeve, which is a pixel thicker than the arm.
        poseStack.translate(-armor / 16.0F, 0.0F, 0.0F);
        if (MantisClient.variant(entity) != null) {
            // The arm cyberware's rig sits on this forearm too: ride just outside it, clear of the rig.
            poseStack.translate(-OUT_PIXELS / 16.0F, 0.0F, 0.0F);
        }
        // The export is in a player-model's space: x to the wearer's right, y up from the feet,
        // the right arm's pivot at (-5, 22, 0) pixels. Seat it on the arm part's own space (y down).
        poseStack.translate(5.0F / 16.0F, 22.0F / 16.0F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, -1.0F);
        renderer.renderInto(poseStack, arm, buffers, light, tickDelta);
        poseStack.popPose();
    }
}
