package com.dhj.actinium.gui.rso.compat;

import net.minecraft.util.text.ITextComponent;

/**
 * Mutable variant of {@link Component} matching the modern
 * {@code MutableComponent} contract used for chained construction.
 */
public class MutableComponent extends Component {
    private final ITextComponent delegate;

    MutableComponent(ITextComponent delegate) {
        this(delegate, null);
    }

    MutableComponent(ITextComponent delegate, Style rgbStyle) {
        super(delegate);
        this.delegate = delegate;
        if (rgbStyle != null) {
            super.withStyle(rgbStyle);
        }
    }

    @Override
    public MutableComponent append(Component other) {
        this.delegate.appendSibling(other.unwrap());
        return this;
    }

    @Override
    public MutableComponent append(String text) {
        return this.append(literal(text));
    }

    @Override
    public MutableComponent withStyle(Style style) {
        super.withStyle(style);
        return this;
    }

    @Override
    public MutableComponent copy() {
        return this.mutableCopy();
    }
}
