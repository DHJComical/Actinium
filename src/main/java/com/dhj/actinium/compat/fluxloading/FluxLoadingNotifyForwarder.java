package com.dhj.actinium.compat.fluxloading;

import com.tttsaurus.fluxloading.core.FluxLoadingManager;
import com.tttsaurus.fluxloading.core.chunk.gate.FluxLoadingChunkSource;

/**
 * Sole holder of FluxLoading class references for the compat. Only reached behind the
 * {@code FluxLoadingCompat.IS_LOADED} guard, so the FluxLoading classes are never resolved on a
 * production install without the mod.
 */
final class FluxLoadingNotifyForwarder {
    private FluxLoadingNotifyForwarder() {
    }

    /**
     * Replays the per-frame signal of FluxLoading's own Celeritas mixin. The {@code CELERITAS}
     * source is the instant-complete variant: FluxLoading never waits for chunk counts behind a
     * Celeritas-class renderer, it just proceeds to the fade-out sequence.
     */
    static void notifyChunkCompileTaskProcessed() {
        FluxLoadingManager.onChunkCompileTaskProcessed(FluxLoadingChunkSource.CELERITAS);
    }
}
