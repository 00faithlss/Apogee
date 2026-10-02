package dev.apogee.client.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.apogee.berserk.BerserkVariant;
import dev.apogee.client.ApogeeClientState;
import dev.apogee.client.config.ApogeeClientConfig;
import dev.apogee.config.BerserkRules;
import dev.apogee.network.ApogeeNetworking.BerserkStatus;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.registry.ApogeeSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Local presentation of the Berserk: a red vignette that beats with the wearer's heart (faster in
 * the last two seconds), a red flash on activation, the heartbeat itself and the HUD square.
 * Everything here is cosmetic and per client.
 */
public final class BerserkEffects {
    private static final int RED = 0xE0101E;
    private static final int HUD_RED = 0xFF2A3A;
    private static final int BEAT_TICKS = 18;
    private static final int FAST_BEAT_TICKS = 10;
    private static final int FLASH_TICKS = 10;

    private static long clientTicks;
    private static boolean localActive;
    private static float vignette;
    private static float hudAlpha;
    private static long lastBeat = Long.MIN_VALUE;
    private static long flashStart = Long.MIN_VALUE;
    private static long killFlashStart = Long.MIN_VALUE;

    private BerserkEffects() {
    }

    /** How visible the Berserk's HUD square is, so other squares can stack over it. */
    public static float hudAlpha() {
        return hudAlpha;
    }

    public static boolean isLocalActive() {
        return localActive;
    }

    public static void reset() {
        localActive = false;
        vignette = 0.0F;
        hudAlpha = 0.0F;
        lastBeat = Long.MIN_VALUE;
        flashStart = Long.MIN_VALUE;
        killFlashStart = Long.MIN_VALUE;
    }

    public static void onStatus(BerserkStatus previous, BerserkStatus current) {
        if (current.active() && !previous.active()) {
            flashStart = clientTicks;
            lastBeat = Long.MIN_VALUE;
        }
        if (current.active() && previous.active() && current.kills() > previous.kills()) {
            killFlashStart = clientTicks;
        }
        localActive = current.active();
    }

    public static void tick() {
        clientTicks++;
        ApogeeClientConfig config = ApogeeClientConfig.get();
        float target = localActive && config.screenTint ? 1.0F : 0.0F;
        vignette = vignette < target ? Math.min(target, vignette + 0.1F) : Math.max(target, vignette - 0.06F);
        BerserkStatus status = ApogeeClientState.berserkStatus();
        boolean inUse = status.active() || status.cooldownEnd() > ApogeeClientState.clientGameTime();
        hudAlpha = Mth.clamp(hudAlpha + (inUse ? 0.25F : -0.125F), 0.0F, 1.0F);

        if (localActive && clientTicks - lastBeat >= beatTicks()) {
            lastBeat = clientTicks;
            float volume = config.soundVolume;
            if (volume > 0.0F) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ApogeeSounds.BERSERK_HEARTBEAT, 1.0F, volume * 0.9F));
            }
        }
    }

    private static int beatTicks() {
        BerserkStatus status = ApogeeClientState.berserkStatus();
        long left = status.endTime() - ApogeeClientState.clientGameTime();
        return left < 40L ? FAST_BEAT_TICKS : BEAT_TICKS;
    }

    // ------------------------------------------------------------------ screen overlay

    /** Drawn before the vanilla HUD, like the Sandevistan's tint. */
    public static void renderScreenOverlay(GuiGraphics graphics, float partialTick) {
        if (vignette <= 0.001F || Minecraft.getInstance().player == null) {
            return;
        }
        float opacity = ApogeeClientConfig.get().screenTintOpacity * 2.2F;
        float sinceBeat = lastBeat == Long.MIN_VALUE ? 99.0F : clientTicks - lastBeat + partialTick;
        float pulse = (float) Math.exp(-sinceBeat / 3.5F);
        float alpha = Mth.clamp(vignette * opacity * (0.55F + 0.45F * pulse), 0.0F, 0.85F);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float thickness = Math.min(width, height) * (0.20F + 0.04F * pulse);

        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int outer = color(RED, alpha);
        int inner = color(RED, 0.0F);
        float w = width;
        float h = height;
        float t = thickness;
        edge(builder, matrix, 0, 0, w, 0, w - t, t, t, t, outer, inner);
        edge(builder, matrix, w, 0, w, h, w - t, h - t, w - t, t, outer, inner);
        edge(builder, matrix, w, h, 0, h, t, h - t, w - t, h - t, outer, inner);
        edge(builder, matrix, 0, h, 0, 0, t, t, t, h - t, outer, inner);
        if (flashStart != Long.MIN_VALUE) {
            float flash = 1.0F - (clientTicks - flashStart + partialTick) / FLASH_TICKS;
            if (flash > 0.0F) {
                int fill = color(RED, flash * 0.35F);
                quad(builder, matrix, 0, 0, w, 0, w, h, 0, h, fill, fill, fill, fill);
            } else {
                flashStart = Long.MIN_VALUE;
            }
        }
        MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** One side of the vignette: the screen edge (x0,y0)-(x1,y1) fading to the inner edge. */
    private static void edge(BufferBuilder builder, Matrix4f matrix, float x0, float y0, float x1, float y1,
                             float ix1, float iy1, float ix0, float iy0, int outer, int inner) {
        quad(builder, matrix, x0, y0, x1, y1, ix1, iy1, ix0, iy0, outer, outer, inner, inner);
    }

    private static void quad(BufferBuilder builder, Matrix4f matrix, float x0, float y0, float x1, float y1, float x2, float y2,
                             float x3, float y3, int c0, int c1, int c2, int c3) {
        builder.addVertex(matrix, x0, y0, 0.0F).setColor(c0);
        builder.addVertex(matrix, x1, y1, 0.0F).setColor(c1);
        builder.addVertex(matrix, x2, y2, 0.0F).setColor(c2);
        builder.addVertex(matrix, x0, y0, 0.0F).setColor(c0);
        builder.addVertex(matrix, x2, y2, 0.0F).setColor(c2);
        builder.addVertex(matrix, x3, y3, 0.0F).setColor(c3);
    }

    private static int color(int rgb, float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24 | rgb & 0xFFFFFF;
    }

    // ------------------------------------------------------------------ HUD

    /**
     * The operating system square in the bottom-left corner, where the Sandevistan's goes (only
     * one of them can be installed): red while active, magenta during the cooldown, with the
     * health the kills so far will restore in yellow above the seconds.
     */
    public static void renderHud(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !ApogeeClientConfig.get().hud || hudAlpha <= 0.0F) {
            return;
        }
        BerserkStatus status = ApogeeClientState.berserkStatus();
        BerserkRules rules = ApogeeClientState.berserkRules();
        BerserkVariant variant = BerserkVariant.byOrdinal(status.variant());
        BerserkRules.Stats stats = rules.stats(variant);
        long now = ApogeeClientState.clientGameTime();
        boolean active = status.active();
        float remaining;
        float fill;
        int accent;
        if (active) {
            remaining = Math.max(0.0F, status.endTime() - now - partialTick);
            fill = Mth.clamp(remaining / stats.durationTicks(), 0.0F, 1.0F);
            boolean lastSeconds = remaining < 40.0F && (clientTicks / 3) % 2 == 0;
            accent = lastSeconds ? CyberwareHudSlot.YELLOW : HUD_RED;
        } else {
            remaining = Math.max(0.0F, status.cooldownEnd() - now - partialTick);
            fill = Mth.clamp(remaining / Math.max(1, stats.cooldownTicks()), 0.0F, 1.0F);
            accent = CyberwareHudSlot.MAGENTA;
        }

        int size = CyberwareHudSlot.SIZE;
        int x = 6;
        int y = graphics.guiHeight() - size - 6;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, (1.0F - hudAlpha) * 6.0F, 0.0F);
        CyberwareHudSlot.draw(graphics, x, y, hudAlpha, fill, active ? HUD_RED : CyberwareHudSlot.MAGENTA, accent, new ItemStack(icon(variant)));
        CyberwareHudSlot.seconds(graphics, x, y, remaining, active ? 3.0F : 0.0F, accent, hudAlpha);
        if (active && status.kills() > 0) {
            int heal = Math.min(100, status.kills() * rules.healPerKillPercent());
            float bump = killFlashStart == Long.MIN_VALUE ? 0.0F
                    : 1.0F - Mth.clamp((clientTicks - killFlashStart + partialTick) / 12.0F, 0.0F, 1.0F);
            Component text = Component.translatable("hud.apogee.berserk.heal", heal)
                    .withStyle(style -> style.withFont(CyberwareHudSlot.FONT));
            graphics.drawString(minecraft.font, text, x + size + 4, y - 8 - Math.round(bump * 3.0F),
                    CyberwareHudSlot.withAlpha(CyberwareHudSlot.YELLOW, hudAlpha), true);
        }
        graphics.pose().popPose();
    }

    public static Item icon(BerserkVariant variant) {
        return switch (variant) {
            case ZETATECH -> ApogeeItems.BERSERK_ZETATECH;
            case BIODYNE -> ApogeeItems.BERSERK_BIODYNE;
            case MILITECH -> ApogeeItems.BERSERK_MILITECH;
        };
    }
}
