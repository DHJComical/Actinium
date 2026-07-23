package com.dhj.actinium.compat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.ReEntranceLock;
import top.outlands.foundation.boot.ActualClassLoader;

import java.util.Iterator;
import java.util.Set;

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
            ReEntranceLock lock = MixinService.getService().getReEntranceLock();
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

    /**
     * Removes vanilla ({@code net.minecraft.*}) entries from the class loader's
     * invalid-class negative cache. A nested class load that fails because of the
     * re-entrance described above is recorded there permanently, so the class can
     * never be loaded for real later; vanilla classes always exist, so their
     * presence in the cache can only come from such transient failures.
     */
    public static void clearInvalidVanillaClasses() {
        try {
            ClassLoader cl = MixinReEntranceLockFix.class.getClassLoader();
            if (!(cl instanceof ActualClassLoader)) {
                return;
            }
            Set<String> invalid = ((ActualClassLoader) cl).getInvalidClasses();
            int removed = 0;
            for (Iterator<String> it = invalid.iterator(); it.hasNext(); ) {
                if (it.next().startsWith("net.minecraft.")) {
                    it.remove();
                    removed++;
                }
            }
            if (removed > 0) {
                LOGGER.warn("Removed {} vanilla class(es) from the invalid-class cache so they can be loaded for real", removed);
            }
        } catch (Throwable t) {
            LOGGER.warn("Unable to clean the invalid-class cache", t);
        }
    }

    /**
     * Loads the given classes without initializing them, so their transformations
     * (including mixins) are applied up front. Used for mixin targets whose first
     * real use would otherwise happen nested inside another class's transform
     * (e.g. Techguns resolving the Tessellator hierarchy), which mixin cannot
     * handle re-entrantly.
     */
    public static void preloadClasses(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            LOGGER.debug("Pre-loaded {}", clazz.getName());
        }
    }
}
