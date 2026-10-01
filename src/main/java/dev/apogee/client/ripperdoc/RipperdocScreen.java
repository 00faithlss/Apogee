package dev.apogee.client.ripperdoc;

import dev.apogee.Apogee;
import dev.apogee.ripperdoc.RipperdocMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The ripperdoc screen, after the project author's design: implant slots by body system around a
 * view of the patient, the inventory below. The patient in the view is the player's own model with
 * whatever cyberware is fitted, and turns when dragged. The 480 by 356 texture is scaled down when
 * the window is smaller than that.
 */
public final class RipperdocScreen extends AbstractContainerScreen<RipperdocMenu> {
    private static final ResourceLocation TEXTURE = Apogee.id("textures/gui/ripperdoc.png");
    /** The patient view on the texture. */
    private static final int VIEW_X = 183;
    private static final int VIEW_Y = 50;
    private static final int VIEW_W = 114;
    private static final int VIEW_H = 174;
    private static final int PINK = 0xFFE6567A;
    private static final int CYAN = 0xFF7CE0EF;
    private static final int GREY = 0xFF8FA3AD;

    /** True while the patient view draws the player, so the couch pose stays off that model. */
    public static boolean renderingPatient;

    private float scale = 1.0F;
    private float yaw = -25.0F;
    private float pitch = 0.0F;
    private boolean dragging;

    public RipperdocScreen(RipperdocMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = RipperdocMenu.WIDTH;
        imageHeight = RipperdocMenu.HEIGHT;
    }

    @Override
    protected void init() {
        scale = Math.min(1.0F, Math.min((width - 8) / (float) imageWidth, (height - 8) / (float) imageHeight));
        super.init();
        leftPos = Math.round((width / scale - imageWidth) / 2.0F);
        topPos = Math.round((height / scale - imageHeight) / 2.0F);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int x = Math.round(mouseX / scale);
        int y = Math.round(mouseY / scale);
        super.render(graphics, x, y, partialTick);
        renderTooltip(graphics, x, y);
        graphics.pose().popPose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().scale(1.0F / scale, 1.0F / scale, 1.0F);
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        renderPatient(graphics);
    }

    /** The player's own model in the view, turned by dragging, with the fitted cyberware on it. */
    private void renderPatient(GuiGraphics graphics) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        int x0 = leftPos + VIEW_X;
        int y0 = topPos + VIEW_Y;
        graphics.enableScissor(Math.round(x0 * scale), Math.round(y0 * scale),
                Math.round((x0 + VIEW_W) * scale), Math.round((y0 + VIEW_H) * scale));
        float bodyRot = player.yBodyRot;
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float headRotO = player.yHeadRotO;
        float headRot = player.yHeadRot;
        player.yBodyRot = 180.0F + yaw;
        player.setYRot(180.0F + yaw);
        player.setXRot(0.0F);
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();
        Quaternionf pose = new Quaternionf().rotateZ(Mth.PI).rotateX(pitch * Mth.DEG_TO_RAD);
        float size = VIEW_H * 0.6F / player.getBbHeight();
        renderingPatient = true;
        try {
            InventoryScreen.renderEntityInInventory(graphics, x0 + VIEW_W / 2.0F, y0 + VIEW_H / 2.0F + 6.0F, size,
                    new Vector3f(0.0F, player.getBbHeight() / 2.0F + 0.0625F, 0.0F), pose, null, player);
        } finally {
            renderingPatient = false;
            player.yBodyRot = bodyRot;
            player.setYRot(yRot);
            player.setXRot(xRot);
            player.yHeadRotO = headRotO;
            player.yHeadRot = headRot;
        }
        graphics.disableScissor();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The texture already names every section; list what is fitted at the lower left.
        int x = 12;
        int y = 262;
        graphics.drawString(font, Component.translatable("screen.apogee.ripperdoc.installed").getString().toUpperCase(java.util.Locale.ROOT), x, y, PINK, false);
        y += 13;
        List<ItemStack> fitted = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot instanceof RipperdocMenu.ImplantView && slot.hasItem()) {
                fitted.add(slot.getItem());
            }
        }
        if (fitted.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.apogee.ripperdoc.none"), x, y, GREY, false);
            y += 11;
        }
        for (ItemStack stack : fitted) {
            graphics.drawString(font, stack.getHoverName(), x, y, CYAN, false);
            y += 11;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, 330, 0.0F);
        graphics.pose().scale(0.75F, 0.75F, 1.0F);
        graphics.drawString(font, Component.translatable("screen.apogee.ripperdoc.surgery"), 0, 0, GREY, false);
        graphics.pose().popPose();
        graphics.pose().pushPose();
        graphics.pose().translate(VIEW_X + 8, VIEW_Y + 6, 0.0F);
        graphics.pose().scale(0.6F, 0.6F, 1.0F);
        graphics.drawString(font, Component.translatable("screen.apogee.ripperdoc.rotate"), 0, 0, GREY, false);
        graphics.pose().popPose();
    }

    private boolean inView(double x, double y) {
        return x >= leftPos + VIEW_X && x < leftPos + VIEW_X + VIEW_W && y >= topPos + VIEW_Y && y < topPos + VIEW_Y + VIEW_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = mouseX / scale;
        double y = mouseY / scale;
        if (button == 0 && inView(x, y)) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            yaw += (float) (dragX / scale) * 1.6F;
            pitch = Mth.clamp(pitch - (float) (dragY / scale) * 0.8F, -35.0F, 35.0F);
            return true;
        }
        return super.mouseDragged(mouseX / scale, mouseY / scale, button, dragX / scale, dragY / scale);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX / scale, mouseY / scale, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX / scale, mouseY / scale, scrollX, scrollY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return mouseX < left || mouseY < top || mouseX >= left + imageWidth || mouseY >= top + imageHeight;
    }
}
