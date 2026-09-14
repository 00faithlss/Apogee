package dev.apogee.client.compat.simplytooltips;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;

/**
 * Soft electric arcs crawling along the tooltip frame in Cyberpunk yellow, cyan and magenta.
 * Each arc is a jagged pixel path that hugs the border; arcs drift around the perimeter and
 * re-roll their shape every few frames so the edge flickers without flashing.
 */
final class TooltipElectricity {
    private static final int[] COLORS = {0xFCEE0A, 0x00F0FF, 0xFF2A6D};
    private static final int ARCS = 5;
    private static final long SHAPE_MS = 70L;
    private static final RandomSource RANDOM = RandomSource.create();

    private TooltipElectricity() {
    }

    static void draw(GuiGraphics graphics, int x, int y, int w, int h, long nowMs) {
        int perimeter = 2 * (w + h);
        for (int side = 0; side < 4; side++) {
            glowEdge(graphics, x, y, w, h, side, 0x1400F0FF);
        }
        long shapeSeed = nowMs / SHAPE_MS;
        for (int arc = 0; arc < ARCS; arc++) {
            // Arcs travel around the frame at slightly different speeds.
            double speed = 0.018 + arc * 0.004;
            int start = (int) ((nowMs * speed + arc * perimeter / (double) ARCS) % perimeter);
            RANDOM.setSeed(shapeSeed * 31L + arc * 7919L);
            if (RANDOM.nextFloat() < 0.2F) {
                continue; // brief gaps keep the effect subtle
            }
            int length = 18 + RANDOM.nextInt(26);
            int rgb = COLORS[arc % COLORS.length];
            drawArc(graphics, x, y, w, h, perimeter, start, length, rgb);
        }
    }

    private static void drawArc(GuiGraphics graphics, int x, int y, int w, int h, int perimeter,
                                int start, int length, int rgb) {
        int previousX = Integer.MIN_VALUE;
        int previousY = 0;
        int previousHaloX = 0;
        int previousHaloY = 0;
        for (int step = 0; step <= length; step += 3) {
            // Zigzag across the frame line, biased outward so the text area stays clean.
            int offset = step == 0 || step + 3 > length ? 0 : RANDOM.nextInt(6) - 2;
            int p = (start + step) % perimeter;
            int[] point = pointOnFrame(x, y, w, h, p, offset);
            int[] halo = pointOnFrame(x, y, w, h, p, offset + 1);
            float fade = 1.0F - Math.abs(step - length / 2.0F) / (length / 2.0F + 1.0F);
            if (previousX != Integer.MIN_VALUE) {
                line(graphics, previousHaloX, previousHaloY, halo[0], halo[1], alpha(rgb, 0.22F * fade));
                line(graphics, previousX, previousY, point[0], point[1], alpha(rgb, 0.45F + 0.45F * fade));
            }
            if (fade > 0.35F && RANDOM.nextInt(4) == 0) {
                // Short spark branching away from the frame.
                int[] spur = pointOnFrame(x, y, w, h, (p + RANDOM.nextInt(3)) % perimeter, offset + 2 + RANDOM.nextInt(3));
                line(graphics, point[0], point[1], spur[0], spur[1], alpha(rgb, 0.35F * fade));
            }
            previousX = point[0];
            previousY = point[1];
            previousHaloX = halo[0];
            previousHaloY = halo[1];
        }
    }

    /** A point on the frame at distance {@code p} clockwise from the top-left, pushed outward by {@code out}. */
    private static int[] pointOnFrame(int x, int y, int w, int h, int p, int out) {
        if (p < w) {
            return new int[]{x + p, y - out};
        }
        p -= w;
        if (p < h) {
            return new int[]{x + w - 1 + out, y + p};
        }
        p -= h;
        if (p < w) {
            return new int[]{x + w - 1 - p, y + h - 1 + out};
        }
        p -= w;
        return new int[]{x - out, y + h - 1 - p};
    }

    private static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int argb) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        for (int guard = 0; guard < 64; guard++) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, argb);
            if (x0 == x1 && y0 == y1) {
                return;
            }
            int doubled = 2 * error;
            if (doubled >= dy) {
                error += dy;
                x0 += sx;
            }
            if (doubled <= dx) {
                error += dx;
                y0 += sy;
            }
        }
    }

    private static void glowEdge(GuiGraphics graphics, int x, int y, int w, int h, int side, int argb) {
        switch (side) {
            case 0 -> graphics.fill(x, y - 1, x + w, y, argb);
            case 1 -> graphics.fill(x + w, y, x + w + 1, y + h, argb);
            case 2 -> graphics.fill(x, y + h, x + w, y + h + 1, argb);
            default -> graphics.fill(x - 1, y, x, y + h, argb);
        }
    }

    private static int alpha(int rgb, float alpha) {
        return Math.max(0, Math.min(255, (int) (alpha * 255.0F))) << 24 | rgb;
    }
}
