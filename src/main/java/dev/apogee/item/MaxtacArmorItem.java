package dev.apogee.item;

import dev.apogee.Apogee;
import dev.apogee.client.armor.ArmorItemRenderer;
import dev.apogee.client.armor.MaxtacArmorModel;
import dev.apogee.client.armor.MaxtacArmorRenderer;
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

public final class MaxtacArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final boolean closed;

    public MaxtacArmorItem(Type type) {
        this(type, false);
    }

    /** Con {@code closed} el casco cubre también boca y mentón; las demás piezas no cambian. */
    public MaxtacArmorItem(Type type, boolean closed) {
        // Base provisional para revisar el conjunto en creativo, sin recetas nuevas.
        super(ArmorMaterials.NETHERITE, type, new Properties()
                .durability(type.getDurability(37)).rarity(Rarity.EPIC).fireResistant());
        this.closed = closed;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private MaxtacArmorRenderer renderer;
            private ArmorItemRenderer<MaxtacArmorItem> itemRenderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (itemRenderer == null) {
                    itemRenderer = new ArmorItemRenderer<>(
                            item -> item.isClosed() ? "maxtac_helmet_closed" : "maxtac_" + item.getType().getName(),
                            MaxtacArmorModel.STANDBY, Apogee.id("animations/maxtac.animation.json"), false);
                }
                return itemRenderer;
            }

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T entity, ItemStack stack, @Nullable EquipmentSlot slot,
                    @Nullable HumanoidModel<T> original) {
                if (renderer == null) {
                    renderer = new MaxtacArmorRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // El movimiento sigue al jugador; el visor cambia de textura mediante su estado sincronizado.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
