package net.caffeinemc.mods.sodium.client.config.search;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Search index lifecycle which explicitly observes the adapter's current language token.
 */
public abstract class SearchIndex {
    private final Supplier<?> languageProvider;
    private final Runnable registerCallback;
    private Object builtLanguage;

    protected SearchIndex(Supplier<?> languageProvider, Runnable registerCallback) {
        this.languageProvider = Objects.requireNonNull(languageProvider, "Language provider must not be null");
        this.registerCallback = Objects.requireNonNull(registerCallback, "Registration callback must not be null");
    }

    /** Adds one source before the registry-backed index is frozen. */
    public abstract void register(TextSource source);

    /** Rebuilds index data from already registered sources. */
    protected abstract void rebuildIndex();

    /** Creates an isolated query session over the current index. */
    protected abstract SearchQuerySession createQuery();

    /** Ensures registration and language invalidation have completed before querying. */
    public final SearchQuerySession startQuery() {
        Object language = Objects.requireNonNull(this.languageProvider.get(), "Language provider returned null");
        if (this.builtLanguage == null) {
            this.builtLanguage = language;
            this.registerCallback.run();
            this.rebuildIndex();
        } else if (!Objects.equals(this.builtLanguage, language)) {
            this.builtLanguage = language;
            this.invalidateSources();
            this.rebuildIndex();
        }
        return this.createQuery();
    }

    /** Invalidates cached localized strings while preserving stable source identities. */
    protected abstract void invalidateSources();
}
