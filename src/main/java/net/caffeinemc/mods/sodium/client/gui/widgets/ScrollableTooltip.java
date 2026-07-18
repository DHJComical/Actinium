package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.GuiRect;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.gui.input.OverlayInputArbiter;
import net.caffeinemc.mods.sodium.client.gui.options.control.OptionControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Wrapped, scrollable tooltip constrained to either its own pane or an option-list overlay. */
public final class ScrollableTooltip {
    private static final ResourceLocation ARROWS = new ResourceLocation("sodium", "textures/gui/tooltip_arrows.png");
    private static final int PADDING = 4;
    private static final int BORDER_WIDTH = 1;
    private static final int SCROLL_THUMB_WIDTH = 3;
    private static final int MAX_WIDTH = 180;
    private final FontRenderer font = Minecraft.getMinecraft().fontRenderer;
    private final ScrollState scroll = new ScrollState(null);
    private GuiRect area;
    private boolean overlay;
    private OptionControl<?> target;
    private List<String> lines = List.of();
    private GuiRect visibleBounds;
    private int mouseX;
    private int mouseY;

    public ScrollableTooltip(GuiRect area, boolean overlay) {
        this.area = area;
        this.overlay = overlay;
    }

    public void setArea(GuiRect area, boolean overlay) {
        this.area = area;
        this.overlay = overlay;
        this.target = null;
        this.lines = List.of();
    }

    public void setTarget(OptionControl<?> target) {
        if (target == this.target) {
            if (target != null) {
                this.visibleBounds = this.positionBounds();
            }
            return;
        }
        this.target = target;
        this.scroll.scrollTo(0);
        if (target == null) {
            this.lines = List.of();
            return;
        }
        int width = Math.max(1, this.tooltipWidth() - PADDING * 2);
        List<String> content = new ArrayList<>(this.font.listFormattedStringToWidth(
                target.getOption().getTooltip().getFormattedText(), width));
        OptionImpact impact = target.getOption().getImpact();
        if (impact != null) {
            content.add(I18n.format("sodium.options.performance_impact_string", impact.name()));
        }
        this.lines = List.copyOf(content);
        this.visibleBounds = this.positionBounds();
        this.scroll.setContext(Math.max(0, this.visibleBounds.height() - PADDING * 2),
                this.contentHeight());
    }

    public void updateTarget(OptionControl<?> hovered, int mouseX, int mouseY) {
        this.updateTarget(hovered, null, mouseX, mouseY);
    }

    public void updateTarget(OptionControl<?> hovered, OptionControl<?> focused, int mouseX, int mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        // Focus is keyboard state and must not create a tooltip under an unrelated pointer
        // position. A visible overlay may retain its current target while the pointer is
        // inside the overlay itself so that scrolling and clicking remain possible.
        boolean insideCurrentTooltip = this.target != null && this.positionBounds().contains(mouseX, mouseY);
        OptionControl<?> target = selectTarget(hovered, focused, this.target, insideCurrentTooltip);
        if (target != null) {
            this.setTarget(target);
        } else {
            this.setTarget(null);
        }
    }

    /** Selects a hovered option, or retains an already-visible tooltip only while hovered. */
    static OptionControl<?> selectTarget(OptionControl<?> hovered, OptionControl<?> focused,
                                         OptionControl<?> current, boolean pointerInsideTooltip) {
        if (hovered != null) {
            return hovered;
        }
        return pointerInsideTooltip ? current : null;
    }

    public void setReservedAreaTopLeftCorner(int x, int y) {
        // Kept for source compatibility with callers of the upstream API. Overlay tooltips no longer
        // reserve space for action buttons; their bounds are constrained by the option viewport.
    }

    public void render() {
        if (this.target == null) {
            return;
        }
        GuiRect box = this.positionBounds();
        Gui.drawRect(box.x(), box.y(), box.right(), box.bottom(),
                this.overlay ? Colors.BACKGROUND_OVERLAY : Colors.BACKGROUND_LIGHT);
        // Keep the tooltip visually close to Minecraft's item tooltip while matching the active mod theme.
        int themeColor = this.target.getTheme().theme();
        Gui.drawRect(box.x(), box.y(), box.right(), box.y() + BORDER_WIDTH, themeColor);
        Gui.drawRect(box.x(), box.bottom() - BORDER_WIDTH, box.right(), box.bottom(), themeColor);
        Gui.drawRect(box.x(), box.y(), box.x() + BORDER_WIDTH, box.bottom(), themeColor);
        Gui.drawRect(box.right() - BORDER_WIDTH, box.y(), box.right(), box.bottom(), themeColor);
        if (!this.overlay) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(ARROWS);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            int arrowY = Math.max(this.area.y(), Math.min(this.target.getBounds().y()
                    + (this.target.getBounds().height() - 9) / 2, this.area.bottom() - 9));
            Gui.drawModalRectWithCustomSizedTexture(box.x() - 5, arrowY, 0, 0, 5, 9, 10, 9);
        }
        int y = box.y() + PADDING - this.scroll.amount();
        for (String line : this.lines) {
            if (y >= box.y() && y + this.font.FONT_HEIGHT <= box.bottom()) {
                this.font.drawString(line, box.x() + PADDING, y, Colors.FOREGROUND);
            }
            y += this.font.FONT_HEIGHT + Layout.TEXT_LINE_SPACING;
        }
        if (this.scroll.canScroll()) {
            int thumbY = box.y() + this.scroll.thumbStart(box.height());
            int thumbX = box.right() - BORDER_WIDTH - SCROLL_THUMB_WIDTH;
            Gui.drawRect(thumbX, thumbY, box.right() - BORDER_WIDTH,
                    Math.min(box.bottom() - BORDER_WIDTH, thumbY + this.scroll.thumbLength(box.height())),
                    this.target.getTheme().themeDisabled());
        }
    }

    public boolean mouseScrolled(int mouseX, int mouseY, int delta) {
        if (this.target != null && this.positionBounds().contains(mouseX, mouseY) && this.scroll.canScroll()) {
            this.scroll.scroll(delta);
            return true;
        }
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        return button >= 0 && this.capturesInput(mouseX, mouseY);
    }

    public boolean capturesInput(int mouseX, int mouseY) {
        return OverlayInputArbiter.captures(this.overlay, this.target != null,
                this.target == null ? null : this.positionBounds(), mouseX, mouseY);
    }

    private GuiRect positionBounds() {
        if (this.target == null) return this.area;
        this.overlay = true;
        int width = this.tooltipWidth();
        int height = Math.min(Math.max(1, this.area.height()), Math.max(1,
                this.contentHeight() + PADDING * 2));
        GuiRect bounds = calculateOverlayBounds(this.area, this.target.getBounds(), width, height,
                this.mouseX, this.mouseY);
        return bounds;
    }

    /** Returns the exact height occupied by the rendered tooltip lines, excluding padding. */
    private int contentHeight() {
        return calculateContentHeight(this.lines.size(), this.font.FONT_HEIGHT);
    }

    static int calculateContentHeight(int lineCount, int fontHeight) {
        if (lineCount <= 0) {
            return 0;
        }
        return lineCount * (fontHeight + Layout.TEXT_LINE_SPACING) - Layout.TEXT_LINE_SPACING;
    }

    private int tooltipWidth() {
        int available = Math.max(1, this.area.width());
        int maximum = Math.min(Math.min(Layout.MAX_TOOLTIP_WIDTH, MAX_WIDTH), available);
        if (this.target == null) {
            return maximum;
        }
        String text = this.target.getOption().getTooltip().getFormattedText();
        OptionImpact impact = this.target.getOption().getImpact();
        if (impact != null) {
            text += "\n" + I18n.format("sodium.options.performance_impact_string", impact.name());
        }
        int longestLine = 0;
        for (String line : text.split("\\n", -1)) {
            longestLine = Math.max(longestLine, this.font.getStringWidth(line));
        }
        int natural = longestLine + PADDING * 2 + BORDER_WIDTH * 2;
        return Math.min(maximum, Math.max(Layout.MIN_TOOLTIP_WIDTH, natural));
    }

    /** Calculates a mouse-adjacent overlay rectangle entirely within the supplied viewport. */
    static GuiRect calculateOverlayBounds(GuiRect area, GuiRect control, int width, int height,
                                          int mouseX, int mouseY) {
        int boundedWidth = Math.min(Math.max(1, width), Math.max(1, area.width()));
        int boundedHeight = Math.min(Math.max(1, height), Math.max(1, area.height()));
        int x = mouseX + Layout.TOOLTIP_OUTER_MARGIN;
        if (x + boundedWidth > area.right()) {
            x = mouseX - boundedWidth - Layout.TOOLTIP_OUTER_MARGIN;
        }
        if (x < area.x()) {
            x = area.x();
        } else if (x + boundedWidth > area.right()) {
            x = area.right() - boundedWidth;
        }

        int y = mouseY + Layout.TOOLTIP_OUTER_MARGIN;
        if (y + boundedHeight > area.bottom()) {
            y = mouseY - boundedHeight - Layout.TOOLTIP_OUTER_MARGIN;
        }
        if (y < area.y()) {
            y = area.y();
        } else if (y + boundedHeight > area.bottom()) {
            y = area.bottom() - boundedHeight;
        }
        return new GuiRect(x, y, boundedWidth, boundedHeight);
    }
}
