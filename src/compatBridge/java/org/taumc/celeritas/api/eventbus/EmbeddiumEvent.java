package org.taumc.celeritas.api.eventbus;

/**
 * Standalone legacy event base with no dependency on Actinium's event classes.
 */
public abstract class EmbeddiumEvent {
    private boolean canceled;

    public boolean isCancelable() {
        return false;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
