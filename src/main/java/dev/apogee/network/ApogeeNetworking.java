package dev.apogee.network;

import dev.apogee.Apogee;
import dev.apogee.config.SandevistanRules;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ApogeeNetworking {
    private ApogeeNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(ToggleSandevistan.TYPE, ToggleSandevistan.CODEC);
        PayloadTypeRegistry.playC2S().register(AirDashRequest.TYPE, AirDashRequest.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleMantis.TYPE, ToggleMantis.CODEC);
        PayloadTypeRegistry.playC2S().register(MantisThrust.TYPE, MantisThrust.CODEC);
        PayloadTypeRegistry.playC2S().register(DoubleJumpRequest.TYPE, DoubleJumpRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(MantisSync.TYPE, MantisSync.CODEC);
        PayloadTypeRegistry.playS2C().register(MantisSwing.TYPE, MantisSwing.CODEC);
        PayloadTypeRegistry.playS2C().register(RulesSync.TYPE, RulesSync.CODEC);
        PayloadTypeRegistry.playS2C().register(WearerStatus.TYPE, WearerStatus.CODEC);
        PayloadTypeRegistry.playS2C().register(TimeFieldSync.TYPE, TimeFieldSync.CODEC);
    }

    /** Client asks to activate, or to end its active Sandevistan early. The server validates. */
    public record ToggleSandevistan() implements CustomPacketPayload {
        public static final ToggleSandevistan INSTANCE = new ToggleSandevistan();
        public static final Type<ToggleSandevistan> TYPE = new Type<>(Apogee.id("toggle_sandevistan"));
        public static final StreamCodec<FriendlyByteBuf, ToggleSandevistan> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Reinforced Tendons air dash performed by the client; the server validates it and replays the effects. */
    public record AirDashRequest() implements CustomPacketPayload {
        public static final AirDashRequest INSTANCE = new AirDashRequest();
        public static final Type<AirDashRequest> TYPE = new Type<>(Apogee.id("air_dash"));
        public static final StreamCodec<FriendlyByteBuf, AirDashRequest> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client performed the Reinforced Tendons double jump; the server validates and plays the effects. */
    public record DoubleJumpRequest() implements CustomPacketPayload {
        public static final DoubleJumpRequest INSTANCE = new DoubleJumpRequest();
        public static final Type<DoubleJumpRequest> TYPE = new Type<>(Apogee.id("double_jump"));
        public static final StreamCodec<FriendlyByteBuf, DoubleJumpRequest> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client asks to deploy or retract the Mantis Blades. The server validates. */
    public record ToggleMantis() implements CustomPacketPayload {
        public static final ToggleMantis INSTANCE = new ToggleMantis();
        public static final Type<ToggleMantis> TYPE = new Type<>(Apogee.id("toggle_mantis"));
        public static final StreamCodec<FriendlyByteBuf, ToggleMantis> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client asks for the two-blade lunge (right click while deployed). */
    public record MantisThrust() implements CustomPacketPayload {
        public static final MantisThrust INSTANCE = new MantisThrust();
        public static final Type<MantisThrust> TYPE = new Type<>(Apogee.id("mantis_thrust"));
        public static final StreamCodec<FriendlyByteBuf, MantisThrust> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Whether a player has the Mantis Blades out, and which variant. Sent to the player and everyone tracking them. */
    public record MantisSync(int entityId, boolean deployed, int variant) implements CustomPacketPayload {
        public static final Type<MantisSync> TYPE = new Type<>(Apogee.id("mantis_sync"));
        public static final StreamCodec<FriendlyByteBuf, MantisSync> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MantisSync::entityId,
                ByteBufCodecs.BOOL, MantisSync::deployed,
                ByteBufCodecs.VAR_INT, MantisSync::variant,
                MantisSync::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Which arm slashed (0 right, 1 left) or 2 for the lunge. Sent to players tracking the wearer. */
    public record MantisSwing(int entityId, int arm) implements CustomPacketPayload {
        public static final Type<MantisSwing> TYPE = new Type<>(Apogee.id("mantis_swing"));
        public static final StreamCodec<FriendlyByteBuf, MantisSwing> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MantisSwing::entityId,
                ByteBufCodecs.VAR_INT, MantisSwing::arm,
                MantisSwing::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Host rules, sent on join so tooltips/HUD mirror the values used by the mechanic. */
    public record RulesSync(SandevistanRules rules) implements CustomPacketPayload {
        public static final Type<RulesSync> TYPE = new Type<>(Apogee.id("rules_sync"));
        public static final StreamCodec<FriendlyByteBuf, RulesSync> CODEC =
                SandevistanRules.STREAM_CODEC.map(RulesSync::new, RulesSync::rules).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * The receiving player's own state, as server game times. The client compares them with its
     * synced level game time, so no per-tick packets are needed for the HUD.
     */
    public record WearerStatus(boolean active, long startTime, long endTime, long maxEndTime, long cooldownEnd)
            implements CustomPacketPayload {
        public static final WearerStatus IDLE = new WearerStatus(false, 0L, 0L, 0L, 0L);
        public static final Type<WearerStatus> TYPE = new Type<>(Apogee.id("wearer_status"));
        public static final StreamCodec<FriendlyByteBuf, WearerStatus> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, WearerStatus::active,
                ByteBufCodecs.VAR_LONG, WearerStatus::startTime,
                ByteBufCodecs.VAR_LONG, WearerStatus::endTime,
                ByteBufCodecs.VAR_LONG, WearerStatus::maxEndTime,
                ByteBufCodecs.VAR_LONG, WearerStatus::cooldownEnd,
                WearerStatus::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Everything slowed and every active wearer in the receiver's dimension. Sent whenever the
     * sets change and whenever a player joins, respawns or changes dimension.
     */
    public record TimeFieldSync(IntList slowedEntities, IntList activeWearers) implements CustomPacketPayload {
        public static final Type<TimeFieldSync> TYPE = new Type<>(Apogee.id("time_field"));
        private static final StreamCodec<FriendlyByteBuf, IntList> INT_LIST = StreamCodec.of(
                (buf, list) -> {
                    buf.writeVarInt(list.size());
                    for (int i = 0; i < list.size(); i++) {
                        buf.writeVarInt(list.getInt(i));
                    }
                },
                buf -> {
                    int size = Math.min(buf.readVarInt(), 65536);
                    IntArrayList list = new IntArrayList(size);
                    for (int i = 0; i < size; i++) {
                        list.add(buf.readVarInt());
                    }
                    return list;
                }
        );
        public static final StreamCodec<FriendlyByteBuf, TimeFieldSync> CODEC = StreamCodec.composite(
                INT_LIST, TimeFieldSync::slowedEntities,
                INT_LIST, TimeFieldSync::activeWearers,
                TimeFieldSync::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
