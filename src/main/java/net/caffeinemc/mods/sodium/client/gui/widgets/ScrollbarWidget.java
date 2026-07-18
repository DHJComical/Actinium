package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

/** Mouse wheel, track paging, and thumb dragging for a vertical viewport. */
public final class ScrollbarWidget extends AbstractWidget {
    private static final int TRACK_COLOR = 0x96323232;
    private static final int THUMB_COLOR = 0x96646464;
    private final ScrollState state;
    private boolean dragging;
    private int dragOffset;
    private long lastInteractionTime;

    public ScrollbarWidget(GuiRect bounds, ScrollState state) {
        super(bounds);
        this.state = state;
    }

    public void render() {
        if (!this.state.canScroll()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!this.isHovered() && !this.dragging && now - this.lastInteractionTime >= 1_000L) {
            return;
        }
        Gui.drawRect(this.bounds.x(), this.bounds.y(), this.bounds.right(), this.bounds.bottom(), TRACK_COLOR);
        int start = this.state.thumbStart(this.bounds.height());
        int length = Math.min(this.bounds.height(), this.state.thumbLength(this.bounds.height()));
        Gui.drawRect(this.bounds.x(), this.bounds.y() + start, this.bounds.right(), this.bounds.y() + start + length,
                THUMB_COLOR);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 || !this.state.canScroll() || !this.bounds.contains(mouseX, mouseY)) {
            return false;
        }
        int thumbStart = this.bounds.y() + this.state.thumbStart(this.bounds.height());
        int thumbLength = this.state.thumbLength(this.bounds.height());
        if (mouseY >= thumbStart && mouseY < thumbStart + thumbLength) {
            this.dragging = true;
            this.dragOffset = mouseY - thumbStart;
        } else {
            this.state.scroll(mouseY < thumbStart ? -this.bounds.height() : this.bounds.height());
        }
        this.lastInteractionTime = System.currentTimeMillis();
        return true;
    }

    public boolean mouseDragged(int mouseY) {
        if (!this.dragging) {
            return false;
        }
        int thumbLength = this.state.thumbLength(this.bounds.height());
        int travel = Math.max(1, this.bounds.height() - thumbLength);
        int thumb = Math.max(0, Math.min(travel, mouseY - this.bounds.y() - this.dragOffset));
        int maximum = Math.max(0, this.state.total() - this.state.visible());
        this.state.scrollTo((int) Math.round((double) thumb / travel * maximum));
        this.lastInteractionTime = System.currentTimeMillis();
        return true;
    }

    public void mouseReleased() {
        this.dragging = false;
        this.lastInteractionTime = System.currentTimeMillis();
    }

    private boolean isHovered() {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int mouseX = Mouse.getX() * resolution.getScaledWidth() / minecraft.displayWidth;
        int mouseY = resolution.getScaledHeight() - Mouse.getY() * resolution.getScaledHeight()
                / minecraft.displayHeight - 1;
        return this.bounds.contains(mouseX, mouseY);
    }

}
