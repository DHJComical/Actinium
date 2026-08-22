package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import com.dhj.actinium.gui.rso.compat.Component;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.ScrollableFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.PageFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoModOptions;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.Holder;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import net.minecraft.client.gui.GuiScreen;
import org.embeddedt.embeddium.api.options.structure.OptionPage;

import java.util.function.Function;

public class Tab<T extends AbstractFrame> {
    private final RsoModOptions modOptions;
    private final Component title;
    private final OptionPage page;
    private final Function<LayoutBounds, T> frameFunction;

    public Tab(RsoModOptions modOptions, Component title, OptionPage page, Function<LayoutBounds, T> frameFunction) {
        this.modOptions = modOptions;
        this.title = title;
        this.page = page;
        this.frameFunction = frameFunction;
    }

    public static Tab.Builder<?> builder() {
        return new Tab.Builder<>();
    }

    public RsoModOptions getModOptions() {
        return modOptions;
    }

    public Component getTitle() {
        return title;
    }

    String key() {
        return this.modOptions.configId() + ":" + this.title.getString();
    }

    public OptionPage getPage() {
        return page;
    }

    public Function<LayoutBounds, T> getFrameFunction() {
        return this.frameFunction;
    }

    public static class Builder<T extends AbstractFrame> {
        private RsoModOptions modOptions;
        private Component title;
        private OptionPage page;
        private Function<LayoutBounds, T> frameFunction;

        public Builder<T> withTitle(Component title) {
            this.title = title;
            return this;
        }

        public Builder<T> withFrameFunction(Function<LayoutBounds, T> frameFunction) {
            this.frameFunction = frameFunction;
            return this;
        }

        public Builder<T> withPage(OptionPage page) {
            this.page = page;
            return this;
        }

        public Builder<T> withModOptions(RsoModOptions modOptions) {
            this.modOptions = modOptions;
            return this;
        }


        public Tab<T> build() {
            return new Tab<T>(this.modOptions, this.title, this.page, this.frameFunction);
        }

        public Tab<ScrollableFrame> from(GuiScreen screen, RsoModOptions modOptions, OptionPage page, Holder<Integer> verticalScrollBarOffset, OptionStateStore optionStateStore) {
            return new Tab<>(modOptions, Component.fromEmbeddium(page.getName()), page, bounds -> {
                PageFrame pageFrame = PageFrame
                        .builder()
                        .withDimension(bounds)
                        .withModOptions(modOptions)
                        .withPage(page)
                        .withScreen(screen)
                        .withOptionStateStore(optionStateStore)
                        .build();
                ScrollableFrame scrollableFrame = ScrollableFrame
                        .builder()
                        .withDimension(bounds)
                        .withModOptions(modOptions)
                        .withScreen(screen)
                        .withFrame(pageFrame)
                        .withVerticalScrollBarOffset(verticalScrollBarOffset)
                        .build();
                pageFrame.setGroupToggleRebuildHandler(collapseKey -> {
                    scrollableFrame.rebuildContentFrame();
                    if (pageFrame.focusGroupHeader(collapseKey)) {
                        scrollableFrame.setFocused(pageFrame);
                    }
                });
                return scrollableFrame;
            });
        }
    }
}
