package me.flashyreese.mods.reeses_sodium_options.client.gui.widget;

import com.dhj.actinium.gui.rso.compat.Component;
import com.dhj.actinium.gui.rso.compat.ComponentPath;
import com.dhj.actinium.gui.rso.compat.FocusNavigationEvent;
import com.dhj.actinium.gui.rso.compat.Font;
import com.dhj.actinium.gui.rso.compat.FormattedText;
import com.dhj.actinium.gui.rso.compat.GuiEventListener;
import com.dhj.actinium.gui.rso.compat.GuiGraphicsExtractor;
import com.dhj.actinium.gui.rso.compat.NarratableEntry;
import com.dhj.actinium.gui.rso.compat.NarratedElementType;
import com.dhj.actinium.gui.rso.compat.NarrationElementOutput;
import com.dhj.actinium.gui.rso.compat.NarrationPriority;
import com.dhj.actinium.gui.rso.compat.Renderable;
import com.dhj.actinium.gui.rso.compat.ScreenRectangle;
import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import org.jspecify.annotations.NonNull;

public abstract class BaseWidget implements Renderable, GuiEventListener, NarratableEntry {
    protected final Font font;
    protected boolean focused;
    protected boolean hovered;
    private static boolean keyboardFocusVisible = false;
    private LayoutBounds dim;

    protected BaseWidget(LayoutBounds dim) {
        this.font = new Font(Minecraft.getMinecraft().fontRenderer);
        this.dim = dim;
    }

    public void setDim(LayoutBounds dim) {
        this.dim = dim;
    }

    public LayoutBounds getDimensions() {
        return this.dim;
    }

    public int getX() {
        return this.dim.x();
    }

    public int getY() {
        return this.dim.y();
    }

    public int getWidth() {
        return this.dim.width();
    }

    public int getHeight() {
        return this.dim.height();
    }

    public int getLimitX() {
        return this.dim.getLimitX();
    }

    public int getLimitY() {
        return this.dim.getLimitY();
    }

    public int getCenterX() {
        return this.dim.getCenterX();
    }

    public int getCenterY() {
        return this.dim.getCenterY();
    }

    public boolean isHovered() {
        return this.hovered;
    }

    public static boolean isKeyboardFocusVisible() {
        return keyboardFocusVisible;
    }

    public static void setKeyboardFocusVisible(boolean keyboardFocusVisible) {
        BaseWidget.keyboardFocusVisible = keyboardFocusVisible;
    }

    protected boolean shouldRenderFocusBorder() {
        return this.shouldRenderFocusBorder(this.isFocused());
    }

    protected boolean shouldRenderFocusBorder(boolean focused) {
        if (!focused) {
            return false;
        }

        return switch (ReeseSodiumOptionsConfig.config().getFocusBorderMode()) {
            case ALWAYS -> true;
            case KEYBOARD -> isKeyboardFocusVisible();
            case NEVER -> false;
        };
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.contains(mouseX, mouseY);
    }

    @Override
    public @NonNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    @Override
    public ComponentPath nextFocusPath(@NonNull FocusNavigationEvent navigation) {
        return this.isFocused() ? null : ComponentPath.leaf(this);
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public @NonNull NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarrationPriority.FOCUSED;
        }

        return this.isHovered() ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NonNull NarrationElementOutput builder) {
    }

    protected void addButtonNarration(NarrationElementOutput builder, Component label) {
        builder.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", label));
        this.addButtonUsageNarration(builder);
    }

    protected void addButtonUsageNarration(NarrationElementOutput builder) {
        this.addUsageNarration(builder, "narration.button.usage.focused", "narration.button.usage.hovered");
    }

    protected void addUsageNarration(NarrationElementOutput builder, String focusedKey, String hoveredKey) {
        if (this.isFocused()) {
            builder.add(NarratedElementType.USAGE, Component.translatable(focusedKey));
        } else if (this.isHovered()) {
            builder.add(NarratedElementType.USAGE, Component.translatable(hoveredKey));
        }
    }

    protected void drawString(GuiGraphicsExtractor guiGraphics, String text, int x, int y, int color) {
        guiGraphics.text(this.font, text, x, y, color);
    }

    protected void drawString(GuiGraphicsExtractor guiGraphics, Component text, int x, int y, int color) {
        guiGraphics.text(this.font, text, x, y, color);
    }

    protected void drawCenteredString(GuiGraphicsExtractor guiGraphics, Component text, int x, int y, int color) {
        guiGraphics.centeredText(this.font, text, x, y, color);
    }

    protected int getStringWidth(FormattedText text) {
        return this.font.width(text);
    }

    protected void drawRect(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1, y1, x2, y2, color);
    }

    protected void drawBorder(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int color) {
        border(guiGraphics, x1, y1, x2, y2, color);
    }

    public static void border(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1, y1, x2, y1 + 1, color);
        guiGraphics.fill(x1, y2 - 1, x2, y2, color);
        guiGraphics.fill(x1, y1, x1 + 1, y2, color);
        guiGraphics.fill(x2 - 1, y1, x2, y2, color);
    }

    protected void playClickSound() {
        Minecraft.getMinecraft()
                .getSoundHandler()
                .playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    protected String truncateTextToFit(String text, int maxWidth) {
        String ellipsis = "...";
        int ellipsisWidth = this.font.width(ellipsis);
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        maxWidth -= ellipsisWidth;
        int high = text.length() - ellipsis.length();
        int low = 1;
        while (high - low > 1) {
            int mid = (high + low) / 2;
            if (this.font.width(text.substring(0, mid)) > maxWidth) {
                high = mid;
            } else {
                low = mid;
            }
        }

        return text.substring(0, low).trim() + ellipsis;
    }
}
