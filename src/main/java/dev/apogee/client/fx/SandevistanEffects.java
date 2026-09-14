package dev.apogee.client.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.apogee.client.ApogeeClientState;
import dev.apogee.client.config.ApogeeClientConfig;
import dev.apogee.config.SandevistanRules;
import dev.apogee.network.ApogeeNetworking.WearerStatus;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.registry.ApogeeSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

/**
 * Local presentation of Sandevistan state: entry/exit audio, radial screen transition, tint,
 * FOV pulse and the duration/cooldown HUD. Everything here is cosmetic and per client.
 */
public final class SandevistanEffects {
    private static final int TINT_RGB = 0x22B14C;
    private static final int EDGE_INNER_RGB = 0x42E56E;
    private static final int EDGE_MID_RGB = 0x2BE6E1;
    private static final int EDGE_OUTER_RGB = 0x806BFF;
    private static final float TRANSITION_TICKS = 8.0F;
    private static final int OVERDRIVE_FLASH_TICKS = 10;
    private static final int HUD_CYAN = 0x02D7F2;
    private static final int HUD_MAGENTA = 0xED1E79;
    private static final int HUD_YELLOW = 0xFCEE0A;
    private static final int HUD_RED = 0xFF4A57;
    private static final ResourceLocation HUD_FONT = ResourceLocation.fromNamespaceAndPath("apogee", "cyber");
    private static final float FOV_PULSE_TICKS = 10.0F;
    private static final int RADIAL_SEGMENTS = 96;

    private static final Int2LongOpenHashMap WEARER_SINCE = new Int2LongOpenHashMap();
    private static boolean localActive;
    private static long transitionStart = Long.MIN_VALUE;
    private static boolean transitionActivating;
    private static float tintAlpha;
    private static long fovPulseStart = Long.MIN_VALUE;
    private static long killFlashStart = Long.MIN_VALUE;
    private static float hudAlpha;
    private static long clientTicks;

    private SandevistanEffects() {
    }

    public static void reset() {
        WEARER_SINCE.clear();
        localActive = false;
        transitionStart = Long.MIN_VALUE;
        tintAlpha = 0.0F;
        fovPulseStart = Long.MIN_VALUE;
        stopLocalSounds();
    }

    // ------------------------------------------------------------------ state transitions

    public static void onStatus(WearerStatus previous, WearerStatus current) {
        if (previous.active() && current.active() && current.endTime() > previous.endTime()) {
            killFlashStart = clientTicks;
        }
        if (previous.active() == current.active()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        localActive = current.active();
        transitionStart = clientTicks;
        transitionActivating = current.active();
        fovPulseStart = clientTicks;
        if (current.active()) {
            minecraft.getSoundManager().stop(ApogeeSounds.SANDEVISTAN_DEACTIVATE.getLocation(), null);
            playLocal(ApogeeSounds.SANDEVISTAN_ACTIVATE);
        } else {
            minecraft.getSoundManager().stop(ApogeeSounds.SANDEVISTAN_ACTIVATE.getLocation(), null);
            playLocal(ApogeeSounds.SANDEVISTAN_DEACTIVATE);
        }
    }

    public static void onWearers(IntSet previous, IntSet current) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer self = minecraft.player;
        if (level == null) {
            return;
        }
        for (int id : current) {
            if (!previous.contains(id)) {
                WEARER_SINCE.put(id, clientTicks);
                playAtEntity(level.getEntity(id), self, ApogeeSounds.SANDEVISTAN_ACTIVATE);
            }
        }
        for (int id : previous) {
            if (!current.contains(id)) {
                WEARER_SINCE.remove(id);
                playAtEntity(level.getEntity(id), self, ApogeeSounds.SANDEVISTAN_DEACTIVATE);
            }
        }
    }

    public static void tick() {
        clientTicks++;
        ApogeeClientConfig config = ApogeeClientConfig.get();
        float target = localActive && config.screenTint ? config.screenTintOpacity : 0.0F;
        float step = localActive ? 0.08F : 0.05F;
        tintAlpha = tintAlpha < target ? Math.min(target, tintAlpha + step) : Math.max(target, tintAlpha - step);
        WearerStatus status = ApogeeClientState.status();
        boolean inUse = status.active() || status.cooldownEnd() > ApogeeClientState.clientGameTime();
        hudAlpha = Mth.clamp(hudAlpha + (inUse ? 0.25F : -0.125F), 0.0F, 1.0F);
    }

    /** The cyan overdrive texture shows during the first half second of an activation. */
    public static boolean isOverdriveFlash(Entity entity) {
        if (!ApogeeClientState.isActiveWearer(entity) || !WEARER_SINCE.containsKey(entity.getId())) {
            return false;
        }
        return clientTicks - WEARER_SINCE.get(entity.getId()) < OVERDRIVE_FLASH_TICKS;
    }

    private static void playLocal(SoundEvent sound) {
        float volume = ApogeeClientConfig.get().soundVolume;
        if (volume > 0.0F) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, volume));
        }
    }

    private static void playAtEntity(Entity entity, LocalPlayer self, SoundEvent sound) {
        float volume = ApogeeClientConfig.get().soundVolume;
        if (entity == null || entity == self || volume <= 0.0F) {
            return;
        }
        entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, 1.0F, false);
    }

    private static void stopLocalSounds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().stop(ApogeeSounds.SANDEVISTAN_ACTIVATE.getLocation(), null);
            minecraft.getSoundManager().stop(ApogeeSounds.SANDEVISTAN_DEACTIVATE.getLocation(), null);
        }
    }

    // ------------------------------------------------------------------ FOV

    public static double modifyFov(double fov, float partialTick) {
        ApogeeClientConfig config = ApogeeClientConfig.get();
        if (!config.fovEffect || fovPulseStart == Long.MIN_VALUE) {
            return fov;
        }
        float elapsed = clientTicks - fovPulseStart + partialTick;
        if (elapsed >= FOV_PULSE_TICKS) {
            return fov;
        }
        float progress = elapsed / FOV_PULSE_TICKS;
        float pulse = Mth.sin(progress * Mth.PI) * config.fovPulseStrength;
        return fov * (1.0F + (transitionActivating ? pulse : -pulse * 0.5F));
    }

    // ------------------------------------------------------------------ screen overlay

    /** Drawn before the vanilla HUD so hotbar and chat stay readable. */
    public static void renderScreenOverlay(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui && tintAlpha <= 0.0F) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float reveal = transitionReveal(partialTick);
        boolean radial = ApogeeClientConfig.get().radialTransition && reveal >= 0.0F;
        if (radial) {
            float alpha = Math.max(tintAlpha, ApogeeClientConfig.get().screenTintOpacity);
            drawRadial(graphics, width, height, reveal, alpha);
        } else if (tintAlpha > 0.001F) {
            graphics.fill(0, 0, width, height, color(TINT_RGB, tintAlpha));
        }
    }

    /** Returns the radial reveal in [0, 1] while a transition runs, or -1 when idle. */
    private static float transitionReveal(float partialTick) {
        if (transitionStart == Long.MIN_VALUE) {
            return -1.0F;
        }
        float linear = (clientTicks - transitionStart + partialTick) / TRANSITION_TICKS;
        if (linear >= 1.0F) {
            transitionStart = Long.MIN_VALUE;
            return -1.0F;
        }
        float inverse = 1.0F - Mth.clamp(linear, 0.0F, 1.0F);
        float eased = 1.0F - inverse * inverse * inverse;
        return transitionActivating ? eased : 1.0F - eased;
    }

    private static void drawRadial(GuiGraphics graphics, int width, int height, float reveal, float alpha) {
        if (reveal <= 0.001F || alpha <= 0.001F) {
            return;
        }
        float centerX = width * 0.5F;
        float centerY = height * 0.5F;
        float radius = (float) Math.sqrt(centerX * centerX + centerY * centerY) * reveal;
        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder disk = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int fill = color(TINT_RGB, alpha);
        for (int i = 0; i < RADIAL_SEGMENTS; i++) {
            double a0 = Math.PI * 2.0 * i / RADIAL_SEGMENTS;
            double a1 = Math.PI * 2.0 * (i + 1) / RADIAL_SEGMENTS;
            vertex(disk, matrix, centerX, centerY, fill);
            vertex(disk, matrix, centerX + (float) Math.cos(a0) * radius, centerY + (float) Math.sin(a0) * radius, fill);
            vertex(disk, matrix, centerX + (float) Math.cos(a1) * radius, centerY + (float) Math.sin(a1) * radius, fill);
        }
        draw(disk);

        BufferBuilder edge = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int inner = color(EDGE_INNER_RGB, alpha * 0.52F);
        int mid = color(EDGE_MID_RGB, alpha * 0.34F);
        int outer = color(EDGE_OUTER_RGB, alpha * 0.16F);
        float innerRadius = Math.max(0.0F, radius - 3.0F);
        float midRadius = radius + 7.0F;
        float outerRadius = radius + 12.0F;
        for (int i = 0; i < RADIAL_SEGMENTS; i++) {
            double a0 = Math.PI * 2.0 * i / RADIAL_SEGMENTS;
            double a1 = Math.PI * 2.0 * (i + 1) / RADIAL_SEGMENTS;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            ring(edge, matrix, centerX, centerY, c0, s0, c1, s1, innerRadius, midRadius, inner, mid);
            ring(edge, matrix, centerX, centerY, c0, s0, c1, s1, midRadius, outerRadius, mid, outer);
        }
        draw(edge);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void ring(BufferBuilder builder, Matrix4f matrix, float cx, float cy, float c0, float s0, float c1, float s1,
                             float r0, float r1, int color0, int color1) {
        vertex(builder, matrix, cx + c0 * r0, cy + s0 * r0, color0);
        vertex(builder, matrix, cx + c0 * r1, cy + s0 * r1, color1);
        vertex(builder, matrix, cx + c1 * r1, cy + s1 * r1, color1);
        vertex(builder, matrix, cx + c0 * r0, cy + s0 * r0, color0);
        vertex(builder, matrix, cx + c1 * r1, cy + s1 * r1, color1);
        vertex(builder, matrix, cx + c1 * r0, cy + s1 * r0, color0);
    }

    private static void vertex(BufferBuilder builder, Matrix4f matrix, float x, float y, int argb) {
        builder.addVertex(matrix, x, y, 0.0F).setColor(argb);
    }

    private static void draw(BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }

    private static int color(int rgb, float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24 | rgb & 0xFFFFFF;
    }

    // ------------------------------------------------------------------ HUD

    /**
     * Cyberware slot in the bottom-left corner, styled after Cyberpunk 2077's quick-access
     * cyberware: the implant icon with a fill that drains while active (cyan) and during the
     * cooldown (magenta), and the seconds left on top. Hidden while the implant is idle.
     */
    public static void renderHud(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !ApogeeClientConfig.get().hud) {
            return;
        }
        WearerStatus status = ApogeeClientState.status();
        SandevistanRules rules = ApogeeClientState.rules();
        long now = ApogeeClientState.clientGameTime();
        boolean active = status.active();
        boolean cooling = !active && status.cooldownEnd() > now;
        if (hudAlpha <= 0.0F) {
            return;
        }

        float remaining;
        float fill;
        float extension = 0.0F;
        int accent;
        if (active) {
            remaining = Math.max(0.0F, status.endTime() - now - partialTick);
            fill = Mth.clamp(remaining / rules.durationTicks(), 0.0F, 1.0F);
            int extra = rules.maxDurationTicks() - rules.durationTicks();
            extension = extra <= 0 ? 0.0F : Mth.clamp((remaining - rules.durationTicks()) / extra, 0.0F, 1.0F);
            boolean lastSeconds = remaining < 40.0F && (clientTicks / 3) % 2 == 0;
            accent = lastSeconds ? HUD_RED : HUD_CYAN;
        } else if (cooling) {
            remaining = Math.max(0.0F, status.cooldownEnd() - now - partialTick);
            fill = Mth.clamp(remaining / Math.max(1, rules.cooldownTicks()), 0.0F, 1.0F);
            accent = HUD_MAGENTA;
        } else {
            remaining = 0.0F;
            fill = 0.0F;
            accent = HUD_CYAN;
        }
        float killFlash = killFlashStart == Long.MIN_VALUE ? 0.0F
                : 1.0F - Mth.clamp((clientTicks - killFlashStart + partialTick) / 16.0F, 0.0F, 1.0F);
        if (killFlash > 0.0F) {
            accent = HUD_YELLOW;
        }

        int size = 24;
        int x = 6;
        int y = graphics.guiHeight() - size - 6;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, (1.0F - hudAlpha) * 6.0F, 0.0F);

        // Slot body with the bottom-left corner cut, like the game's quick-access squares.
        int background = withAlpha(0x12151A, 0.9F * hudAlpha);
        int chamfer = 5;
        for (int row = 0; row < size; row++) {
            int inset = Math.max(0, row - (size - chamfer));
            graphics.fill(x + inset, y + row, x + size, y + row + 1, background);
        }
        int fillHeight = Math.round(size * fill);
        int fillColor = withAlpha(active ? 0x02D7F2 : 0xED1E79, 0.32F * hudAlpha);
        for (int row = size - fillHeight; row < size; row++) {
            int inset = Math.max(0, row - (size - chamfer));
            graphics.fill(x + inset, y + row, x + size, y + row + 1, fillColor);
        }
        if (extension > 0.0F) {
            graphics.fill(x, y, x + Math.max(1, Math.round(size * extension)), y + 2, withAlpha(0xFCEE0A, hudAlpha));
        }

        // One-pixel frame following the chamfer.
        int frame = withAlpha(accent, hudAlpha);
        graphics.fill(x, y, x + size, y + 1, frame);
        graphics.fill(x + size - 1, y, x + size, y + size, frame);
        graphics.fill(x, y, x + 1, y + size - chamfer, frame);
        graphics.fill(x + chamfer, y + size - 1, x + size, y + size, frame);
        for (int i = 0; i < chamfer; i++) {
            graphics.fill(x + i, y + size - chamfer + i, x + i + 1, y + size - chamfer + i + 1, frame);
        }

        // Small implant icon in the upper-left, leaving room for the seconds.
        graphics.pose().pushPose();
        graphics.pose().translate(x + 1.5F, y + 1.5F, 0.0F);
        graphics.pose().scale(0.7F, 0.7F, 1.0F);
        graphics.renderItem(new ItemStack(ApogeeItems.SANDEVISTAN), 0, 0);
        graphics.pose().popPose();

        Font font = minecraft.font;
        if (remaining > 0.0F) {
            float secondsLeft = remaining / 20.0F;
            String number = active && secondsLeft < 3.0F
                    ? String.format(Locale.ROOT, "%.1f", secondsLeft)
                    : Integer.toString(Mth.ceil(secondsLeft));
            Component text = Component.literal(number).withStyle(style -> style.withFont(HUD_FONT));
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            graphics.drawString(font, text, x + size - 3 - font.width(text), y + size - 11, withAlpha(0xFFFFFF, hudAlpha), true);
            graphics.pose().popPose();
        }
        if (killFlash > 0.0F) {
            Component bonus = Component.translatable("tooltip.apogee.value.bonus_seconds", Integer.toString(rules.killExtensionTicks() / 20))
                    .withStyle(style -> style.withFont(HUD_FONT));
            graphics.drawString(font, bonus, x + size + 4, y + 2 - Math.round((1.0F - killFlash) * 6.0F),
                    withAlpha(0xFCEE0A, killFlash * hudAlpha), true);
        }
        graphics.pose().popPose();
    }

    private static int withAlpha(int rgb, float alpha) {
        return Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | rgb & 0xFFFFFF;
    }

    public static String seconds(float ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0F);
    }
}
