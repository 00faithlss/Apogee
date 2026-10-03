package dev.apogee.client.bloodpump;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.client.mantis.MantisClient;
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

    private static final float OUT_PIXELS = 2.5F;

    private final BloodPumpGeoRenderer renderer = new BloodPumpGeoRenderer();

    private BloodPumpArmRenderer() {
    }

    public static void clear() {
        ARMS.clear();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntity> contextModel,
                       PoseStack poseStack, MultiBufferSource buffers, int light, LivingEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (!(contextModel instanceof HumanoidModel humanoid) || entity.isInvisible()) {
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
