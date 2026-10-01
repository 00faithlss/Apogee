package dev.apogee.client.ripperdoc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.registry.ApogeeBlocks;
import dev.apogee.ripperdoc.RipperdocTableBlockEntity;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The table as an item: the couch model, scaled down to fit a slot or a hand. */
public final class RipperdocTableItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    public static final RipperdocTableItemRenderer INSTANCE = new RipperdocTableItemRenderer();

    private RipperdocTableRenderer renderer;
    private RipperdocTableBlockEntity display;

    private RipperdocTableItemRenderer() {
    }

    @Override
    public void render(ItemStack stack, ItemDisplayContext mode, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (renderer == null) {
            renderer = new RipperdocTableRenderer();
            display = new RipperdocTableBlockEntity(BlockPos.ZERO, ApogeeBlocks.RIPPERDOC_TABLE.defaultBlockState());
        }
        poseStack.pushPose();
        switch (mode) {
            case GUI -> {
                poseStack.translate(0.5F, 0.22F, 0.5F);
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(135.0F));
                poseStack.scale(0.42F, 0.42F, 0.42F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.5F, 0.4F, 0.5F);
                poseStack.scale(0.3F, 0.3F, 0.3F);
            }
            default -> {
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
        }
        // The block renderer draws around the block's corner; centre it on the origin.
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        renderer.render(display, 0.0F, poseStack, buffers, light, overlay);
        poseStack.popPose();
    }
}
