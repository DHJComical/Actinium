package com.dhj.actinium.mixin.mod.extrautils2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.function.Function;

/**
 * Fixes the ConcurrentModificationException crash while building chunk meshes for Extra
 * Utilities 2 blocks (e.g. an ender lilly) under Actinium.
 *
 * <p>{@code XUBlockStatic$3} (the baked model wrapper each block state gets) bakes its quad lists
 * lazily inside plain {@code HashMap}s ({@code cachedLists} plus the per-facing maps nested inside
 * it) via {@code computeIfAbsent}. The mapping function is expensive (it rebuilds a
 * {@code MutableModel}), so the window in which two threads race inside the map is real. Vanilla
 * only ever calls {@code IBakedModel#getQuads} from the single chunk rebuild worker, but Actinium
 * builds chunk meshes on several worker threads and portal view passes can rebuild the same
 * section again while the main pass is still building it, so two threads can enter
 * {@code getQuads} for the same block state at once and corrupt the map.</p>
 *
 * <p>Both {@code computeIfAbsent} call sites in {@code getQuads} are redirected to serialize on
 * the receiver map's own monitor: every level of the cache is then only ever read or mutated
 * while holding its own lock, which keeps first-bake results consistent (and deduplicated) without
 * changing the returned quads. The nested maps are created while the outer lock is held, so a
 * freshly-created inner map cannot be observed unsynchronized.</p>
 *
 * <p>The mixin keeps remapping enabled: the target class is an Extra Utilities 2 class, but
 * {@code getQuads} implements Minecraft's {@code IBakedModel} interface, so its name is the
 * searge {@code func_188616_a} in the production jar and must go through the refmap.</p>
 */
@Mixin(targets = "com.rwtema.extrautils2.backend.XUBlockStatic$3")
public abstract class MixinXUBlockStaticLazyQuads {

    @Redirect(
            method = "getQuads",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/HashMap;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"),
            require = 2)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object celeritas$synchronizedComputeIfAbsent(HashMap map, Object key, Function mappingFunction) {
        synchronized (map) {
            return map.computeIfAbsent(key, mappingFunction);
        }
    }
}
