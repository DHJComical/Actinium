package net.caffeinemc.mods.sodium.client.config.search;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Base index retaining text source identities across language rebuilds. */
public abstract class SourceStoringIndex extends SearchIndex {
    protected final List<TextSource> sources = new ArrayList<>();

    protected SourceStoringIndex(Supplier<?> languageProvider, Runnable registerCallback) {
        super(languageProvider, registerCallback);
    }

    @Override
    public final void register(TextSource source) {
        if (source == null) {
            throw new IllegalArgumentException("Text source must not be null");
        }
        this.sources.add(source);
    }

    @Override
    protected final void invalidateSources() {
        this.sources.forEach(TextSource::invalidateText);
    }
}
