package org.taumc.celeritas.compat;

import java.util.function.Supplier;

/**
 * Tracks bridge-owned current model construction so it is not dispatched back to legacy listeners.
 */
final class BridgeDispatchGuard {
    static final BridgeDispatchGuard INSTANCE = new BridgeDispatchGuard();
    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    private BridgeDispatchGuard() {
    }

    boolean isSuppressed() {
        return depth.get() > 0;
    }

    <T> T suppress(Supplier<T> action) {
        depth.set(depth.get() + 1);
        try {
            return action.get();
        } finally {
            int nextDepth = depth.get() - 1;
            if (nextDepth == 0) {
                depth.remove();
            } else {
                depth.set(nextDepth);
            }
        }
    }
}
