package me.flashyreese.mods.reeses_sodium_options.client.gui.widget;

import com.dhj.actinium.gui.rso.compat.Component;
import com.dhj.actinium.gui.rso.compat.ComponentPath;
import com.dhj.actinium.gui.rso.compat.CursorTypes;
import com.dhj.actinium.gui.rso.compat.FocusNavigationEvent;
import com.dhj.actinium.gui.rso.compat.GuiGraphicsExtractor;
import com.dhj.actinium.gui.rso.compat.KeyEvent;
import com.dhj.actinium.gui.rso.compat.MouseButtonEvent;
import com.dhj.actinium.gui.rso.compat.NarrationElementOutput;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuide;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuideProvider;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiThemes;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class LabelWidget extends BaseWidget implements ControlGuideProvider {
    private static final int TEXT_PADDING = 5;
    private static final int CHEVRON_GAP = 4;
    private static final String CHEVRON_COLLAPSED = "\u25b6";
    private static final String CHEVRON_EXPANDED = "\u25bc";

    private final Component text;
    private final int color;
    private final @Nullable GuiTheme theme;
    private final @Nullable ResourceLocation collapseKey;
    private final @Nullable Runnable onToggle;
    private boolean collapsed;

    public LabelWidget(LayoutBounds dim, Component text, int color) {
        this(dim, text, color, null, null, null, false);
    }

    public LabelWidget(LayoutBounds dim, Component text, int color, @Nullable ResourceLocation collapseKey, @Nullable Runnable onToggle, boolean collapsed) {
        this(dim, text, color, null, collapseKey, onToggle, collapsed);
    }

    public LabelWidget(LayoutBounds dim, Component text, int color, @Nullable GuiTheme theme, @Nullable ResourceLocation collapseKey, @Nullable Runnable onToggle, boolean collapsed) {
        super(dim);
        this.text = text;
        this.color = color;
        this.theme = theme;
        this.collapseKey = collapseKey;
        this.onToggle = onToggle;
        this.collapsed = collapsed;
    }

    public @Nullable ResourceLocation collapseKey() {
        return this.collapseKey;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    @Override
    public List<ControlGuide> controlGuides() {
        if (this.onToggle == null || !this.isFocused()) {
            return List.of();
        }

        return List.of(ControlGuide.press(Component.translatable(this.collapsed ? "rso.controller.guide.expand" : "rso.controller.guide.collapse")));
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (this.onToggle == null) {
            int textWidth = this.getStringWidth(this.text);
            int x = this.getCenterX() - (textWidth / 2);
            this.drawString(guiGraphics, this.text, x, this.getY(), this.color);
            return;
        }

        this.hovered = this.isMouseOver(mouseX, mouseY);
        if (this.theme != null) {
            this.drawRect(guiGraphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), this.hovered ? this.theme.bgHighlight : this.theme.bgDefault);
        }

        int textY = (int) (this.getY() + Math.ceil((double) (this.getHeight() - this.font.lineHeight) / 2));
        String chevron = this.collapsed ? CHEVRON_COLLAPSED : CHEVRON_EXPANDED;
        int chevronX = this.getX() + TEXT_PADDING;
        this.drawString(guiGraphics, chevron, chevronX, textY, this.color);
        this.drawString(guiGraphics, this.text, chevronX + this.font.width(chevron) + CHEVRON_GAP, textY, this.color);

        if (this.shouldRenderFocusBorder()) {
            this.drawBorder(guiGraphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), GuiThemes.OPTION_FOCUS_BORDER);
        }

        if (this.hovered) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (this.onToggle != null && event.button() == 0 && this.isMouseOver(event.x(), event.y())) {
            this.onToggle.run();
            this.playClickSound();
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (this.onToggle != null && this.isFocused() && event.isSelection()) {
            this.onToggle.run();
            this.playClickSound();
            return true;
        }

        return false;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent navigation) {
        if (this.onToggle == null || this.isFocused()) {
            return null;
        }

        return ComponentPath.leaf(this);
    }

    @Override
    public boolean isActive() {
        return this.onToggle != null;
    }

    @Override
    public void updateNarration(@NonNull NarrationElementOutput builder) {
        if (this.onToggle == null) {
            return;
        }

        Component state = Component.translatable(this.collapsed ? "rso.narration.collapsed" : "rso.narration.expanded");
        this.addButtonNarration(builder, com.dhj.actinium.gui.rso.compat.CommonComponents.optionNameValue(this.text, state));
    }

    @Override
    public boolean isFocused() {
        return this.onToggle != null && this.focused;
    }

    @Override
    public void setFocused(boolean focused) {
        if (this.onToggle != null) {
            this.focused = focused;
        }
    }
}
