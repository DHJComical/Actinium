package com.dhj.actinium.compat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.transformer.MixinProcessor;
import org.spongepowered.asm.mixin.transformer.Proxy;
import org.spongepowered.asm.util.ReEntranceLock;

import java.lang.reflect.Field;

/**
 * Works around a re-entrance lock leak in the Cleanroom sponge-mixin 0.8.7 build.
 *
 * <p>During mixin select/prepare, legacy coremod ASM transformers (e.g. Techguns'
 * TechgunsASMTransformer) may trigger nested class loads via
 * {@code ClassWriter#getCommonSuperClass}. When the nested-loaded class is a mixin
 * target, {@code MixinProcessor.lockAndSelect}/{@code applyMixins} throw
 * {@code ReEntrantTransformerError} without popping the processor's
 * {@link ReEntranceLock}. The leaked lock depth makes every later
 * {@code applyMixins} call throw, which aborts the launch with
 * {@code NoClassDefFoundError: net/minecraft/client/Minecraft}.
 *
 * <p>Once mixin select has completed and no transform is in progress on the
 * current thread, the lock depth must be zero; anything above zero is leaked.
 * Calling {@link #clearLeakedLock()} at that point restores the balanced state.
 */
public final class MixinReEntranceLockFix {

    private static final Logger LOGGER = LogManager.getLogger("Actinium");

    private MixinReEntranceLockFix() {
    }

    /**
     * Clears any leaked mixin re-entrance lock depth. Safe to call at any time
     * outside an active class transform; never throws.
     */
    public static void clearLeakedLock() {
        try {
            MixinProcessor processor = Proxy.transformer.processor;
            Field lockField = MixinProcessor.class.getDeclaredField("lock");
            lockField.setAccessible(true);
            ReEntranceLock lock = (ReEntranceLock) lockField.get(processor);
            if (lock != null && lock.getDepth() > 0) {
                LOGGER.warn("Detected leaked mixin re-entrance lock (depth {}), draining it to keep class transforms working", lock.getDepth());
                drain(lock);
            }
        } catch (Throwable t) {
            LOGGER.warn("Mixin re-entrance lock fix unavailable; launch may fail if a legacy transformer re-enters mixin", t);
        }
    }

    /**
     * Drains the lock depth back to zero. {@link ReEntranceLock#clear()} only resets
     * the semaphore and does not touch the depth, while {@code check()} (used by
     * {@code MixinProcessor.lockAndSelect}) compares depth against maxDepth, so the
     * leaked depth has to be popped away explicitly.
     */
    static void drain(ReEntranceLock lock) {
        while (lock.getDepth() > 0) {
            lock.pop();
        }
        lock.clear();
    }
}
