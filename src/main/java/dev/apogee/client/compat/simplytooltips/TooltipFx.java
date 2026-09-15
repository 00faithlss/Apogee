package dev.apogee.client.compat.simplytooltips;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

/**
 * Animated extras for the Sandevistan tooltip: neon particles falling behind the content, monitor
 * scanlines with a cyan sweep, and a waving title that leaves colored afterimages. Everything is
 * derived from the clock, so no state is kept between frames.
 */
final class TooltipFx {
    private static final int CYAN = 0x00F0FF;
    private static final int MAGENTA = 0xFF2A6D;
    private static final int YELLOW = 0xFCEE0A;
    private static final int[] PARTICLE_COLORS = {YELLOW, CYAN, MAGENTA};
    private static final int PARTICLES = 40;
    private static final int SWEEP_H = 12;

    private TooltipFx() {
    }

    /** Pixel sparks drifting downward inside the panel. Draw before the content. */
    static void particles(GuiGraphics graphics, int x, int y, int w, int h, long nowMs) {
        int innerW = w - 6;
        int innerH = h - 6;
        for (int i = 0; i < PARTICLES; i++) {
            long lifeMs = 2200L + hash(i, 0) % 2600L;
            long clock = nowMs + hash(i, 1) % lifeMs;
            long cycle = clock / lifeMs;
            float age = (clock % lifeMs) / (float) lifeMs;
            float fall = innerH * (0.35F + (hash(i, cycle + 2) % 60) / 100.0F);
            float px = x + 3 + (hash(i, cycle + 3) % innerW) + Mth.sin(age * 6.0F + i) * 1.5F;
            float py = y + 3 + age * fall;
            if (py > y + h - 3) {
                continue;
            }
            float twinkle = 0.55F + 0.45F * Mth.sin(nowMs * 0.012F + i * 1.7F);
            float fade = Math.min(1.0F, (1.0F - age) * 1.6F) * Math.min(1.0F, age * 8.0F);
            int rgb = PARTICLE_COLORS[i % PARTICLE_COLORS.length];
            int size = hash(i, cycle + 4) % 6 == 0 ? 2 : 1;
            int ix = Math.round(px);
            int iy = Math.round(py);
            if (size == 2) {
                graphics.fill(ix - 1, iy - 1, ix + 3, iy + 3, argb(rgb, 0.12F * fade * twinkle));
            }
            graphics.fill(ix, iy, ix + size, iy + size, argb(rgb, 0.75F * fade * twinkle));
        }
    }

    /** Faint scanlines plus a cyan band sweeping downward. Draw after the content. */
    static void scan(GuiGraphics graphics, int x, int y, int w, int h, long nowMs) {
        int left = x + 2;
        int right = x + w - 2;
        for (int row = y + 2; row < y + h - 2; row += 2) {
            graphics.fill(left, row, right, row + 1, 0x14000000);
        }
        int span = h + SWEEP_H * 2;
        int bandBottom = y - SWEEP_H + (int) (nowMs * 0.045 % span);
        for (int i = 0; i < SWEEP_H; i++) {
            int row = bandBottom - i;
            if (row < y + 2 || row >= y + h - 2) {
                continue;
            }
            float strength = i == 0 ? 0.42F : 0.13F * (1.0F - i / (float) SWEEP_H);
            graphics.fill(left, row, right, row + 1, argb(CYAN, strength));
        }
    }

    /**
     * Title drawn letter by letter on a sine wave with a white-cyan-pink gradient, trailed by
     * magenta, yellow and cyan afterimages like the Sandevistan's own effect.
     */
    static void wavyTitle(GuiGraphics graphics, Font font, Component title, float x, float y, float scale, long nowMs) {
        String text = title.getString();
        Style style = title.getStyle();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        float sway = 0.6F + 0.4F * Mth.sin(nowMs * 0.0023F);
        drawRun(graphics, font, text, style, -5.0F * sway, nowMs, MAGENTA, 0.22F);
        drawRun(graphics, font, text, style, -3.2F * sway, nowMs, YELLOW, 0.28F);
        drawRun(graphics, font, text, style, -1.6F * sway, nowMs, CYAN, 0.40F);
        drawRun(graphics, font, text, style, 0.0F, nowMs, -1, 1.0F);
        pose.popPose();
    }

    private static void drawRun(GuiGraphics graphics, Font font, String text, Style style, float dx,
                                long nowMs, int solidRgb, float alpha) {
        PoseStack pose = graphics.pose();
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            Component letter = Component.literal(new String(Character.toChars(codePoint))).withStyle(style);
            // TTF advances are fractional: place each letter by the exact width of the prefix.
            float cursor = font.getSplitter().stringWidth(Component.literal(text.substring(0, i)).withStyle(style));
            float wave = Mth.sin(nowMs * 0.005F - i * 0.55F) * 1.0F;
            int rgb = solidRgb >= 0 ? solidRgb : gradient(nowMs, i);
            pose.pushPose();
            pose.translate(cursor + dx, wave, 0.0F);
            graphics.drawString(font, letter, 0, 0, argb(rgb, alpha), solidRgb < 0);
            pose.popPose();
            i += Character.charCount(codePoint);
        }
    }

    private static int gradient(long nowMs, int index) {
        float k = (Mth.sin(nowMs * 0.002F - index * 0.35F) + 1.0F) / 2.0F;
        return k < 0.5F ? lerp(0xFFFFFF, CYAN, k * 2.0F) : lerp(CYAN, 0xFF7AA8, (k - 0.5F) * 2.0F);
    }

    private static int lerp(int from, int to, float t) {
        int r = Math.round(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return r << 16 | g << 8 | b;
    }

    private static int argb(int rgb, float alpha) {
        // Font treats alpha below 4 as opaque, so keep a small floor.
        return Mth.clamp(Math.round(alpha * 255.0F), 5, 255) << 24 | rgb & 0xFFFFFF;
    }

    private static int hash(int index, long salt) {
        long h = index * 0x9E3779B97F4A7C15L + salt * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return (int) (h & 0x7FFFFFFF);
    }
}
