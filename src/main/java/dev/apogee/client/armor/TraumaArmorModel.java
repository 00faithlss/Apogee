package dev.apogee.client.armor;

import dev.apogee.Apogee;
import dev.apogee.item.TraumaArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

public final class TraumaArmorModel extends GeoModel<TraumaArmorItem> {
    public static final ResourceLocation LIT = Apogee.id("textures/armor/trauma_team.png");
    public static final ResourceLocation UNLIT = Apogee.id("textures/armor/trauma_team_off.png");

    @Override
    public ResourceLocation getModelResource(TraumaArmorItem item) {
        return Apogee.id("geo/trauma_team.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TraumaArmorItem item) {
        return LIT;
    }

    @Override
    public ResourceLocation getAnimationResource(TraumaArmorItem item) {
        return Apogee.id("animations/trauma_team.animation.json");
    }

    @Override
    public void setCustomAnimations(TraumaArmorItem item, long instanceId, AnimationState<TraumaArmorItem> state) {
        var entity = state.getData(DataTickets.ENTITY);
        // Se calcula por portador para que dos cascos no compartan la pose de un singleton de objeto.
        float amount = TraumaHelmetClient.openness(entity, state.getPartialTick());
        getBone("frontal_abatible").ifPresent(bone -> bone.setRotX((float)Math.toRadians(60) * amount));
    }
}
