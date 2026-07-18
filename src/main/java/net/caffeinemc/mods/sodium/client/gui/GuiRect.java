package net.caffeinemc.mods.sodium.client.gui;

/** Immutable inclusive-exclusive GUI rectangle. */
public record GuiRect(int x, int y, int width, int height) {
    public GuiRect {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("GUI rectangle dimensions must not be negative");
        }
    }

    public int right() {
        return this.x + this.width;
    }

    public int bottom() {
        return this.y + this.height;
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX < this.right() && mouseY >= this.y && mouseY < this.bottom();
    }

    public GuiRect intersection(GuiRect other) {
        int left = Math.max(this.x, other.x);
        int top = Math.max(this.y, other.y);
        int right = Math.min(this.right(), other.right());
        int bottom = Math.min(this.bottom(), other.bottom());
        return new GuiRect(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }
}
