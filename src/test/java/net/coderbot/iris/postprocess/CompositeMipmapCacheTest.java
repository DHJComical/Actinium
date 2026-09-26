package net.coderbot.iris.postprocess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeMipmapCacheTest {
    @Test
    void reusesOnlyTheGeneratedTexture() {
        CompositeMipmapCache cache = new CompositeMipmapCache();
        assertTrue(cache.needsGeneration(11));
        cache.markGenerated(11);
        assertFalse(cache.needsGeneration(11));
        assertTrue(cache.needsGeneration(12));
    }

    @Test
    void invalidatesTheActualDrawAttachmentWithoutInvalidatingItsPair() {
        CompositeMipmapCache cache = new CompositeMipmapCache();
        cache.markGenerated(11);
        cache.markGenerated(12);

        cache.invalidateDrawTarget(11, 12, true);
        assertTrue(cache.needsGeneration(11));
        assertFalse(cache.needsGeneration(12));

        cache.markGenerated(11);
        cache.invalidateDrawTarget(11, 12, false);
        assertFalse(cache.needsGeneration(11));
        assertTrue(cache.needsGeneration(12));
    }

    @Test
    void unrelatedPassWriteKeepsAnExistingMipmapReusable() {
        CompositeMipmapCache cache = new CompositeMipmapCache();
        cache.markGenerated(11);

        // A composite pass writes colortex1 while the next pass reads colortex0 again.
        cache.invalidateDrawTarget(21, 22, false);

        assertFalse(cache.needsGeneration(11));
    }

    @Test
    void computeOrImageWritesInvalidateAllTrackedTextures() {
        CompositeMipmapCache cache = new CompositeMipmapCache();
        cache.markGenerated(11);
        cache.markGenerated(12);
        cache.invalidateAll();
        assertTrue(cache.needsGeneration(11));
        assertTrue(cache.needsGeneration(12));
    }

    @Test
    void eachRenderInvocationStartsWithAnEmptyCache() {
        CompositeMipmapCache firstInvocation = new CompositeMipmapCache();
        firstInvocation.markGenerated(11);
        assertFalse(firstInvocation.needsGeneration(11));
        assertTrue(new CompositeMipmapCache().needsGeneration(11));
    }
}
