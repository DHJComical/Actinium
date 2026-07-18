package net.caffeinemc.mods.sodium.client.config.search;

/**
 * Lazily resolved localized text source whose cache is invalidated on language changes.
 */
public abstract class TextSource {
    private String text;
    private float score;
    private int resultIndex;

    /** Resolves text using the current language. */
    protected abstract String getTextFromSource();

    /** Returns cached non-null resolved text. */
    public final String getText() {
        if (this.text == null) {
            this.text = this.getTextFromSource();
        }
        return this.text;
    }

    final void invalidateText() {
        this.text = null;
    }

    /** Returns the latest query relevance score. */
    public final float getScore() {
        return this.score;
    }

    final void setScore(float score) {
        this.score = score;
    }

    /** Returns the UI-assigned result index. */
    public final int getResultIndex() {
        return this.resultIndex;
    }

    /** Assigns a stable index for keyboard result navigation. */
    public final void setResultIndex(int resultIndex) {
        this.resultIndex = resultIndex;
    }
}
