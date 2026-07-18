package net.caffeinemc.mods.sodium.client.config.search;

/** Object capable of registering all language-sensitive text it owns. */
public interface Searchable {
    /** Adds text sources to the supplied index. */
    void registerTextSources(SearchIndex index);
}
