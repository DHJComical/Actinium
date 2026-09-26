package net.coderbot.iris.postprocess;

import java.util.HashSet;
import java.util.Set;

/** Tracks texture mipmaps generated during one composite render invocation. */
final class CompositeMipmapCache {
    private final Set<Integer> generatedTextures = new HashSet<>();

    boolean needsGeneration(int texture) {
        return !generatedTextures.contains(texture);
    }

    void markGenerated(int texture) {
        generatedTextures.add(texture);
    }

    void invalidate(int texture) {
        generatedTextures.remove(texture);
    }

    void invalidateDrawTarget(int mainTexture, int altTexture, boolean readsFromAlt) {
        invalidate(readsFromAlt ? mainTexture : altTexture);
    }

    void invalidateAll() {
        generatedTextures.clear();
    }
}
