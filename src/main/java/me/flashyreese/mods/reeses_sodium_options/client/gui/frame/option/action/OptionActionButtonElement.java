package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.action;

import com.dhj.actinium.gui.rso.compat.Component;
import com.dhj.actinium.gui.rso.compat.ComponentPath;
import com.dhj.actinium.gui.rso.compat.FocusNavigationEvent;
import com.dhj.actinium.gui.rso.compat.GuiEventListener;
import com.dhj.actinium.gui.rso.compat.GuiGraphicsExtractor;
import com.dhj.actinium.gui.rso.compat.KeyEvent;
import com.dhj.actinium.gui.rso.compat.MouseButtonEvent;
import com.dhj.actinium.gui.rso.compat.NarratableEntry;
import com.dhj.actinium.gui.rso.compat.NarratedElementType;
import com.dhj.actinium.gui.rso.compat.NarrationElementOutput;
import com.dhj.actinium.gui.rso.compat.NarrationPriority;
import com.dhj.actinium.gui.rso.compat.ScreenRectangle;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuide;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuideProvider;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoOption;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class OptionActionButtonElement implements GuiEventListener, NarratableEntry, ControlGuideProvider {
    private final Supplier<LayoutBounds> rowBoundsSupplier;
    private final Supplier<@Nullable RsoOption> optionSupplier;
    private final IntSupplier buttonsFromRight;
    private final ResourceLocation icon;
    private final Component guideLabel;
    private final Function<RsoOption, Component> narrationLabelProvider;
    private final Predicate<RsoOption> visiblePredicate;
    private final Predicate<RsoOption> activePredicate;
    private final Consumer<RsoOption> action;
    private final Runnable clickSound;
    private final Runnable afterAction;
    private boolean focused;
    private boolean hovered;

    OptionActionButtonElement(Supplier<LayoutBounds> rowBoundsSupplier, Supplier<@Nullable RsoOption> optionSupplier,
                              IntSupplier buttonsFromRight, ResourceLocation icon, Component guideLabel, Function<RsoOption, Component> narrationLabelProvider, Predicate<RsoOption> visiblePredicate, Predicate<RsoOption> activePredicate,
                              Consumer<RsoOption> action, Runnable clickSound, Runnable afterAction) {
        this.rowBoundsSupplier = rowBoundsSupplier;
        this.optionSupplier = optionSupplier;
        this.buttonsFromRight = buttonsFromRight;
        this.icon = icon;
        this.guideLabel = guideLabel;
        this.narrationLabelProvider = narrationLabelProvider;
        this.visiblePredicate = visiblePredicate;
        this.activePredicate = activePredicate;
        this.action = action;
        this.clickSound = clickSound;
        this.afterAction = afterAction;
    }

    @Override
    public boolean isActive() {
        RsoOption option = this.optionSupplier.get();

        return option != null && this.visiblePredicate.test(option) && this.activePredicate.test(option);
    }

    public boolean isVisible() {
        RsoOption option = this.optionSupplier.get();

        return option != null && this.visiblePredicate.test(option);
    }

    @Override
    public List<ControlGuide> controlGuides() {
        return this.isFocused() && this.isActive() ? List.of(new ControlGuide(ControlGuide.Input.PRESS, this.guideLabel)) : List.of();
    }

    public LayoutBounds getDimensions() {
        return this.getButtonDim(this.rowBoundsSupplier.get());
    }

    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean focused) {
        RsoOption option = this.optionSupplier.get();

        if (option == null || !this.visiblePredicate.test(option)) {
            return;
        }

        boolean active = this.activePredicate.test(option);
        this.hovered = active && this.isMouseOver(mouseX, mouseY);
        OptionActionButtonRenderer.render(guiGraphics, this.icon, this.getButtonDim(this.rowBoundsSupplier.get()), mouseX, mouseY, focused, active);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || !this.isMouseOver(event.x(), event.y())) {
            return false;
        }

        return this.performAction();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.focused || !event.isSelection()) {
            return false;
        }

        return this.performAction();
    }

    public boolean performAction() {
        RsoOption option = this.optionSupplier.get();

        if (option == null || !this.visiblePredicate.test(option) || !this.activePredicate.test(option)) {
            return false;
        }

        this.action.accept(option);
        this.clickSound.run();
        this.afterAction.run();

        return true;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent navigation) {
        return this.isActive() && !this.focused ? ComponentPath.leaf(this) : null;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        RsoOption option = this.optionSupplier.get();

        return option != null && this.visiblePredicate.test(option) && this.activePredicate.test(option)
                && this.getButtonDim(this.rowBoundsSupplier.get()).contains(mouseX, mouseY);
    }

    @Override
    public @NonNull ScreenRectangle getRectangle() {
        if (!this.isActive()) {
            return ScreenRectangle.empty();
        }

        LayoutBounds dim = this.getDimensions();

        return new ScreenRectangle(dim.x(), dim.y(), dim.width(), dim.height());
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused && this.isActive();
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public @NonNull NarrationPriority narrationPriority() {
        if (this.focused) {
            return NarrationPriority.FOCUSED;
        }

        return this.hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NonNull NarrationElementOutput builder) {
        RsoOption option = this.optionSupplier.get();

        if (option == null || !this.visiblePredicate.test(option) || !this.activePredicate.test(option)) {
            return;
        }

        builder.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", this.narrationLabelProvider.apply(option)));
        if (this.focused) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
        } else if (this.hovered) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
        }
    }

    private LayoutBounds getButtonDim(LayoutBounds rowBounds) {
        return OptionActionButtonRenderer.buttonBounds(rowBounds, this.buttonsFromRight.getAsInt());
    }
}
