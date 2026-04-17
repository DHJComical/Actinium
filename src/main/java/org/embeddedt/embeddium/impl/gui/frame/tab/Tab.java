package org.embeddedt.embeddium.impl.gui.frame.tab;

import org.embeddedt.embeddium.impl.gui.frame.AbstractFrame;
import org.embeddedt.embeddium.impl.gui.frame.OptionPageFrame;
import org.embeddedt.embeddium.impl.gui.frame.ScrollableFrame;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.util.Dim2i;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionPage;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public record Tab<T extends AbstractFrame>(OptionIdentifier<Void> id, TextComponent title, Supplier<Boolean> onSelectFunction, Function<Dim2i, T> frameFunction) {
    public static <T extends AbstractFrame> Builder<T> createBuilder() {
        return new Builder<>();
    }

    public static <T extends AbstractFrame> Builder<T> builder() {
        return new Builder<>();
    }

    public T createFrame(Dim2i dim) {
        return this.frameFunction != null ? this.frameFunction.apply(dim) : null;
    }

    public static Tab<ScrollableFrame> from(OptionPage page, Predicate<Option<?>> optionFilter, AtomicReference<Integer> verticalScrollBarOffset) {
        Function<Dim2i, ScrollableFrame> frameFunction = dim2i -> ScrollableFrame
                .createBuilder()
                .setDimension(dim2i)
                .setFrame(OptionPageFrame
                        .createBuilder()
                        .setDimension(new Dim2i(dim2i.x(), dim2i.y(), dim2i.width(), dim2i.height()))
                        .setOptionPage(page)
                        .setOptionFilter(optionFilter)
                        .build())
                .setVerticalScrollBarOffset(verticalScrollBarOffset)
                .build();
        return Tab.<ScrollableFrame>builder()
                .setTitle(page.getName())
                .setId(page.getId())
                .setFrameFunction(frameFunction)
                .build();
    }

    public static final class Builder<T extends AbstractFrame> {
        private OptionIdentifier<Void> id;
        private TextComponent title;
        private Supplier<Boolean> onSelectFunction;
        private Function<Dim2i, T> frameFunction;

        public Builder<T> setId(OptionIdentifier<Void> id) {
            this.id = id;
            return this;
        }

        public Builder<T> setTitle(TextComponent title) {
            this.title = title;
            return this;
        }

        public Builder<T> setOnSelectFunction(Supplier<Boolean> onSelectFunction) {
            this.onSelectFunction = onSelectFunction;
            return this;
        }

        public Builder<T> setFrameFunction(Function<Dim2i, T> frameFunction) {
            this.frameFunction = frameFunction;
            return this;
        }

        public Tab<T> build() {
            return new Tab<>(this.id, this.title, this.onSelectFunction, this.frameFunction);
        }
    }
}
