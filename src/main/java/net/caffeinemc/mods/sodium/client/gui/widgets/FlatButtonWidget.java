package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.ButtonTheme;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.minecraft.client.gui.Gui;

/** Flat action button used by the footer, reset affordances, and search clear action. */
public final class FlatButtonWidget extends AbstractWidget {
    private final Runnable action;
    private final String label;
    private final boolean drawBackground;
    private final boolean drawFrame;
    private final boolean leftAlign;
    private boolean enabled = true;
    private boolean visible = true;
    private boolean selected;
    private final ButtonTheme theme;
    private static final ButtonTheme DEFAULT_THEME = new ButtonTheme(Colors.FOREGROUND, Colors.FOREGROUND,
            Colors.FOREGROUND_DISABLED, Colors.BACKGROUND_HOVER, Colors.BACKGROUND_DEFAULT,
            Colors.BACKGROUND_LIGHT);

    public FlatButtonWidget(GuiRect bounds, String label, Runnable action) {
        this(bounds, label, action, true, true, false, DEFAULT_THEME);
    }

    public FlatButtonWidget(GuiRect bounds, String label, Runnable action, boolean drawBackground,
                            boolean drawFrame, boolean leftAlign, ButtonTheme theme) {
        super(bounds);
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Button label must not be blank");
        }
        this.label = label;
        this.action = action;
        this.drawBackground = drawBackground;
        this.drawFrame = drawFrame;
        this.leftAlign = leftAlign;
        this.theme = theme;
    }

    public void render(int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        boolean hovered = this.bounds.contains(mouseX, mouseY);
        int background = this.enabled ? (hovered ? this.theme.backgroundHighlight() : this.theme.backgroundDefault())
                : this.theme.backgroundInactive();
        int foreground = this.enabled ? this.theme.themeLighter() : this.theme.themeDarker();
        if (this.drawBackground) {
            Gui.drawRect(this.bounds.x(), this.bounds.y(), this.bounds.right(), this.bounds.bottom(), background);
        }
        if (this.focused) {
            drawBorder(this.bounds, Colors.BUTTON_BORDER);
        }
        String text = this.font.trimStringToWidth(this.label, Math.max(1, this.bounds.width() - 8));
        int textX = this.leftAlign ? this.bounds.x() + 8
                : this.bounds.x() + (this.bounds.width() - this.font.getStringWidth(text)) / 2;
        this.font.drawString(text, textX,
                this.bounds.y() + (this.bounds.height() - this.font.FONT_HEIGHT) / 2, foreground);
        if (this.enabled && this.selected) {
            Gui.drawRect(this.bounds.x(), this.bounds.bottom() - 1, this.bounds.right(), this.bounds.bottom(), Colors.THEME);
        }
        if (this.drawFrame || (this.enabled && this.focused)) {
            drawBorder(this.bounds, Colors.BUTTON_BORDER);
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && this.visible && this.enabled && this.bounds.contains(mouseX, mouseY)) {
            this.activate();
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (this.focused && this.enabled && this.visible && (keyCode == 28 || keyCode == 57)) {
            this.activate();
            return true;
        }
        return false;
    }

    private void activate() {
        this.action.run();
        this.playClickSound();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean isFocusable() {
        return this.visible && this.enabled;
    }

    private static void drawBorder(GuiRect bounds, int color) {
        Gui.drawRect(bounds.x(), bounds.y(), bounds.right(), bounds.y() + 1, color);
        Gui.drawRect(bounds.x(), bounds.bottom() - 1, bounds.right(), bounds.bottom(), color);
        Gui.drawRect(bounds.x(), bounds.y(), bounds.x() + 1, bounds.bottom(), color);
        Gui.drawRect(bounds.right() - 1, bounds.y(), bounds.right(), bounds.bottom(), color);
    }
}
