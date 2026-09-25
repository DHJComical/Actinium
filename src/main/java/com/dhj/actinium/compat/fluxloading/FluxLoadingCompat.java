package com.dhj.actinium.compat.fluxloading;

import com.gtnewhorizon.gtnhlib.compat.Mods;

/**
 * Compatibility for FluxLoading (mod id {@code fluxloading}).
 *
 * <p>FluxLoading drives its loading-screen state machine from chunk-compile notifications. Its
 * Celeritas hook is a late mixin that only loads when the {@code celeritas} mod id is present,
 * and the vanilla {@code ChunkRenderWorker.processTask} hook never fires because Actinium
 * meshes chunks on its own builder threads. Actinium does not provide the {@code celeritas} mod id,
 * so without this compat the state machine stalls in {@code DEFAULT_WORLD_LOADING} forever and the
 * loading screen never fades out after re-entering a world (#102).</p>
 *
 * <p>The compat forwards the same per-frame signal FluxLoading's own Celeritas mixin would emit:
 * {@code RenderSectionManager.updateChunks} returning in {@link
 * com.dhj.actinium.render.terrain.VintageRenderSectionManager}. Forwarding is guarded by mod
 * presence, and every reference to FluxLoading classes lives in {@link FluxLoadingNotifyForwarder}
 * so they are never class-loaded unless FluxLoading is installed.</p>
 */
public final class FluxLoadingCompat {
    public static final boolean IS_LOADED = Mods.FLUXLOADING;

    private FluxLoadingCompat() {
    }

    /**
     * Forwards FluxLoading's Celeritas chunk-compile signal once per {@code updateChunks} frame.
     * FluxLoading ignores the signal whenever its state machine is not in a phase that consumes
     * it, so forwarding unconditionally while installed matches the mixin semantics.
     */
    public static void onRenderSectionManagerUpdateChunks() {
        if (IS_LOADED) {
            FluxLoadingNotifyForwarder.notifyChunkCompileTaskProcessed();
        }
    }
}
