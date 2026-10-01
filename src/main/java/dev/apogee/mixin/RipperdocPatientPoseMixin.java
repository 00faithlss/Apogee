package dev.apogee.mixin;

import dev.apogee.ripperdoc.RipperdocSeat;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A patient on the ripperdoc couch takes the sleeping pose, the same one a bed gives, so every
 * client (and animation packs such as Fresh Animations) shows them lying down. Only the pose
 * changes: they do not sleep, so nights are not skipped and the couch is no spawn point.
 */
@Mixin(Player.class)
public abstract class RipperdocPatientPoseMixin {
    /** Replaces vanilla's choice of pose, which would leave it alone: a standing body does not fit in the couch. */
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void apogee$lieOnCouch(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.getVehicle() instanceof RipperdocSeat) {
            player.setPose(Pose.SLEEPING);
            ci.cancel();
        }
    }
}
