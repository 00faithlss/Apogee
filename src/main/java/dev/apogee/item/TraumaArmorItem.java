package dev.apogee.item;

import dev.apogee.Apogee;
import dev.apogee.client.armor.ArmorItemRenderer;
import dev.apogee.client.armor.TraumaArmorModel;
import dev.apogee.client.armor.TraumaArmorRenderer;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class TraumaArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TraumaArmorItem(Type type) {
        // Base provisional para revisar el conjunto en creativo, sin recetas nuevas.
        super(ArmorMaterials.NETHERITE, type, new Properties()
                .durability(type.getDurability(37)).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private TraumaArmorRenderer renderer;
            private ArmorItemRenderer<TraumaArmorItem> itemRenderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (itemRenderer == null) {
                    itemRenderer = new ArmorItemRenderer<>(item -> "trauma_team_" + item.getType().getName(),
                            TraumaArmorModel.LIT, Apogee.id("animations/trauma_team.animation.json"), true);
                }
                return itemRenderer;
            }

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T entity, ItemStack stack, @Nullable EquipmentSlot slot,
                    @Nullable HumanoidModel<T> original) {
                if (renderer == null) {
                    renderer = new TraumaArmorRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // La bisagra se interpola por portador desde su estado sincronizado.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
