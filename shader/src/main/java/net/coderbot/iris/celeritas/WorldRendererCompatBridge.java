package net.coderbot.iris.celeritas;

/**
 * Registration point for the world-terrain renderer used by the shadow pass.
 * The host mod registers its implementation at startup.
 */
public final class WorldRendererCompatBridge {

    /** Supplies the current world renderer instance. */
    public interface Provider {
        WorldRendererCompat instance();
    }

    private static volatile Provider provider;

    private WorldRendererCompatBridge() {
    }

    public static void setProvider(Provider newProvider) {
        provider = newProvider;
    }

    public static WorldRendererCompat instance() {
        Provider p = provider;
        if (p == null) {
            throw new IllegalStateException("WorldRendererCompat provider is not registered");
        }
        return p.instance();
    }
}
