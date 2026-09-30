package dev.apogee.client.compat.simplytooltips;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.apogee.client.tooltip.MantisTooltipContent;
import dev.apogee.mantis.MantisVariant;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplytooltips.client.TooltipKeybinds;

/**
 * Cyberpunk 2077 style weapon card for the Mantis Blades: translucent red panel, salmon-red frame
 * with the game's cyan corner, yellow text, damage per hit and attacks per second
 * in large type with the damage type beside them. Below, the Description view lists the controls
 * and the variant's ability under their own headers (as Simply Swords does for its unique
 * weapons) and the Stats view the exact numbers; Simply Tooltips' cycle key switches them.
 * Colors were sampled from the game's inventory screen.
 */
public final class MantisTooltipPanel {
    private static final int RED = 0xFFF2565A;
    private static final int RED_SOFT = 0xFFE6716A;
    private static final int RED_DIM = 0xFF7A2A30;
    private static final int RED_DARK = 0xFF3A0E13;
    private static final int YELLOW = 0xFFFCEE0A;
    private static final int YELLOW_SOFT = 0xFFEDE38A;
    private static final int CYAN = 0xFF7CE0EF;
    private static final int BACKGROUND_TOP = 0xC8401219;
    private static final int BACKGROUND_BOTTOM = 0xC8180609;
    private static final int CARD = 0x90100305;
    private static final int DROP_SHADOW = 0xA0050102;
    private static final int TAG_TEXT = 0xFF1A0508;

    private static final int PAD = 8;
    private static final int CARD_SIZE = 30;
    private static final int HEADER_H = 31;
    private static final int NUMBERS_H = 21;
    private static final int COLUMN_GAP = 10;
    private static final int LINE_H = 10;
    private static final int STAT_H = 11;
    /** Same as the Sandevistan panel, so both tooltips are the same size. */
    private static final int MIN_WIDTH = 176;
    private static final int CORNER = 6;
    private static final float SMALL = 0.8F;
    private static final float TITLE_SCALE = 1.15F;
    private static final float NUMBER_SCALE = 1.5F;

    private MantisTooltipPanel() {
    }

    public static void render(GuiGraphics graphics, Font font, ItemStack stack, MantisVariant variant,
                              int mouseX, int mouseY, int screenW, int screenH) {
        long now = Util.getMillis();
        TooltipTabs.View view = TooltipTabs.update(stack);

        Component title = cyber(stack.getHoverName().copy());
        Component badge = cyber(MantisTooltipContent.badge());
        Component damage = cyber(MantisTooltipContent.damagePerHit());
        Component damageLabel = cyber(MantisTooltipContent.damagePerHitLabel());
        Component speed = cyber(MantisTooltipContent.attacksPerSecond());
        Component speedLabel = cyber(MantisTooltipContent.attacksPerSecondLabel());
        Component type = cyber(MantisTooltipContent.damageType(variant));
        Component typeLabel = cyber(MantisTooltipContent.damageTypeLabel());
        List<Component> description = MantisTooltipContent.descriptionLines(variant).stream().map(MantisTooltipPanel::cyber).toList();
        List<MantisTooltipContent.Section> sections = MantisTooltipContent.sections(variant).stream()
                .map(section -> new MantisTooltipContent.Section(cyber(section.header()),
                        section.lines().stream().map(MantisTooltipPanel::cyber).toList()))
                .toList();
        Component statsHeader = cyber(MantisTooltipContent.statsHeader());
        List<MantisTooltipContent.Stat> stats = MantisTooltipContent.stats(variant).stream()
                .map(stat -> new MantisTooltipContent.Stat(cyber(stat.label()), cyber(stat.value()), stat.separatedBefore()))
                .toList();
        Component note = cyber(MantisTooltipContent.note());
        Component brand = cyber(MantisTooltipContent.brand());
        Component key = cyber(TooltipKeybinds.CYCLE_TAB.getTranslatedKeyMessage());

        // ---- measure
        int titleW = Math.round(font.width(title) * TITLE_SCALE);
        int tagW = font.width(badge) + 8;
        int contentW = Math.max(MIN_WIDTH - PAD * 2, CARD_SIZE + 7 + Math.max(titleW, tagW));
        int damageW = Math.max(Math.round(font.width(damage) * NUMBER_SCALE), Math.round(font.width(damageLabel) * SMALL));
        int speedW = Math.max(Math.round(font.width(speed) * NUMBER_SCALE), Math.round(font.width(speedLabel) * SMALL));
        int typeW = Math.max(10 + font.width(type), Math.round(font.width(typeLabel) * SMALL));
        contentW = Math.max(contentW, damageW + speedW + typeW + COLUMN_GAP * 2);
        // Control and ability names form a column; their text starts at the same x.
        int sectionText = 0;
        for (MantisTooltipContent.Section section : sections) {
            sectionText = Math.max(sectionText, 9 + font.width(section.header()) + 6);
        }
        if (view.showDescription()) {
            for (Component line : description) {
                contentW = Math.max(contentW, font.width(line));
            }
            for (MantisTooltipContent.Section section : sections) {
                for (Component line : section.lines()) {
                    contentW = Math.max(contentW, sectionText + font.width(line));
                }
            }
        }
        if (view.showStats()) {
            contentW = Math.max(contentW, font.width(statsHeader) + 9);
            for (MantisTooltipContent.Stat stat : stats) {
                contentW = Math.max(contentW, font.width(stat.label()) + 12 + font.width(stat.value()));
            }
            contentW = Math.max(contentW, Math.round(font.width(note) * SMALL));
        }
        int panelW = contentW + PAD * 2;

        int bodyH = 0;
        if (view.showDescription()) {
            bodyH += description.size() * LINE_H + 5;
            for (MantisTooltipContent.Section section : sections) {
                bodyH += section.lines().size() * STAT_H;
            }
        }
        if (view.showStats()) {
            if (view.showDescription()) {
                bodyH += 8;
            }
            bodyH += LINE_H + 3;
            for (MantisTooltipContent.Stat stat : stats) {
                bodyH += (stat.separatedBefore() ? 6 : 0) + STAT_H;
            }
            bodyH += 10;
        }
        int panelH = 6 + HEADER_H + 8 + NUMBERS_H + 7 + bodyH + 9 + 13 + 5;

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + panelW > screenW - 6) {
            x = mouseX - panelW - 12;
        }
        x = Math.max(6, x);
        y = Math.max(6, Math.min(y, screenH - panelH - 6));

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 400.0F);

        drawFrame(graphics, x, y, panelW, panelH);
        motif(graphics, variant, x, y, panelW, panelH, now);

        int left = x + PAD;
        int right = x + panelW - PAD;
        int top = y + 6;

        // ---- header: item card, name and tag
        drawItemCard(graphics, stack, left, top);
        int textX = left + CARD_SIZE + 7;
        glitchTitle(graphics, font, title, textX, top + 2, now);
        int tagY = top + 17;
        drawTag(graphics, font, badge, textX, tagY, tagW);

        int cursor = top + HEADER_H + 3;
        graphics.fill(left, cursor, right, cursor + 1, RED_DIM);
        cursor += 5;

        // ---- big numbers, as on the game's weapon cards
        int spare = (right - left) - (damageW + speedW + typeW + COLUMN_GAP * 2);
        int speedX = left + damageW + COLUMN_GAP + spare / 2;
        int typeX = right - typeW;
        drawNumber(graphics, font, damage, damageLabel, left, cursor);
        drawNumber(graphics, font, speed, speedLabel, speedX, cursor);
        int typeColor = typeColor(variant);
        drawTypeIcon(graphics, variant, typeX, cursor + 2, typeColor);
        graphics.drawString(font, type, typeX + 10, cursor + 1, typeColor, false);
        drawScaled(graphics, font, typeLabel.getVisualOrderText(), typeX, cursor + 14, RED_SOFT);
        cursor += NUMBERS_H;
        graphics.fill(left, cursor, right, cursor + 1, RED_DIM);
        cursor += 7;

        // ---- description view: flavor, controls and ability
        if (view.showDescription()) {
            for (Component line : description) {
                graphics.drawString(font, line, left, cursor, YELLOW_SOFT, false);
                cursor += LINE_H;
            }
            cursor += 5;
            for (MantisTooltipContent.Section section : sections) {
                drawDiamond(graphics, left + 2, cursor + 4, 2, RED);
                graphics.drawString(font, section.header(), left + 9, cursor + 1, RED, false);
                for (Component line : section.lines()) {
                    graphics.drawString(font, line, left + sectionText, cursor + 1, YELLOW_SOFT, false);
                    cursor += STAT_H;
                }
            }
        }

        // ---- stats view
        if (view.showStats()) {
            if (view.showDescription()) {
                cursor += 8;
            }
            drawDiamond(graphics, left + 2, cursor + 3, 2, RED);
            graphics.drawString(font, statsHeader, left + 9, cursor, RED, false);
            cursor += LINE_H + 3;
            for (MantisTooltipContent.Stat stat : stats) {
                if (stat.separatedBefore()) {
                    graphics.fill(left, cursor + 1, right, cursor + 2, RED_DIM);
                    cursor += 6;
                }
                graphics.drawString(font, stat.label(), left, cursor + 1, RED_SOFT, false);
                graphics.drawString(font, stat.value(), right - font.width(stat.value()), cursor + 1, YELLOW, false);
                cursor += STAT_H;
            }
            drawScaled(graphics, font, note.getVisualOrderText(), left, cursor + 2, RED_SOFT);
        }

        // ---- footer
        int footerY = y + panelH - 5 - 13;
        graphics.fill(left, footerY - 4, right, footerY - 3, RED_DIM);
        drawScaled(graphics, font, brand.getVisualOrderText(), left, footerY + 4, RED_SOFT);
        if (view.tabsEnabled()) {
            int keyW = Math.max(13, font.width(key) + 6);
            int keyX = right - keyW - CORNER;
            graphics.fill(keyX, footerY + 10, keyX + keyW, footerY + 12, RED_DARK);
            graphics.fill(keyX, footerY, keyX + keyW, footerY + 10, YELLOW);
            graphics.drawString(font, key, keyX + (keyW - font.width(key)) / 2 + 1, footerY + 1, TAG_TEXT, false);
            int dotY = footerY + 5;
            drawDiamond(graphics, keyX - 16, dotY, 2, view.statsView() ? RED_DIM : CYAN);
            drawDiamond(graphics, keyX - 7, dotY, 2, view.statsView() ? CYAN : RED_DIM);
        }

        TooltipFx.scan(graphics, x, y, panelW, panelH, now, RED & 0xFFFFFF);
        if (variant == MantisVariant.ELECTRIC) {
            TooltipElectricity.draw(graphics, x, y, panelW, panelH, now);
        }
        pose.popPose();
    }

    private static Component cyber(Component text) {
        return SandevistanTooltipPanel.cyber(text);
    }

    private static void drawFrame(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x + 1, y + h, x + w + 1, y + h + 2, DROP_SHADOW);
        graphics.fillGradient(x, y, x + w, y + h, BACKGROUND_TOP, BACKGROUND_BOTTOM);
        // Salmon-red frame.
        graphics.fill(x, y, x + w, y + 1, RED);
        graphics.fill(x, y + h - 1, x + w, y + h, RED);
        graphics.fill(x, y, x + 1, y + h, RED);
        graphics.fill(x + w - 1, y, x + w, y + h, RED);
        // Cyan details: a stretch of the right edge and the filled bottom-right corner.
        graphics.fill(x + w - 1, y + 4, x + w, y + 18, CYAN);
        for (int i = 0; i < CORNER; i++) {
            graphics.fill(x + w - 1 - i, y + h - 1 - (CORNER - 1 - i), x + w - i, y + h - 1, CYAN);
        }
        graphics.fill(x + w - CORNER - 1, y + h - 1, x + w, y + h, CYAN);
    }

    private static void drawItemCard(GuiGraphics graphics, ItemStack stack, int left, int top) {
        int size = CARD_SIZE;
        graphics.fill(left, top, left + size, top + size, CARD);
        graphics.fill(left, top, left + size, top + 1, RED_DIM);
        graphics.fill(left, top + size - 1, left + size, top + size, RED_DIM);
        graphics.fill(left, top, left + 1, top + size, RED_DIM);
        graphics.fill(left + size - 1, top, left + size, top + size, RED_DIM);
        // Circuit-board traces behind the item, like the game's inventory tiles.
        int trace = RED_DARK;
        graphics.fill(left + 4, top + 8, left + 14, top + 9, trace);
        graphics.fill(left + 14, top + 8, left + 15, top + 20, trace);
        graphics.fill(left + 15, top + 20, left + 27, top + 21, trace);
        graphics.fill(left + 22, top + 4, left + 23, top + 14, trace);
        graphics.fill(left + 22, top + 14, left + 27, top + 15, trace);
        graphics.fill(left + 5, top + 25, left + 18, top + 26, trace);
        for (int i = 0; i < 4; i++) {
            graphics.fill(left + size - 1 - i, top + size - 1 - (3 - i), left + size - i, top + size - 1, CYAN);
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left + 1.0F, top + 1.0F, 0.0F);
        pose.scale(1.75F, 1.75F, 1.0F);
        graphics.renderItem(stack, 0, 0);
        pose.popPose();
    }

    /** Yellow tag with a cut corner, the game's label style. */
    private static void drawTag(GuiGraphics graphics, Font font, Component text, int x, int y, int w) {
        graphics.fill(x, y, x + w - 3, y + 10, YELLOW);
        for (int i = 0; i < 3; i++) {
            graphics.fill(x + w - 3 + i, y, x + w - 2 + i, y + 10 - (i + 1) * 2, YELLOW);
        }
        graphics.drawString(font, text, x + 4, y + 1, TAG_TEXT, false);
    }

    private static void drawNumber(GuiGraphics graphics, Font font, Component value, Component label, int x, int y) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(NUMBER_SCALE, NUMBER_SCALE, 1.0F);
        graphics.drawString(font, value, 0, 0, YELLOW, false);
        pose.popPose();
        drawScaled(graphics, font, label.getVisualOrderText(), x, y + 14, RED_SOFT);
    }

    /**
     * Cyberpunk-style chromatic glitch: the yellow name stays still and, a few times a minute,
     * splits into cyan and red copies with a sliced offset for a fraction of a second.
     */
    private static void glitchTitle(GuiGraphics graphics, Font font, Component title, int x, int y, long now) {
        long phase = now % 2600L;
        boolean glitch = phase < 140L;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
        if (glitch) {
            float jitter = (phase / 35L % 2 == 0) ? 1.2F : -0.8F;
            pose.pushPose();
            pose.translate(-jitter, 0.0F, 0.0F);
            graphics.drawString(font, title, 0, 0, TooltipFx.argb(CYAN, 0.55F), false);
            pose.popPose();
            pose.pushPose();
            pose.translate(jitter, 0.0F, 0.0F);
            graphics.drawString(font, title, 0, 0, TooltipFx.argb(RED, 0.55F), false);
            pose.popPose();
        } else {
            pose.pushPose();
            pose.translate(-0.6F, 0.4F, 0.0F);
            graphics.drawString(font, title, 0, 0, TooltipFx.argb(RED, 0.35F), false);
            pose.popPose();
        }
        graphics.drawString(font, title, 0, 0, YELLOW, false);
        pose.popPose();
    }

    private static void motif(GuiGraphics graphics, MantisVariant variant, int x, int y, int w, int h, long now) {
        switch (variant) {
            case THERMAL -> TooltipFx.particles(graphics, x, y, w, h, now, new int[]{0xFF8A3D, 0xFCEE0A, 0xF2565A}, 34, true);
            case ELECTRIC -> TooltipFx.particles(graphics, x, y, w, h, now, new int[]{0x7CE0EF, 0xFCEE0A}, 24, false);
            default -> TooltipFx.particles(graphics, x, y, w, h, now, new int[]{0xF2565A, 0xFCEE0A}, 16, false);
        }
    }

    private static int typeColor(MantisVariant variant) {
        return switch (variant) {
            case THERMAL -> 0xFFFF8A3D;
            case ELECTRIC -> 0xFF5BE4FF;
            default -> 0xFFE8E2C8;
        };
    }

    private static final String[] SLASH = {
            "......#",
            ".....#.",
            "....#..",
            "...#...",
            ".##....",
            ".#.....",
            "#......"};
    private static final String[] FLAME = {
            "...#...",
            "..##...",
            "..###..",
            ".#####.",
            ".##.##.",
            ".#...#.",
            "..###.."};
    private static final String[] BOLT = {
            "....##.",
            "...##..",
            "..##...",
            ".#####.",
            "...##..",
            "..##...",
            ".##...."};

    /** Seven-pixel damage type icon: a cut, a flame or a bolt. */
    private static void drawTypeIcon(GuiGraphics graphics, MantisVariant variant, int x, int y, int color) {
        String[] icon = switch (variant) {
            case THERMAL -> FLAME;
            case ELECTRIC -> BOLT;
            default -> SLASH;
        };
        for (int row = 0; row < icon.length; row++) {
            for (int col = 0; col < icon[row].length(); col++) {
                if (icon[row].charAt(col) == '#') {
                    graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }

    private static void drawDiamond(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = radius - Math.abs(dy);
            graphics.fill(centerX - half, centerY + dy, centerX + half + 1, centerY + dy + 1, color);
        }
    }

    private static void drawScaled(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(SMALL, SMALL, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        pose.popPose();
    }
}
