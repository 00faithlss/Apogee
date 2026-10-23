package dev.apogee.client.compat.simplytooltips;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.Apogee;
import dev.apogee.client.tooltip.TendonsTooltipContent;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplytooltips.client.TooltipKeybinds;

/**
 * The Reinforced Tendons card: the same layout and size as the Sandevistan's (translucent black
 * panel, metal frame, a Description view and a Stats view switched with Simply Tooltips' cycle
 * key, G by default), in orange, with the item's own 3D model turning in the rhombus.
 */
public final class TendonsTooltipPanel {
    private static final int ACCENT = 0xFFFFA11A;
    /** Steam and sparks: amber, pale gold and hot orange. */
    private static final int[] SPARKS = {0xFFA11A, 0xFFD36B, 0xFF5A1F};
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

    private static final int PAD = 8;
    private static final int HEADER_H = 42;
    private static final int MODEL_W = 36;
    private static final int LINE_H = 10;
    private static final int STAT_H = 11;
    /** Same as the Sandevistan and Mantis panels, so every Apogee tooltip is the same width. */
    private static final int MIN_WIDTH = 176;
    private static final float SMALL = 0.8F;
    private static final float TITLE_SCALE = 1.3F;
    private static final ResourceLocation CYBER_FONT = Apogee.id("cyber");

    private TendonsTooltipPanel() {
    }

    public static void render(GuiGraphics graphics, Font font, ItemStack stack, int mouseX, int mouseY, int screenW, int screenH) {
        long now = Util.getMillis();
        TooltipTabs.View view = TooltipTabs.update(stack);
        boolean tabsEnabled = view.tabsEnabled();
        boolean statsView = view.statsView();

        Component title = cyber(TendonsTooltipContent.title());
        Component badge = cyber(TendonsTooltipContent.badge());
        List<Component> description = TendonsTooltipContent.descriptionLines().stream().map(TendonsTooltipPanel::cyber).toList();
        List<TendonsTooltipContent.Stat> stats = TendonsTooltipContent.stats().stream()
                .map(stat -> new TendonsTooltipContent.Stat(cyber(stat.label()), cyber(stat.value()), stat.separatedBefore()))
                .toList();
        Component note = cyber(TendonsTooltipContent.note());
        Component brand = cyber(TendonsTooltipContent.brand());
        Component key = cyber(TooltipKeybinds.CYCLE_TAB.getTranslatedKeyMessage());
        Component descriptionHeader = cyber(TendonsTooltipContent.descriptionHeader());
        Component statsHeader = cyber(TendonsTooltipContent.statsHeader());

        int titleW = Math.round(font.width(title) * TITLE_SCALE);
        int contentW = Math.max(MIN_WIDTH - PAD * 2, MODEL_W + 7 + Math.max(titleW, font.width(badge)));
        if (view.showDescription()) {
            contentW = Math.max(contentW, font.width(descriptionHeader) + 9);
            for (Component line : description) {
                contentW = Math.max(contentW, font.width(line));
            }
        }
        if (view.showStats()) {
            contentW = Math.max(contentW, font.width(statsHeader) + 9);
            for (TendonsTooltipContent.Stat stat : stats) {
                contentW = Math.max(contentW, font.width(stat.label()) + 12 + font.width(stat.value()));
            }
            contentW = Math.max(contentW, Math.round(font.width(note) * SMALL));
        }
        int panelW = contentW + PAD * 2;

        int bodyH = 0;
        if (view.showDescription()) {
            bodyH += LINE_H + 3 + description.size() * LINE_H;
        }
        if (view.showStats()) {
            if (view.showDescription()) {
                bodyH += 8;
            }
            bodyH += LINE_H + 3;
            for (TendonsTooltipContent.Stat stat : stats) {
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

        drawFrame(graphics, x, y, panelW, panelH);
        TooltipFx.particles(graphics, x, y, panelW, panelH, now, SPARKS, 40, true);

        int top = y + 7;
        int left = x + PAD;
        drawModel(graphics, stack, left, top, now);
        int textX = left + MODEL_W + 7;
        TooltipFx.wavyTitle(graphics, font, title, textX, top + 8, TITLE_SCALE, now);
        graphics.drawString(font, badge, textX, top + 24, ACCENT, false);

        int cursor = top + HEADER_H + 3;
        graphics.fill(left, cursor, x + panelW - PAD, cursor + 1, DIVIDER);
        cursor += 7;

        if (view.showDescription()) {
            cursor = drawSectionTitle(graphics, font, descriptionHeader, left, cursor);
            for (Component line : description) {
                graphics.drawString(font, line, left, cursor, TEXT, false);
                cursor += LINE_H;
            }
        }
        if (view.showStats()) {
            if (view.showDescription()) {
                cursor += 8;
            }
            cursor = drawSectionTitle(graphics, font, statsHeader, left, cursor);
            int right = x + panelW - PAD;
            for (TendonsTooltipContent.Stat stat : stats) {
                if (stat.separatedBefore()) {
                    graphics.fill(left, cursor + 1, right, cursor + 2, DIVIDER);
                    cursor += 6;
                }
                graphics.drawString(font, stat.label(), left, cursor + 1, MUTED, false);
                graphics.drawString(font, stat.value(), right - font.width(stat.value()), cursor + 1, ACCENT, false);
                cursor += STAT_H;
            }
            drawScaled(graphics, font, note.getVisualOrderText(), left, cursor + 1, MUTED);
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

        TooltipFx.scan(graphics, x, y, panelW, panelH, now, SPARKS[0]);
        TooltipElectricity.draw(graphics, x, y, panelW, panelH, now, SPARKS, 0x14FFA11A);
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

    /** The implant's own 3D item model, slowly turning inside the rhombus, like the Sandevistan's implant. */
    private static void drawModel(GuiGraphics graphics, ItemStack stack, int left, int top, long now) {
        int centerX = left + MODEL_W / 2;
        int centerY = top + HEADER_H / 2;
        drawDiamond(graphics, centerX, centerY, 17, EDGE);
        drawDiamond(graphics, centerX, centerY, 16, RHOMBUS_FILL);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        float bob = (float) Math.sin(now * 0.001) * 0.6F;
        pose.translate(centerX + 0.5F, centerY + bob, 150.0F);
        pose.scale(40.0F, -40.0F, 40.0F);
        pose.mulPose(Axis.XP.rotation(0.45F));
        pose.mulPose(Axis.YP.rotation(-1.15F + (float) Math.sin(now * 0.0007) * 0.5F));
        pose.mulPose(Axis.ZP.rotation(0.55F));
        Lighting.setupForFlatItems();
        RenderSystem.enableDepthTest();
        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer().render(stack, ItemDisplayContext.FIXED, false, pose, graphics.bufferSource(),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, mc.getItemRenderer().getModel(stack, null, null, 0));
        graphics.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
    }
}
