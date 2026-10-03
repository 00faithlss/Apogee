package dev.apogee.client.compat.simplytooltips;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.apogee.Apogee;
import dev.apogee.registry.ApogeeItems;
import dev.apogee.client.tooltip.BloodPumpTooltipContent;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplytooltips.client.TooltipKeybinds;

/**
 * The Blood Pump's tooltip, laid out like the Sandevistan's: translucent black panel, metal frame
 * with red details, the implant inside a rhombus, a Description view and a Stats view toggled by Simply Tooltips'
 * own cycle key (G by default).
 */
public final class BloodPumpTooltipPanel {
    private static final int ACCENT = 0xFFFF4A57;
    private static final int EDGE = 0xFF666E76;
    private static final int EDGE_INNER = 0xFF202429;
    private static final int BACKGROUND = 0xF2101215;
    private static final int DROP_SHADOW = 0xC007090B;
    private static final int TEXT = 0xFFE8E9ED;
    private static final int MUTED = 0xFFA6ABB3;
    private static final int SECTION = 0xFFC6CBD2;
    private static final int DIVIDER = 0xFF343B43;
    private static final int BRAND = 0xFF90979F;
    private static final int DOT_IDLE = 0xFF5C626B;
    private static final int RHOMBUS_FILL = 0xFF171B20;
    private static final int KEY_FACE = 0xFFD9DFE2;
    private static final int KEY_SHADOW = 0xFF67727B;
    private static final int KEY_TEXT = 0xFF131619;

    private static final int[] DROPS = {0xFF4A57, 0xB3122B, 0xFF8A3D, 0xFFB3BC};
    private static final int[] ARCS = {0xFF4A57, 0xFF2A6D, 0xFF8A3D};
    private static final int PAD = 8;
    private static final int HEADER_H = 42;
    private static final int MODEL_W = 36;
    private static final int LINE_H = 10;
    private static final int STAT_H = 11;
    private static final int MIN_WIDTH = 176;
    private static final float SMALL = 0.8F;
    private static final float TITLE_SCALE = 1.3F;
    private static final ResourceLocation CYBER_FONT = Apogee.id("cyber");

    private BloodPumpTooltipPanel() {
    }

    public static void render(GuiGraphics graphics, Font font, ItemStack stack, int mouseX, int mouseY, int screenW, int screenH) {
        long now = Util.getMillis();
        TooltipTabs.View view = TooltipTabs.update(stack);
        boolean tabsEnabled = view.tabsEnabled();
        boolean statsView = view.statsView();
        boolean showDescription = view.showDescription();
        boolean showStats = view.showStats();

        Component title = cyber(BloodPumpTooltipContent.title());
        Component badge = cyber(BloodPumpTooltipContent.badge());
        List<Component> description = BloodPumpTooltipContent.descriptionLines().stream().map(BloodPumpTooltipPanel::cyber).toList();
        List<BloodPumpTooltipContent.Stat> stats = BloodPumpTooltipContent.stats().stream()
                .map(stat -> new BloodPumpTooltipContent.Stat(cyber(stat.label()), cyber(stat.value()), stat.separatedBefore()))
                .toList();
        Component note = cyber(BloodPumpTooltipContent.note());
        Component brand = cyber(BloodPumpTooltipContent.brand());
        Component key = cyber(TooltipKeybinds.CYCLE_TAB.getTranslatedKeyMessage());
        Component descriptionHeader = cyber(BloodPumpTooltipContent.descriptionHeader());
        Component statsHeader = cyber(BloodPumpTooltipContent.statsHeader());

        int titleW = Math.round(font.width(title) * TITLE_SCALE);
        int contentW = Math.max(MIN_WIDTH - PAD * 2, MODEL_W + 7 + Math.max(titleW, font.width(badge)));
        if (showDescription) {
            contentW = Math.max(contentW, font.width(descriptionHeader) + 9);
            for (Component line : description) {
                contentW = Math.max(contentW, font.width(line));
            }
        }
        if (showStats) {
            contentW = Math.max(contentW, font.width(statsHeader) + 9);
            for (BloodPumpTooltipContent.Stat stat : stats) {
                contentW = Math.max(contentW, font.width(stat.label()) + 12 + font.width(stat.value()));
            }
            contentW = Math.max(contentW, Math.round(font.width(note) * SMALL));
        }
        int panelW = contentW + PAD * 2;

        int bodyH = 0;
        if (showDescription) {
            bodyH += LINE_H + 3 + description.size() * LINE_H;
        }
        if (showStats) {
            if (showDescription) {
                bodyH += 8;
            }
            bodyH += LINE_H + 3;
            for (BloodPumpTooltipContent.Stat stat : stats) {
                bodyH += (stat.separatedBefore() ? 6 : 0) + STAT_H;
            }
            bodyH += 9;
        }
        int panelH = 7 + HEADER_H + 10 + bodyH + 8 + 13 + 5;

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

        float beat = heartbeat(now);
        drawFrame(graphics, x, y, panelW, panelH);
        TooltipFx.particles(graphics, x, y, panelW, panelH, now, DROPS, 30, false);

        int top = y + 7;
        int left = x + PAD;
        drawModel(graphics, left, top, now, beat);
        int textX = left + MODEL_W + 7;
        TooltipFx.wavyTitle(graphics, font, title, textX, top + 8, TITLE_SCALE, now, new int[]{0xFF2A6D, 0xFF8A3D, 0xFF4A57}, 0xFF4A57, 0xFFB3BC);
        graphics.drawString(font, badge, textX, top + 24, ACCENT, false);

        int cursor = top + HEADER_H + 3;
        drawEcg(graphics, left, x + panelW - PAD, cursor, now);
        cursor += 7;

        if (showDescription) {
            cursor = drawSectionTitle(graphics, font, descriptionHeader, left, cursor);
            for (Component line : description) {
                graphics.drawString(font, line, left, cursor, TEXT, false);
                cursor += LINE_H;
            }
        }
        if (showStats) {
            if (showDescription) {
                cursor += 8;
            }
            cursor = drawSectionTitle(graphics, font, statsHeader, left, cursor);
            int right = x + panelW - PAD;
            for (BloodPumpTooltipContent.Stat stat : stats) {
                if (stat.separatedBefore()) {
                    graphics.fill(left, cursor + 1, right, cursor + 2, DIVIDER);
                    cursor += 6;
                }
                graphics.drawString(font, stat.label(), left, cursor + 1, MUTED, false);
                graphics.drawString(font, stat.value(), right - font.width(stat.value()), cursor + 1, ACCENT, false);
                cursor += STAT_H;
            }
            drawScaled(graphics, font, note.getVisualOrderText(), left, cursor + 1, MUTED);
            cursor += 9;
        }

        int footerY = y + panelH - 5 - 13;
        drawScaled(graphics, font, brand.getVisualOrderText(), left, footerY + 4, BRAND);
        if (tabsEnabled) {
            int keyW = Math.max(13, font.width(key) + 6);
            int keyX = x + panelW - PAD - keyW;
            graphics.fill(keyX, footerY + 10, keyX + keyW, footerY + 12, KEY_SHADOW);
            graphics.fill(keyX, footerY, keyX + keyW, footerY + 10, KEY_FACE);
            graphics.drawString(font, key, keyX + (keyW - font.width(key)) / 2 + 1, footerY + 1, KEY_TEXT, false);
            int dotY = footerY + 5;
            drawDiamond(graphics, keyX - 16, dotY, 2, statsView ? DOT_IDLE : ACCENT);
            drawDiamond(graphics, keyX - 7, dotY, 2, statsView ? ACCENT : DOT_IDLE);
        }


        TooltipFx.scan(graphics, x, y, panelW, panelH, now, ACCENT & 0xFFFFFF);
        TooltipElectricity.draw(graphics, x, y, panelW, panelH, now, ARCS, TooltipFx.argb(0xFF4A57, 0.08F + 0.2F * beat));

        pose.popPose();
    }

    /** Rajdhani, a Cyberpunk-style typeface, with Minecraft's font as fallback for missing glyphs. */
    static Component cyber(Component text) {
        return text.copy().withStyle(style -> style.withFont(CYBER_FONT));
    }

    private static void drawFrame(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x + 1, y + h, x + w + 1, y + h + 2, DROP_SHADOW);
        graphics.fill(x, y, x + w, y + h, BACKGROUND);
        graphics.fill(x, y, x + w, y + 1, EDGE);
        graphics.fill(x, y + h - 1, x + w, y + h, EDGE);
        graphics.fill(x, y, x + 1, y + h, EDGE);
        graphics.fill(x + w - 1, y, x + w, y + h, EDGE);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, EDGE_INNER);
        graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, EDGE_INNER);
        graphics.fill(x + 1, y + 2, x + 2, y + h - 2, EDGE_INNER);
        graphics.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, EDGE_INNER);
        graphics.fill(x + 7, y, x + 15, y + 1, ACCENT);
        graphics.fill(x + w - 15, y, x + w - 7, y + 1, ACCENT);
    }

    private static int drawSectionTitle(GuiGraphics graphics, Font font, Component text, int left, int cursor) {
        drawDiamond(graphics, left + 2, cursor + 3, 2, ACCENT);
        graphics.drawString(font, text, left + 9, cursor, SECTION, false);
        return cursor + LINE_H + 3;
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

    /** A double beat, lub-dub, every 1.1 s: 1 at each beat, falling off quickly in between. */
    private static float heartbeat(long now) {
        float t = (now % 1100L) / 1000.0F;
        return Math.max(pulse(t, 0.0F), 0.7F * pulse(t, 0.22F));
    }

    private static float pulse(float t, float at) {
        float d = t - at;
        return d < 0.0F ? 0.0F : (float) Math.exp(-d * 14.0F);
    }

    /** The divider as a heart monitor trace: a flat line with a P-QRS-T complex scrolling along it. */
    private static void drawEcg(GuiGraphics graphics, int x0, int x1, int baseline, long now) {
        graphics.fill(x0, baseline, x1, baseline + 1, DIVIDER);
        int period = 72;
        int scroll = (int) (now * 0.05F);
        int previous = baseline;
        for (int px = x0; px < x1; px++) {
            int t = Math.floorMod(px - x0 - scroll, period);
            int dy = ecg(t);
            float head = (px - x0) / (float) Math.max(1, x1 - x0);
            int color = TooltipFx.argb(0xFF4A57, 0.35F + 0.5F * head);
            int y = baseline + dy;
            int from = Math.min(previous, y);
            int to = Math.max(previous, y);
            graphics.fill(px, from, px + 1, to + 1, color);
            previous = y;
        }
    }

    /** Height of the trace at {@code t} pixels into one beat; negative is up. */
    private static int ecg(int t) {
        if (t >= 6 && t < 12) {
            return -Math.round(2.0F * (float) Math.sin((t - 6) / 6.0F * Math.PI));
        }
        if (t == 20) {
            return 1;
        }
        if (t == 21) {
            return -3;
        }
        if (t == 22) {
            return -6;
        }
        if (t == 23) {
            return 2;
        }
        if (t == 24) {
            return 3;
        }
        if (t >= 32 && t < 44) {
            return -Math.round(3.0F * (float) Math.sin((t - 32) / 12.0F * Math.PI));
        }
        return 0;
    }

    private static void drawModel(GuiGraphics graphics, int left, int top, long now, float beat) {
        int centerX = left + MODEL_W / 2;
        int centerY = top + HEADER_H / 2;
        drawDiamond(graphics, centerX, centerY, 17, EDGE);
        drawDiamond(graphics, centerX, centerY, 16, RHOMBUS_FILL);
        // A red glow behind the implant that swells with each beat.
        for (int radius = 14; radius >= 4; radius -= 4) {
            drawDiamond(graphics, centerX, centerY, radius, TooltipFx.argb(0xFF4A57, (0.03F + 0.10F * beat) * (15 - radius) / 10.0F));
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        float bob = (float) Math.sin(now * 0.001) * 0.6F;
        float scale = 1.5F + 0.12F * beat;
        pose.translate(centerX + 0.5F, centerY + bob, 150.0F);
        pose.mulPose(com.mojang.math.Axis.ZP.rotation((float) Math.sin(now * 0.0007) * 0.06F));
        pose.scale(scale, scale, 1.0F);
        pose.translate(-8.0F, -8.0F, 0.0F);
        graphics.renderItem(new ItemStack(ApogeeItems.BLOOD_PUMP), 0, 0);
        pose.popPose();
    }
}
