package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.config.structure.ColorTheme;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.caffeinemc.mods.sodium.client.gui.widgets.ResetButton;
import net.caffeinemc.mods.sodium.client.gui.input.FocusTarget;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/** Shared option row rendering, focus, dependency state, tooltip hit-testing, and per-option reset. */
public abstract class OptionControl<O extends Option> implements FocusTarget {
    protected static final int RESET_WIDTH = 16;
    protected final O option;
    protected final ColorTheme theme;
    protected final FontRenderer font;
    private GuiRect bounds = new GuiRect(0, 0, 0, 0);
    private boolean focused;

    protected OptionControl(O option, ColorTheme theme, FontRenderer font) {
        this.option = option;
        this.theme = theme;
        this.font = font;
    }

    public final void setBounds(GuiRect bounds) {
        this.bounds = bounds;
    }

    public final GuiRect getBounds() {
        return this.bounds;
    }

    public final O getOption() {
        return this.option;
    }

    /** Returns the owner theme used to render this option and its related overlays. */
    public final ColorTheme getTheme() {
        return this.theme;
    }

    public final void render(int mouseX, int mouseY) {
        boolean enabled = this.option.isEnabled();
        boolean hovered = this.bounds.contains(mouseX, mouseY);
        int background = hovered ? Colors.BACKGROUND_HOVER : Colors.BACKGROUND_LIGHT;
        Gui.drawRect(this.bounds.x(), this.bounds.y(), this.bounds.right(), this.bounds.bottom(), background);
        if (this.focused) {
            drawBorder(this.bounds, 0xFFFFFFFF);
        }

        int controlWidth = this.controlWidth();
        int labelWidth = Math.max(8, this.bounds.width() - controlWidth - RESET_WIDTH - 12);
        String label = this.font.trimStringToWidth(this.option.getName().getUnformattedText()
                + (enabled && this.option.hasChanged() ? " *" : ""), labelWidth);
        if (enabled && this.option.hasChanged()) {
            label = "\u00a7o" + label;
        } else if (!enabled) {
            label = "\u00a77\u00a7m" + label;
        }
        int foreground = enabled ? Colors.FOREGROUND : Colors.FOREGROUND_DISABLED;
        this.font.drawString(label, this.bounds.x() + 6,
                this.bounds.y() + (this.bounds.height() - this.font.FONT_HEIGHT) / 2, foreground);

        if (this.option.hasChanged()) {
            if (ResetButton.isActive(true, hovered)) {
                ResetButton.render(this.bounds);
            }
            int resetX = this.bounds.right() - RESET_WIDTH;
            this.font.drawString("R", resetX + (RESET_WIDTH - this.font.getStringWidth("R")) / 2,
                    this.bounds.y() + (this.bounds.height() - this.font.FONT_HEIGHT) / 2,
                    enabled ? this.theme.themeHighlight() : this.theme.themeDisabled());
        }
        if (!this.shouldHideControl()) {
            this.renderControl(this.controlBounds(), enabled, mouseX, mouseY);
        }
    }

    public final boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 || !this.option.isEnabled() || !this.bounds.contains(mouseX, mouseY)) {
            return false;
        }
        if (this.option.hasChanged() && mouseX >= this.bounds.right() - RESET_WIDTH
                && ResetButton.isActive(true, this.bounds.contains(mouseX, mouseY))) {
            this.option.resetToDefault();
            return true;
        }
        return !this.shouldHideControl() && this.onMouseClicked(this.controlBounds(), mouseX, mouseY);
    }

    public final boolean keyPressed(int keyCode) {
        return this.focused && this.option.isEnabled() && !this.shouldHideControl()
                && this.onKeyPressed(keyCode);
    }

    public final boolean mouseDragged(int mouseX, int mouseY) {
        return this.focused && this.option.isEnabled() && !this.shouldHideControl()
                && this.onMouseDragged(this.controlBounds(), mouseX, mouseY);
    }

    @Override
    public final void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public final boolean isFocused() {
        return this.focused;
    }

    @Override
    public final boolean isFocusable() {
        return this.option.isEnabled() && !this.shouldHideControl();
    }

    private GuiRect controlBounds() {
        int width = Math.min(this.controlWidth(), Math.max(20, this.bounds.width() / 2));
        return new GuiRect(this.bounds.right() - RESET_WIDTH - width - 4, this.bounds.y() + 3,
                width, Math.max(1, this.bounds.height() - 6));
    }

    protected abstract int controlWidth();

    protected abstract void renderControl(GuiRect control, boolean enabled, int mouseX, int mouseY);

    protected abstract boolean onMouseClicked(GuiRect control, int mouseX, int mouseY);

    protected abstract boolean onKeyPressed(int keyCode);

    protected boolean onMouseDragged(GuiRect control, int mouseX, int mouseY) {
        return false;
    }

    private boolean shouldHideControl() {
        return this.option instanceof StatefulOption<?> statefulOption && statefulOption.shouldHideControl();
    }

    private static void drawBorder(GuiRect bounds, int color) {
        Gui.drawRect(bounds.x(), bounds.y(), bounds.right(), bounds.y() + 1, color);
        Gui.drawRect(bounds.x(), bounds.bottom() - 1, bounds.right(), bounds.bottom(), color);
        Gui.drawRect(bounds.x(), bounds.y(), bounds.x() + 1, bounds.bottom(), color);
        Gui.drawRect(bounds.right() - 1, bounds.y(), bounds.right(), bounds.bottom(), color);
    }
}
