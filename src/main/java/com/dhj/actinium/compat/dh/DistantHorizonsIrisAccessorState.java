package com.dhj.actinium.compat.dh;

import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;

/** Resolves whether Distant Horizons should treat the registered Iris accessor as active this frame. */
public final class DistantHorizonsIrisAccessorState {
    private DistantHorizonsIrisAccessorState() {
    }

    /**
     * Returns the accessor only while its shader pack is active, preserving Distant Horizons' null-based branches.
     *
     * @param accessor the accessor cached by Distant Horizons during class initialization
     * @return the original accessor while shaders are active, otherwise {@code null}
     */
    public static IIrisAccessor activeAccessor(IIrisAccessor accessor) {
        return accessor != null && accessor.isShaderPackInUse() ? accessor : null;
    }
}
