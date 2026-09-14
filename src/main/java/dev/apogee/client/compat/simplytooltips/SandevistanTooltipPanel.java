package dev.apogee.client.compat.simplytooltips;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.apogee.Apogee;
import dev.apogee.client.render.SandevistanImplantRenderer;
import dev.apogee.client.tooltip.SandevistanTooltipContent;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplytooltips.client.TooltipKeybinds;
import net.sweenus.simplytooltips.client.TooltipNavigationConfig;
import net.sweenus.simplytooltips.client.render.TabState;

/**
 * The approved Apogee tooltip: translucent black panel, metal frame with cyan details, the 3D
 * implant inside a rhombus, a Description view and a Stats view toggled by Simply Tooltips'
 * own cycle key (G by default).
 */
public final class SandevistanTooltipPanel {
    private static final int ACCENT = 0xFF78E2DF;
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
    private static final int MIN_WIDTH = 176;
    private static final float SMALL = 0.8F;
    private static final float TITLE_SCALE = 1.3F;
    private static final ResourceLocation CYBER_FONT = Apogee.id("cyber");

    private static final ResourceLocation RESET_KEY = Apogee.id("tooltip_left");
    private static final List<TabState.Tab> TABS = List.of(TabState.Tab.LORE, TabState.Tab.STATS);
    private static final long HIDDEN_AFTER_MS = 150L;
    private static long lastRenderMs;
    private static boolean ownsTabs;

    private SandevistanTooltipPanel() {
    }

    public static void render(GuiGraphics graphics, Font font, ItemStack stack, int mouseX, int mouseY, int screenW, int screenH) {
        boolean tabsEnabled = TooltipNavigationConfig.tooltipTabs();
        long now = Util.getMillis();
        if (tabsEnabled) {
            if (now - lastRenderMs > HIDDEN_AFTER_MS) {
                // Hovering away and back always opens on the description.
                TabState.notifyItem(RESET_KEY, TABS);
            }
            TabState.notifyItem(BuiltInRegistries.ITEM.getKey(stack.getItem()), TABS);
            ownsTabs = true;
        }
        lastRenderMs = now;
        boolean statsView = tabsEnabled && TabState.activeTab() == TabState.Tab.STATS;
        boolean showDescription = !tabsEnabled || !statsView;
        boolean showStats = !tabsEnabled || statsView;

        Component title = cyber(SandevistanTooltipContent.title());
        Component badge = cyber(SandevistanTooltipContent.badge());
        List<Component> description = SandevistanTooltipContent.descriptionLines().stream().map(SandevistanTooltipPanel::cyber).toList();
        List<SandevistanTooltipContent.Stat> stats = SandevistanTooltipContent.stats().stream()
                .map(stat -> new SandevistanTooltipContent.Stat(cyber(stat.label()), cyber(stat.value()), stat.separatedBefore()))
                .toList();
        Component note = cyber(SandevistanTooltipContent.cooldownNote());
        Component brand = cyber(SandevistanTooltipContent.brand());
        Component key = cyber(TooltipKeybinds.CYCLE_TAB.getTranslatedKeyMessage());
        Component descriptionHeader = cyber(SandevistanTooltipContent.descriptionHeader());
        Component statsHeader = cyber(SandevistanTooltipContent.statsHeader());

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
            for (SandevistanTooltipContent.Stat stat : stats) {
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
            for (SandevistanTooltipContent.Stat stat : stats) {
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
        TooltipElectricity.draw(graphics, x, y, panelW, panelH, now);

        int top = y + 7;
        int left = x + PAD;
        drawModel(graphics, left, top, now);
        int textX = left + MODEL_W + 7;
        pose.pushPose();
        pose.translate(textX, top + 8, 0.0F);
        pose.scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
        graphics.drawString(font, title, 0, 0, TEXT, true);
        pose.popPose();
        graphics.drawString(font, badge, textX, top + 24, ACCENT, false);

        int cursor = top + HEADER_H + 3;
        graphics.fill(left, cursor, x + panelW - PAD, cursor + 1, DIVIDER);
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
            for (SandevistanTooltipContent.Stat stat : stats) {
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

        pose.popPose();
    }

    /** Rajdhani, a Cyberpunk-style typeface, with Minecraft's font as fallback for missing glyphs. */
    private static Component cyber(Component text) {
        return text.copy().withStyle(style -> style.withFont(CYBER_FONT));
    }

    /** Clears the shared tab list once the Sandevistan tooltip is gone so G is not swallowed elsewhere. */
    static void releaseTabsIfHidden() {
        if (ownsTabs && Util.getMillis() - lastRenderMs > HIDDEN_AFTER_MS) {
            TabState.notifyItem(RESET_KEY, List.of());
            ownsTabs = false;
        }
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

    private static void drawModel(GuiGraphics graphics, int left, int top, long now) {
        int centerX = left + MODEL_W / 2;
        int centerY = top + HEADER_H / 2;
        drawDiamond(graphics, centerX, centerY, 17, EDGE);
        drawDiamond(graphics, centerX, centerY, 16, RHOMBUS_FILL);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        float bob = (float) Math.sin(now * 0.001) * 0.6F;
        pose.translate(centerX + 0.5F, centerY + bob, 150.0F);
        pose.scale(34.0F, -34.0F, 34.0F);
        pose.mulPose(Axis.XP.rotation(0.06F));
        pose.mulPose(Axis.YP.rotation(-0.4F + (float) Math.sin(now * 0.00065) * 0.18F));
        pose.mulPose(Axis.ZP.rotation(0.18F));
        Lighting.setupForFlatItems();
        RenderSystem.enableDepthTest();
        SandevistanImplantRenderer.INSTANCE.renderCentered(pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, false);
        graphics.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
    }
}
