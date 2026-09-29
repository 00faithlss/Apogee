package dev.apogee.client.mantis;

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
 * One Mantis Blade on one player's arm, as GeckoLib sees it. Each arm has its own controller, so
 * the two sides animate independently: folded, deploying, deployed, attacking or retracting.
 */
public final class MantisArm implements GeoAnimatable {
    private static final RawAnimation FOLDED = RawAnimation.begin().thenLoop("animation.mantis.folded");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.mantis.idle");
    private static final RawAnimation DEPLOY = RawAnimation.begin().thenPlayAndHold("animation.mantis.deploy");
    private static final RawAnimation RETRACT = RawAnimation.begin().thenPlayAndHold("animation.mantis.retract");
    /** Named after the arm that cuts; the other arm plays the same clip to follow through. */
    private static final RawAnimation ATTACK_RIGHT = RawAnimation.begin().thenPlay("animation.mantis.attack_right");
    private static final RawAnimation ATTACK_LEFT = RawAnimation.begin().thenPlay("animation.mantis.attack_left");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final boolean right;
    private MantisVariant variant = MantisVariant.STANDARD;
    private boolean deployed;
    private RawAnimation current = FOLDED;
    private boolean restart;
    /** Most segments of the blade tracked for its effects. */
    static final int MAX_BLADE_SEGMENTS = 16;
    /** Ends of each blade cube along its length, in world space, as last drawn: x0 y0 z0 x1 y1 z1 per segment. */
    final double[] bladeSegments = new double[MAX_BLADE_SEGMENTS * 6];
    int bladeSegmentCount;
    /** Client tick the segments were recorded on. */
    long bladeTick = Long.MIN_VALUE;
    /** Whether the segments come from the first-person hands. */
    boolean bladeFirstPerson;

    public MantisArm(boolean right) {
        this.right = right;
    }

    public boolean right() {
        return right;
    }

    public MantisVariant variant() {
        return variant;
    }

    public void setVariant(MantisVariant variant) {
        this.variant = variant;
    }

    /**
     * Follows the synced deployed flag. With {@code instant} (a player already deployed when
     * first seen) the blade appears open instead of replaying the whole unfold.
     */
    public void setDeployed(boolean deployed, boolean instant) {
        if (this.deployed == deployed) {
            return;
        }
        this.deployed = deployed;
        current = deployed ? (instant ? IDLE : DEPLOY) : (instant ? FOLDED : RETRACT);
        restart = true;
    }

    /** Plays the attack clip for the arm that cuts ({@link MantisRules#ARM_RIGHT} or {@link MantisRules#ARM_LEFT}). */
    public void attack(int arm) {
        if (!deployed) {
            return;
        }
        current = arm == MantisRules.ARM_LEFT ? ATTACK_LEFT : ATTACK_RIGHT;
        restart = true;
    }

    private PlayState handle(AnimationState<MantisArm> state) {
        AnimationController<MantisArm> controller = state.getController();
        if (restart) {
            restart = false;
            controller.forceAnimationReset();
        } else if ((current == ATTACK_RIGHT || current == ATTACK_LEFT) && controller.hasAnimationFinished()) {
            current = IDLE;
        }
        return state.setAndContinue(current);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "mantis", 0, this::handle));
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
