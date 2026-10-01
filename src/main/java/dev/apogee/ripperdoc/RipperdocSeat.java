package dev.apogee.ripperdoc;

import dev.apogee.registry.ApogeeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * An invisible seat on the ripperdoc table that the patient rides while the screen is open, so
 * they lie on the couch facing its foot. It goes away as soon as nobody rides it or the table is
 * gone.
 */
public class RipperdocSeat extends Entity {
    /** Height of the seat over the foot block, where the cushion is. */
    private static final double SEAT_HEIGHT = 0.62;
    /** Top of the cushion over the foot block (10 px). */
    public static final double CUSHION = 10.0 / 16.0;
    /** Height of the patient's hips over the cushion: half a leg's thickness. */
    public static final double HIPS_OVER_CUSHION = 2.0 / 16.0;
    /** How far the cushion's hinge lies from the foot block's centre, towards the head end. */
    public static final float HINGE_BEHIND = 3.0F / 16.0F;
    /** Tilt of the raised head end over the flat foot end, in degrees. */
    public static final float BACKREST = 30.0F;

    public RipperdocSeat(EntityType<? extends RipperdocSeat> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Lays {@code player} on the table whose foot is at {@code foot}; false if someone else lies there. */
    public static boolean layDown(Level level, BlockPos foot, BlockState footState, Player player) {
        if (!(footState.getBlock() instanceof RipperdocTableBlock)) {
            return false;
        }
        for (RipperdocSeat seat : level.getEntitiesOfClass(RipperdocSeat.class, new net.minecraft.world.phys.AABB(foot))) {
            if (seat.hasPassenger(player)) {
                return true;
            }
            if (!seat.getPassengers().isEmpty()) {
                return false;
            }
            seat.discard();
        }
        RipperdocSeat seat = new RipperdocSeat(ApogeeBlocks.RIPPERDOC_SEAT, level);
        // Facing the foot of the couch, the back against the raised head end.
        float yaw = footState.getValue(RipperdocTableBlock.FACING).getOpposite().toYRot();
        seat.moveTo(foot.getX() + 0.5, foot.getY() + SEAT_HEIGHT, foot.getZ() + 0.5, yaw, 0.0F);
        level.addFreshEntity(seat);
        player.stopRiding();
        return player.startRiding(seat, true);
    }

    /** Height of the top of the cushion this seat stands on. */
    public double cushionY() {
        return Math.floor(getY() - SEAT_HEIGHT + 1.0E-3) + CUSHION;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (getPassengers().isEmpty() || !(level().getBlockState(blockPosition()).getBlock() instanceof RipperdocTableBlock))) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        super.positionRider(passenger, move);
        // Keep the body along the couch; the head may still look around.
        passenger.setYBodyRot(getYRot());
        float delta = net.minecraft.util.Mth.wrapDegrees(passenger.getYRot() - getYRot());
        float clamped = net.minecraft.util.Mth.clamp(delta, -80.0F, 80.0F);
        passenger.yRotO += clamped - delta;
        passenger.setYRot(passenger.getYRot() + clamped - delta);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return Vec3.ZERO;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity passenger) {
        // Step off beside the couch, on the side the player was facing from.
        Vec3 side = Vec3.directionFromRotation(0.0F, getYRot() + 90.0F);
        return position().add(side).add(0.0, 0.4, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
