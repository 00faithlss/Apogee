package dev.apogee.client.compat.simplytooltips;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.berserk.BerserkVariant;
import dev.apogee.client.berserk.BerserkImplantRenderer;
import dev.apogee.client.tooltip.BerserkTooltipContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplytooltips.client.TooltipKeybinds;

/**
 * The Berserk tooltip, built like the Sandevistan's: translucent panel, metal frame, the 3D
 * implant in a rhombus, wavy title, particles, electric arcs and scanlines, with a Description
 * view and a Stats view toggled by the cycle key (G by default). Red instead of cyan.
 */
public final class BerserkTooltipPanel {
    private static final int ACCENT = 0xFFFF4B55;
    private static final int EDGE = 0xFF7A5C61;
    private static final int EDGE_INNER = 0xFF2A1A1D;
    private static final int BACKGROUND = 0xF2140E10;
    private static final int DROP_SHADOW = 0xC0090506;
    private static final int TEXT = 0xFFEDE6E7;
    private static final int MUTED = 0xFFB5A3A6;
    private static final int SECTION = 0xFFD9C5C8;
    private static final int DIVIDER = 0xFF47292D;
    private static final int BRAND = 0xFF9C8286;
    private static final int DOT_IDLE = 0xFF6B4E52;
    private static final int RHOMBUS_FILL = 0xFF1F1214;
    private static final int KEY_FACE = 0xFFE3D5D7;
    private static final int KEY_SHADOW = 0xFF7A5C61;
    private static final int KEY_TEXT = 0xFF1B1012;
    private static final int[] PARTICLES = {0xFF4B55, 0xFCEE0A, 0xFF8A3D};
    private static final int[] ARCS = {0xFF4B55, 0xFCEE0A, 0xFF8A3D};

    private static final int PAD = 8;
    private static final int HEADER_H = 42;
    private static final int MODEL_W = 36;
    private static final int LINE_H = 10;
    private static final int STAT_H = 11;
    private static final int MIN_WIDTH = 176;
    private static final int MAX_TEXT_W = 190;
    private static final float SMALL = 0.8F;
    private static final float TITLE_SCALE = 1.3F;

    private BerserkTooltipPanel() {
    }

    public static void render(GuiGraphics graphics, Font font, ItemStack stack, BerserkVariant variant,
                              int mouseX, int mouseY, int screenW, int screenH) {
        long now = Util.getMillis();
        TooltipTabs.View view = TooltipTabs.update(stack);
        boolean showDescription = view.showDescription();
        boolean showStats = view.showStats();

        Component title = SandevistanTooltipPanel.cyber(stack.getHoverName());
        Component badge = SandevistanTooltipPanel.cyber(BerserkTooltipContent.badge(variant));
        List<FormattedCharSequence> description = new ArrayList<>();
        for (Component line : BerserkTooltipContent.descriptionLines(variant)) {
            description.addAll(font.split(SandevistanTooltipPanel.cyber(line), MAX_TEXT_W));
        }
        List<BerserkTooltipContent.Stat> stats = BerserkTooltipContent.stats(variant).stream()
                .map(stat -> new BerserkTooltipContent.Stat(SandevistanTooltipPanel.cyber(stat.label()), SandevistanTooltipPanel.cyber(stat.value())))
                .toList();
        Component brand = SandevistanTooltipPanel.cyber(Component.translatable("tooltip.apogee.brand"));
        Component key = SandevistanTooltipPanel.cyber(TooltipKeybinds.CYCLE_TAB.getTranslatedKeyMessage());
        Component descriptionHeader = SandevistanTooltipPanel.cyber(Component.translatable("tooltip.apogee.berserk.description_header"));
        Component statsHeader = SandevistanTooltipPanel.cyber(BerserkTooltipContent.statsHeader());

        int titleW = Math.round(font.width(title) * TITLE_SCALE);
        int contentW = Math.max(MIN_WIDTH - PAD * 2, MODEL_W + 7 + Math.max(titleW, font.width(badge)));
        if (showDescription) {
            contentW = Math.max(contentW, font.width(descriptionHeader) + 9);
            for (FormattedCharSequence line : description) {
                contentW = Math.max(contentW, font.width(line));
            }
        }
        if (showStats) {
            contentW = Math.max(contentW, font.width(statsHeader) + 9);
            for (BerserkTooltipContent.Stat stat : stats) {
                contentW = Math.max(contentW, font.width(stat.label()) + 12 + font.width(stat.value()));
            }
        }
        int panelW = contentW + PAD * 2;

        int bodyH = 0;
        if (showDescription) {
            bodyH += LINE_H + 3 + description.size() * LINE_H;
        }
        if (showStats) {
            bodyH += LINE_H + 3 + stats.size() * STAT_H;
        }
        int panelH = 7 + HEADER_H + 10 + bodyH + 6 + 13 + 5;

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
        TooltipFx.particles(graphics, x, y, panelW, panelH, now, PARTICLES, 28, true);

        int top = y + 7;
        int left = x + PAD;
        drawModel(graphics, stack, left, top, now);
        int textX = left + MODEL_W + 7;
        TooltipFx.wavyTitle(graphics, font, title, textX, top + 8, TITLE_SCALE, now);
        graphics.drawString(font, badge, textX, top + 24, ACCENT, false);

        int cursor = top + HEADER_H + 3;
        graphics.fill(left, cursor, x + panelW - PAD, cursor + 1, DIVIDER);
        cursor += 7;

        if (showDescription) {
            cursor = drawSectionTitle(graphics, font, descriptionHeader, left, cursor);
            for (FormattedCharSequence line : description) {
                graphics.drawString(font, line, left, cursor, TEXT, false);
                cursor += LINE_H;
            }
        }
        if (showStats) {
            cursor = drawSectionTitle(graphics, font, statsHeader, left, cursor);
            int right = x + panelW - PAD;
            for (BerserkTooltipContent.Stat stat : stats) {
                graphics.drawString(font, stat.label(), left, cursor + 1, MUTED, false);
                graphics.drawString(font, stat.value(), right - font.width(stat.value()), cursor + 1, ACCENT, false);
                cursor += STAT_H;
            }
        }

        int footerY = y + panelH - 5 - 13;
        drawScaled(graphics, font, brand.getVisualOrderText(), left, footerY + 4, BRAND);
        if (view.tabsEnabled()) {
            int keyW = Math.max(13, font.width(key) + 6);
            int keyX = x + panelW - PAD - keyW;
            graphics.fill(keyX, footerY + 10, keyX + keyW, footerY + 12, KEY_SHADOW);
            graphics.fill(keyX, footerY, keyX + keyW, footerY + 10, KEY_FACE);
            graphics.drawString(font, key, keyX + (keyW - font.width(key)) / 2 + 1, footerY + 1, KEY_TEXT, false);
            int dotY = footerY + 5;
            drawDiamond(graphics, keyX - 16, dotY, 2, view.statsView() ? DOT_IDLE : ACCENT);
            drawDiamond(graphics, keyX - 7, dotY, 2, view.statsView() ? ACCENT : DOT_IDLE);
        }

        TooltipFx.scan(graphics, x, y, panelW, panelH, now, ACCENT & 0xFFFFFF);
        TooltipElectricity.draw(graphics, x, y, panelW, panelH, now, ARCS, 0x14FF4B55);

        pose.popPose();
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

    private static void drawModel(GuiGraphics graphics, ItemStack stack, int left, int top, long now) {
        int centerX = left + MODEL_W / 2;
        int centerY = top + HEADER_H / 2;
        drawDiamond(graphics, centerX, centerY, 17, EDGE);
        drawDiamond(graphics, centerX, centerY, 16, RHOMBUS_FILL);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        float bob = (float) Math.sin(now * 0.001) * 0.6F;
        pose.translate(centerX + 0.5F, centerY + bob, 150.0F);
        pose.scale(28.0F, -28.0F, 28.0F);
        pose.mulPose(Axis.YP.rotation((float) Math.sin(now * 0.00065) * 0.3F));
        pose.translate(-0.5F, -0.5F, -0.5F);
        Lighting.setupForFlatItems();
        RenderSystem.enableDepthTest();
        BerserkImplantRenderer.INSTANCE.renderForTooltip(stack, pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        graphics.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
    }
}
