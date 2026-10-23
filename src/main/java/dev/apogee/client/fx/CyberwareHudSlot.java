package dev.apogee.client.fx;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * One quick-access cyberware square of the HUD, styled after Cyberpunk 2077: a dark square with
 * the bottom-left corner cut, a fill that drains with the time left, a frame in the accent colour,
 * the implant's icon and the seconds left beside it. Shared by the Sandevistan and arm cyberware.
 */
public final class CyberwareHudSlot {
    public static final int SIZE = 26;
    public static final int CYAN = 0x02D7F2;
    public static final int MAGENTA = 0xED1E79;
    public static final int YELLOW = 0xFCEE0A;
    public static final int RED = 0xFF4A57;
    public static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath("apogee", "cyber");
    private static final int CHAMFER = 5;

    private CyberwareHudSlot() {
    }

    /** The square at {@code x, y}: body, fill ({@code 0..1}, from the bottom), frame and icon. */
    public static void draw(GuiGraphics graphics, int x, int y, float alpha, float fill, int fillRgb, int accent, ItemStack icon) {
        int background = withAlpha(0x12151A, 0.9F * alpha);
        for (int row = 0; row < SIZE; row++) {
            int inset = Math.max(0, row - (SIZE - CHAMFER));
            graphics.fill(x + inset, y + row, x + SIZE, y + row + 1, background);
        }
        int fillHeight = Math.round(SIZE * Mth.clamp(fill, 0.0F, 1.0F));
        int fillColor = withAlpha(fillRgb, 0.32F * alpha);
        for (int row = SIZE - fillHeight; row < SIZE; row++) {
            int inset = Math.max(0, row - (SIZE - CHAMFER));
            graphics.fill(x + inset, y + row, x + SIZE, y + row + 1, fillColor);
        }
        // One-pixel frame following the chamfer.
        int frame = withAlpha(accent, alpha);
        graphics.fill(x, y, x + SIZE, y + 1, frame);
        graphics.fill(x + SIZE - 1, y, x + SIZE, y + SIZE, frame);
        graphics.fill(x, y, x + 1, y + SIZE - CHAMFER, frame);
        graphics.fill(x + CHAMFER, y + SIZE - 1, x + SIZE, y + SIZE, frame);
        for (int i = 0; i < CHAMFER; i++) {
            graphics.fill(x + i, y + SIZE - CHAMFER + i, x + i + 1, y + SIZE - CHAMFER + i + 1, frame);
        }
        int iconOffset = (SIZE - 16) / 2;
        graphics.renderItem(icon, x + iconOffset, y + iconOffset);
    }

    /** Seconds left beside the square, with one decimal under {@code decimalsBelow} seconds. */
    public static void seconds(GuiGraphics graphics, int x, int y, float remainingTicks, float decimalsBelow, int accent, float alpha) {
        if (remainingTicks <= 0.0F) {
            return;
        }
        float secondsLeft = remainingTicks / 20.0F;
        String number = secondsLeft < decimalsBelow ? String.format(Locale.ROOT, "%.1f", secondsLeft) : Integer.toString(Mth.ceil(secondsLeft));
        Component text = Component.literal(number).withStyle(style -> style.withFont(FONT));
        graphics.pose().pushPose();
        graphics.pose().translate(x + SIZE + 4, y + SIZE / 2.0F - 6.0F, 0.0F);
        graphics.pose().scale(1.4F, 1.4F, 1.0F);
        graphics.drawString(Minecraft.getInstance().font, text, 0, 0, withAlpha(accent, alpha), true);
        graphics.pose().popPose();
    }

    /** Horizontal start of the floating bonus text, clear of the seconds drawn beside the square. */
    public static final int BONUS_X_OFFSET = SIZE + 36;

    /** Vertical step between stacked squares. */
    public static final int STEP = SIZE + 6;

    /**
     * Visibility of the operating system square (Sandevistan or Berserk, they share the slot),
     * so the squares stacked over it keep a single, consistent position.
     */
    public static float osAlpha() {
        return Math.max(SandevistanEffects.hudAlpha(), BerserkEffects.hudAlpha());
    }

    public static int withAlpha(int rgb, float alpha) {
        return Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | rgb & 0xFFFFFF;
    }
}
