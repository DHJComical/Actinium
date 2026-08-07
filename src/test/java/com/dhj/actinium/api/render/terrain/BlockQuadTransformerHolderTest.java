package com.dhj.actinium.api.render.terrain;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockQuadTransformerHolderTest {
    private final List<BlockQuadTransformer> registered = new ArrayList<>();

    @AfterEach
    void clearRegisteredTransformers() {
        for (BlockQuadTransformer transformer : registered) {
            BlockQuadTransformerHolder.unregister(transformer);
        }
        registered.clear();
    }

    @Test
    void returnsOriginalListWhenNothingIsRegistered() {
        List<BakedQuad> quads = new ArrayList<>();

        List<BakedQuad> result = transform(quads);

        assertSame(quads, result);
    }

    @Test
    void appliesTransformersInRegistrationOrder() {
        List<BakedQuad> input = new ArrayList<>();
        List<BakedQuad> firstOutput = nonEmptyQuadList();
        List<BakedQuad> secondOutput = nonEmptyQuadList();
        List<List<BakedQuad>> received = new ArrayList<>();
        BlockQuadTransformer first = (state, pos, blockAccess, layer, side, quads) -> {
            received.add(quads);
            return firstOutput;
        };
        BlockQuadTransformer second = (state, pos, blockAccess, layer, side, quads) -> {
            received.add(quads);
            return secondOutput;
        };
        registerForCleanup(first);
        registerForCleanup(second);

        List<BakedQuad> result = transform(input);

        assertSame(secondOutput, result);
        assertEquals(2, received.size());
        assertSame(input, received.get(0));
        assertSame(firstOutput, received.get(1));
    }

    @Test
    void emptyResultShortCircuitsRemainingTransformers() {
        List<BakedQuad> input = new ArrayList<>();
        List<BakedQuad> skipped = Collections.emptyList();
        List<String> calls = new ArrayList<>();
        BlockQuadTransformer first = (state, pos, blockAccess, layer, side, quads) -> {
            calls.add("first");
            return skipped;
        };
        BlockQuadTransformer second = (state, pos, blockAccess, layer, side, quads) -> {
            calls.add("second");
            return nonEmptyQuadList();
        };
        registerForCleanup(first);
        registerForCleanup(second);

        List<BakedQuad> result = transform(input);

        assertSame(skipped, result);
        assertEquals(List.of("first"), calls);
    }

    @Test
    void rejectsNullRegistration() {
        assertThrows(IllegalArgumentException.class,
                () -> BlockQuadTransformerHolder.register(null));
    }

    @Test
    void rejectsDuplicateInstanceRegistration() {
        BlockQuadTransformer transformer =
                (state, pos, blockAccess, layer, side, quads) -> quads;
        registerForCleanup(transformer);

        assertThrows(IllegalArgumentException.class,
                () -> BlockQuadTransformerHolder.register(transformer));
    }

    @Test
    void rejectsTransformerNullResult() {
        List<BakedQuad> input = new ArrayList<>();
        BlockQuadTransformer transformer =
                (state, pos, blockAccess, layer, side, quads) -> null;
        registerForCleanup(transformer);

        assertThrows(IllegalStateException.class, () -> transform(input));
    }

    @Test
    void rethrowsTransformerRuntimeException() {
        List<BakedQuad> input = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("transformer failed");
        BlockQuadTransformer transformer =
                (state, pos, blockAccess, layer, side, quads) -> {
                    throw failure;
                };
        registerForCleanup(transformer);

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> transform(input));

        assertSame(failure, thrown);
    }

    @Test
    void unregisterPreventsLaterCalls() {
        List<BakedQuad> input = new ArrayList<>();
        List<String> calls = new ArrayList<>();
        BlockQuadTransformer transformer = (state, pos, blockAccess, layer, side, quads) -> {
            calls.add("called");
            return new ArrayList<>();
        };
        registerForCleanup(transformer);
        assertTrue(BlockQuadTransformerHolder.unregister(transformer));

        List<BakedQuad> result = transform(input);

        assertSame(input, result);
        assertTrue(calls.isEmpty());
    }

    private void registerForCleanup(BlockQuadTransformer transformer) {
        BlockQuadTransformerHolder.register(transformer);
        registered.add(transformer);
    }

    private static List<BakedQuad> transform(List<BakedQuad> quads) {
        return BlockQuadTransformerHolder.transform(null, null, null, null, null, quads);
    }

    private static List<BakedQuad> nonEmptyQuadList() {
        return List.of(new BakedQuad(
                new int[0], -1, null, null, true, DefaultVertexFormats.ITEM));
    }
}
