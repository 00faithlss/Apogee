package dev.apogee.client.nanowire;

import dev.apogee.mantis.MantisRules;
import dev.apogee.mantis.MantisVariant;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtil;

/**
 * One forearm housing of a player's nanowire, as GeckoLib sees it: its shutter, cover, spool and
 * wire. Both sides play the same clip (each export holds its own arm's part of every move), so a
 * lash by one arm also moves the other wire.
 */
public final class NanowireArm implements GeoAnimatable {
    private static final RawAnimation STOWED = RawAnimation.begin().thenLoop("animation.nanowire.stowed");
    private static final RawAnimation DEPLOY = RawAnimation.begin().thenPlay("animation.nanowire.deploy").thenLoop("animation.nanowire.idle");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.nanowire.idle");
    private static final RawAnimation RETRACT = RawAnimation.begin().thenPlay("animation.nanowire.retract").thenLoop("animation.nanowire.stowed");
    /** Named after the arm that lashes. */
    private static final RawAnimation ATTACK_RIGHT = RawAnimation.begin().thenPlay("animation.nanowire.attack_right").thenLoop("animation.nanowire.idle");
    private static final RawAnimation ATTACK_LEFT = RawAnimation.begin().thenPlay("animation.nanowire.attack_left").thenLoop("animation.nanowire.idle");
    private static final RawAnimation HEAVY = RawAnimation.begin().thenPlay("animation.nanowire.heavy").thenLoop("animation.nanowire.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final boolean right;
    private boolean deployed;
    private MantisVariant variant = MantisVariant.NANOWIRE;
    private RawAnimation current = STOWED;
    private boolean restart;

    public NanowireArm(boolean right) {
        this.right = right;
    }

    public MantisVariant variant() {
        return variant;
    }

    public void setVariant(MantisVariant variant) {
        this.variant = variant;
    }

    public boolean right() {
        return right;
    }

    /**
     * Follows the synced deployed flag. With {@code instant} (a player already deployed when
     * first seen) the wire appears in its rest pose instead of replaying the whole deploy.
     */
    public void setDeployed(boolean deployed, boolean instant) {
        if (this.deployed == deployed) {
            return;
        }
        this.deployed = deployed;
        current = deployed ? (instant ? IDLE : DEPLOY) : (instant ? STOWED : RETRACT);
        restart = true;
    }

    /** A lash by the given arm ({@link MantisRules#ARM_RIGHT} or {@link MantisRules#ARM_LEFT}). */
    public void attack(int arm) {
        if (!deployed) {
            return;
        }
        current = arm == MantisRules.ARM_LEFT ? ATTACK_LEFT : ATTACK_RIGHT;
        restart = true;
    }

    /** The two-wire lash. */
    public void heavy() {
        if (!deployed) {
            return;
        }
        current = HEAVY;
        restart = true;
    }

    private PlayState handle(AnimationState<NanowireArm> state) {
        if (restart) {
            restart = false;
            state.getController().forceAnimationReset();
        }
        return state.setAndContinue(current);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "nanowire", 0, this::handle));
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
