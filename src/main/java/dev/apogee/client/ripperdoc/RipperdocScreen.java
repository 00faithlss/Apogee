package dev.apogee.client.ripperdoc;

import dev.apogee.ripperdoc.RipperdocMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The ripperdoc screen, laid out like a chest: on top a dark monitor with a view of the patient
 * between the implant slots, under it the usual inventory. The patient in the view is the player's
 * own model with whatever cyberware is fitted, and turns when dragged; in x-ray mode the skin turns
 * see-through and only the cyberware is drawn on it. Everything is drawn at the normal GUI size.
 */
public final class RipperdocScreen extends AbstractContainerScreen<RipperdocMenu> {
    private static final ResourceLocation CHEST = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int PANEL = RipperdocMenu.PANEL_HEIGHT;
    /** The monitor inside the chest-like frame. */
    private static final int MONITOR_X = 7;
    private static final int MONITOR_Y = 16;
    private static final int MONITOR_W = 162;
    private static final int MONITOR_H = PANEL - MONITOR_Y - 3;
    /** The patient view in the middle of the monitor. */
    private static final int VIEW_X = 57;
    private static final int VIEW_Y = 19;
    private static final int VIEW_W = 62;
    private static final int VIEW_H = 92;
    /** The x-ray and skin buttons under the view. */
    private static final int BUTTON_Y = VIEW_Y + VIEW_H + 2;
    private static final int BUTTON_W = 30;
    private static final int BUTTON_H = 12;
    private static final int XRAY_X = VIEW_X;
    private static final int SKIN_X = VIEW_X + VIEW_W - BUTTON_W;
    /** The side columns, each with one implant slot. */
    private static final int COLUMN_W = 44;
    private static final int LEFT_X = 10;
    private static final int RIGHT_X = 122;

    private static final int MONITOR = 0xFF0B1219;
    private static final int VIEW_BG = 0xFF0F1D26;
    private static final int FRAME = 0xFF2A5563;
    private static final int PINK = 0xFFE6567A;
    private static final int CYAN = 0xFF7CE0EF;
    private static final int GREY = 0xFF8FA3AD;
    private static final int WHITE = 0xFFE6F1F4;
    /** Standing height of a player: the view frames the patient upright whatever their pose. */
    private static final float PATIENT_HEIGHT = EntityType.PLAYER.getHeight();

    /** True while the patient view draws the player, so the couch pose stays off that model. */
    public static boolean renderingPatient;
    /** Whether the view is in x-ray mode; kept between visits to the table. */
    private static boolean xray;

    private float yaw = -25.0F;
    private float pitch = 0.0F;
    private boolean dragging;

    public RipperdocScreen(RipperdocMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = RipperdocMenu.WIDTH;
        imageHeight = RipperdocMenu.HEIGHT;
        inventoryLabelY = imageHeight - 94;
    }

    /** True while the patient view draws the player under x-ray: see-through skin, cyberware only. */
    public static boolean xrayPatient() {
        return renderingPatient && xray;
    }

    public static boolean xray() {
        return xray;
    }

    /** Switches the view between x-ray and skin, as the buttons do. */
    public void setXray(boolean on) {
        if (xray != on) {
            xray = on;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty() && hoveredSlot instanceof RipperdocMenu.ImplantView && !hoveredSlot.hasItem()) {
            // An empty implant slot names its body system and what fitting costs.
            graphics.renderComponentTooltip(font, List.of(slotName(hoveredSlot),
                    Component.translatable("screen.apogee.ripperdoc.surgery").withStyle(net.minecraft.ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // A chest's frame: its top edge, one row of its side edges stretched down the panel, and
        // its inventory part as is.
        graphics.blit(CHEST, x, y, 0, 0, imageWidth, 4);
        graphics.blit(CHEST, x, y + 4, imageWidth, PANEL - 4, 0.0F, 4.0F, imageWidth, 1, 256, 256);
        graphics.blit(CHEST, x, y + PANEL, 0, 126, imageWidth, 96);

        // The monitor.
        graphics.fill(x + MONITOR_X, y + MONITOR_Y, x + MONITOR_X + MONITOR_W, y + MONITOR_Y + MONITOR_H, MONITOR);
        graphics.renderOutline(x + MONITOR_X, y + MONITOR_Y, MONITOR_W, MONITOR_H, FRAME);
        corners(graphics, x + MONITOR_X, y + MONITOR_Y, MONITOR_W, MONITOR_H, 5, CYAN);

        // The patient view and its buttons.
        graphics.fill(x + VIEW_X, y + VIEW_Y, x + VIEW_X + VIEW_W, y + VIEW_Y + VIEW_H, VIEW_BG);
        graphics.renderOutline(x + VIEW_X, y + VIEW_Y, VIEW_W, VIEW_H, FRAME);
        corners(graphics, x + VIEW_X + 2, y + VIEW_Y + 2, VIEW_W - 4, VIEW_H - 4, 4, CYAN);
        renderPatient(graphics);
        button(graphics, XRAY_X, "screen.apogee.ripperdoc.xray", xray, mouseX, mouseY);
        button(graphics, SKIN_X, "screen.apogee.ripperdoc.skin", !xray, mouseX, mouseY);

        // Frames of the implant slots.
        for (Slot slot : menu.slots) {
            if (slot instanceof RipperdocMenu.ImplantView) {
                graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, VIEW_BG);
                graphics.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, slot.hasItem() ? CYAN : FRAME);
            }
        }
    }

    /** Short cyan brackets on the corners of a box, as on the author's design. */
    private static void corners(GuiGraphics graphics, int x, int y, int w, int h, int length, int color) {
        graphics.fill(x, y, x + length, y + 1, color);
        graphics.fill(x, y, x + 1, y + length, color);
        graphics.fill(x + w - length, y, x + w, y + 1, color);
        graphics.fill(x + w - 1, y, x + w, y + length, color);
        graphics.fill(x, y + h - 1, x + length, y + h, color);
        graphics.fill(x, y + h - length, x + 1, y + h, color);
        graphics.fill(x + w - length, y + h - 1, x + w, y + h, color);
        graphics.fill(x + w - 1, y + h - length, x + w, y + h, color);
    }

    private void button(GuiGraphics graphics, int buttonX, String key, boolean active, int mouseX, int mouseY) {
        int x = leftPos + buttonX;
        int y = topPos + BUTTON_Y;
        boolean hovered = onButton(mouseX, mouseY, buttonX);
        graphics.fill(x, y, x + BUTTON_W, y + BUTTON_H, active ? 0xFF1C4A55 : hovered ? 0xFF16313B : 0xFF112029);
        graphics.renderOutline(x, y, BUTTON_W, BUTTON_H, active ? CYAN : FRAME);
        small(graphics, Component.translatable(key), x + BUTTON_W / 2.0F, y + 3.5F, 0.65F, active ? WHITE : GREY, true);
    }

    /** The player's own model in the view, turned by dragging, with the fitted cyberware on it. */
    private void renderPatient(GuiGraphics graphics) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        int x0 = leftPos + VIEW_X + 1;
        int y0 = topPos + VIEW_Y + 1;
        graphics.enableScissor(x0, y0, x0 + VIEW_W - 2, y0 + VIEW_H - 2);
        float bodyRot = player.yBodyRot;
        float bodyRotO = player.yBodyRotO;
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float headRotO = player.yHeadRotO;
        float headRot = player.yHeadRot;
        player.yBodyRot = 180.0F + yaw;
        player.yBodyRotO = player.yBodyRot;
        player.setYRot(180.0F + yaw);
        player.setXRot(0.0F);
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();
        Quaternionf pose = new Quaternionf().rotateZ(Mth.PI).rotateX(pitch * Mth.DEG_TO_RAD);
        float size = VIEW_H * 0.62F / PATIENT_HEIGHT;
        renderingPatient = true;
        try {
            InventoryScreen.renderEntityInInventory(graphics, leftPos + VIEW_X + VIEW_W / 2.0F, topPos + VIEW_Y + VIEW_H / 2.0F + 2.0F, size,
                    new Vector3f(0.0F, PATIENT_HEIGHT / 2.0F + 0.0625F, 0.0F), pose, null, player);
        } finally {
            renderingPatient = false;
            player.yBodyRot = bodyRot;
            player.yBodyRotO = bodyRotO;
            player.setYRot(yRot);
            player.setXRot(xRot);
            player.yHeadRotO = headRotO;
            player.yHeadRot = headRot;
        }
        graphics.disableScissor();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        boolean fitted = false;
        for (Slot slot : menu.slots) {
            if (slot instanceof RipperdocMenu.ImplantView) {
                int columnX = slot.x < VIEW_X ? LEFT_X : RIGHT_X;
                // Body system over the slot, what is fitted under it.
                wrapped(graphics, slotName(slot).copy().withStyle(net.minecraft.ChatFormatting.BOLD), columnX, 22, 0.6F, PINK, true);
                Component fittedName = slot.hasItem() ? slot.getItem().getHoverName() : Component.translatable("screen.apogee.ripperdoc.empty");
                wrapped(graphics, fittedName, columnX, slot.y + 20, 0.6F, slot.hasItem() ? CYAN : GREY, true);
                fitted |= slot.hasItem();
            }
        }
        wrapped(graphics, Component.translatable("screen.apogee.ripperdoc.surgery"), LEFT_X, 98, 0.55F, GREY, false);
        wrapped(graphics, Component.translatable("screen.apogee.ripperdoc.rotate"), RIGHT_X, 104, 0.55F, GREY, false);
        if (xray && !fitted) {
            wrapped(graphics, Component.translatable("screen.apogee.ripperdoc.xray_empty"), VIEW_X + 3, VIEW_Y + VIEW_H - 12, 0.55F, PINK, true);
        }
    }

    private Component slotName(Slot slot) {
        return Component.translatable("screen.apogee.ripperdoc.slot." + ((RipperdocMenu.ImplantView) slot).implant().slot);
    }

    /** Small text, centred on {@code x} when asked. */
    private void small(GuiGraphics graphics, Component text, float x, float y, float scale, int color, boolean centred) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, centred ? -font.width(text) / 2 : 0, 0, color, false);
        graphics.pose().popPose();
    }

    /** Small text wrapped to a side column's width (or the view's), each line centred when asked. */
    private void wrapped(GuiGraphics graphics, Component text, int x, int y, float scale, int color, boolean centred) {
        int width = x >= VIEW_X && x < VIEW_X + VIEW_W ? VIEW_W - 6 : COLUMN_W;
        int lineWidth = Math.round(width / scale);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        int line = 0;
        for (FormattedCharSequence part : font.split(text, lineWidth)) {
            int offset = centred ? (lineWidth - font.width(part)) / 2 : 0;
            graphics.drawString(font, part, offset, line, color, false);
            line += font.lineHeight;
        }
        graphics.pose().popPose();
    }

    private boolean onButton(double x, double y, int buttonX) {
        return x >= leftPos + buttonX && x < leftPos + buttonX + BUTTON_W && y >= topPos + BUTTON_Y && y < topPos + BUTTON_Y + BUTTON_H;
    }

    private boolean inView(double x, double y) {
        return x >= leftPos + VIEW_X && x < leftPos + VIEW_X + VIEW_W && y >= topPos + VIEW_Y && y < topPos + VIEW_Y + VIEW_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && onButton(mouseX, mouseY, XRAY_X)) {
            setXray(true);
            return true;
        }
        if (button == 0 && onButton(mouseX, mouseY, SKIN_X)) {
            setXray(false);
            return true;
        }
        if (button == 0 && inView(mouseX, mouseY)) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            // The patient turns the way the mouse moves.
            yaw -= (float) dragX * 2.5F;
            pitch = Mth.clamp(pitch - (float) dragY * 1.2F, -35.0F, 35.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** The view's turn, for checks. */
    public float yaw() {
        return yaw;
    }
}
