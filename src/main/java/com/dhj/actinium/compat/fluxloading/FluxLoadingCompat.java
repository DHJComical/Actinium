package com.dhj.actinium.compat.fluxloading;

import net.minecraftforge.fml.common.Loader;

/**
 * Compatibility for FluxLoading (mod id {@code fluxloading}).
 *
 * <p>FluxLoading drives its loading-screen state machine from chunk-compile notifications. Its
 * Celeritas hook is a late mixin that only loads when {@code Loader.isModLoaded("celeritas")}
 * passes, and the vanilla {@code ChunkRenderWorker.processTask} hook never fires because Actinium
 * meshes chunks on its own builder threads. In a production install neither source exists, so the
 * state machine stalls in {@code DEFAULT_WORLD_LOADING} forever and the loading screen never fades
 * out after re-entering a world (#102); dev runs do not reproduce this because {@code runClient}
 * installs the compat-bridge jar whose mod id satisfies the check.</p>
 *
 * <p>The compat forwards the same per-frame signal FluxLoading's own Celeritas mixin would emit:
 * {@code RenderSectionManager.updateChunks} returning in {@link
 * com.dhj.actinium.render.terrain.VintageRenderSectionManager}. Forwarding is guarded by mod
 * presence, and every reference to FluxLoading classes lives in {@link FluxLoadingNotifyForwarder}
 * so they are never class-loaded unless FluxLoading is installed.</p>
 */
public final class FluxLoadingCompat {
    /**
     * The mod ID of FluxLoading.
     */
    public static final String MODID = "fluxloading";
    public static final boolean IS_LOADED = Loader.isModLoaded(MODID);

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
