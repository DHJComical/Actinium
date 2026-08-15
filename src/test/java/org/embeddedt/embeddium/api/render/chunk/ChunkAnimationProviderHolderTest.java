package org.embeddedt.embeddium.api.render.chunk;

import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkAnimationProviderHolderTest {
    @AfterEach
    void clearProvider() {
        ChunkAnimationProviderHolder.setProvider(null);
    }

    @Test
    void returnsNullWhenNothingIsRegistered() {
        assertNull(ChunkAnimationProviderHolder.getProvider());
    }

    @Test
    void returnsRegisteredProvider() {
        ChunkAnimationProvider provider = noopProvider();

        ChunkAnimationProviderHolder.setProvider(provider);

        assertSame(provider, ChunkAnimationProviderHolder.getProvider());
    }

    @Test
    void replacementOverwritesPreviousProvider() {
        ChunkAnimationProvider first = noopProvider();
        ChunkAnimationProvider second = noopProvider();
        ChunkAnimationProviderHolder.setProvider(first);

        ChunkAnimationProviderHolder.setProvider(second);

        assertSame(second, ChunkAnimationProviderHolder.getProvider());
    }

    @Test
    void clearingNullsProvider() {
        ChunkAnimationProviderHolder.setProvider(noopProvider());

        ChunkAnimationProviderHolder.setProvider(null);

        assertNull(ChunkAnimationProviderHolder.getProvider());
    }

    private static ChunkAnimationProvider noopProvider() {
        return new ChunkAnimationProvider() {
            @Override
            public boolean getSectionOffset(RenderSection section, float[] out) {
                return false;
            }
        };
    }
}
