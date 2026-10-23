package dev.apogee.client.bloodpump;

import dev.apogee.bloodpump.BloodPumpRules;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtil;

/** One player's pump on the right forearm: the syringes, the tubes and the blood bag. */
public final class BloodPumpArm implements GeoAnimatable {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.blood_pump.idle");
    private static final RawAnimation INJECT = RawAnimation.begin().thenPlay("animation.blood_pump.inject");
    private static final RawAnimation REGEN = RawAnimation.begin().thenPlay("animation.blood_pump.regen");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int phase = -1;
    private float elapsed = -1.0F;

    /** Follows {@link BloodPumpClient#elapsed}: idle when ready, the injection, then the refill. */
    public void follow(float elapsed) {
        this.elapsed = elapsed;
    }

    private PlayState handle(AnimationState<BloodPumpArm> state) {
        int next = elapsed < 0.0F ? 0 : elapsed < BloodPumpRules.REGEN_TICKS ? 1 : 2;
        if (next != phase) {
            phase = next;
            state.getController().forceAnimationReset();
        }
        return state.setAndContinue(next == 0 ? IDLE : next == 1 ? INJECT : REGEN);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "blood_pump", 0, this::handle));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return RenderUtil.getCurrentTick();
    }
}
