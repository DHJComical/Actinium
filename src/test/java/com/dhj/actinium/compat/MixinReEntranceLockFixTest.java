package com.dhj.actinium.compat;

import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.transformer.MixinProcessor;
import org.spongepowered.asm.mixin.transformer.MixinTransformer;
import org.spongepowered.asm.mixin.transformer.Proxy;
import org.spongepowered.asm.util.ReEntranceLock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the reflective contract {@link MixinReEntranceLockFix} relies on. If the
 * bundled mixin build changes these internals, these tests fail so the shim can be
 * updated in the same change.
 */
class MixinReEntranceLockFixTest {

    @Test
    void proxyExposesTransformerStatically() throws Exception {
        Field transformer = Proxy.class.getDeclaredField("transformer");
        assertTrue(Modifier.isPublic(transformer.getModifiers()), "Proxy.transformer must be public");
        assertTrue(Modifier.isStatic(transformer.getModifiers()), "Proxy.transformer must be static");
        assertEquals(MixinTransformer.class, transformer.getType());
    }

    @Test
    void mixinTransformerExposesProcessor() throws Exception {
        Field processor = MixinTransformer.class.getDeclaredField("processor");
        assertTrue(Modifier.isPublic(processor.getModifiers()), "MixinTransformer.processor must be public");
        assertEquals(MixinProcessor.class, processor.getType());
    }

    @Test
    void mixinProcessorHasReEntranceLockField() throws Exception {
        Field lock = MixinProcessor.class.getDeclaredField("lock");
        assertEquals(ReEntranceLock.class, lock.getType());
    }

    @Test
    void reEntranceLockExposesDepthAndClear() throws Exception {
        assertNotNull(ReEntranceLock.class.getMethod("getDepth"));
        assertNotNull(ReEntranceLock.class.getMethod("clear"));
    }

    @Test
    void drainPopsLeakedDepthBackToZero() {
        ReEntranceLock lock = new ReEntranceLock(1);
        lock.push(); // outer lockAndSelect
        lock.push(); // nested lockAndSelect that throws without popping
        lock.pop();  // outer call pops once, leaving the leaked depth of 1
        assertEquals(1, lock.getDepth());
        MixinReEntranceLockFix.drain(lock);
        assertEquals(0, lock.getDepth());
    }

    @Test
    void clearLeakedLockNeverThrowsOutsideLaunchEnvironment() {
        assertDoesNotThrow(MixinReEntranceLockFix::clearLeakedLock);
    }
}
