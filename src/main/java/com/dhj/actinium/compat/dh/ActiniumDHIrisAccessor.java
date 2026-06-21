package com.dhj.actinium.compat.dh;

import com.dhj.actinium.shadows.ShadowRenderingState;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import net.coderbot.iris.Iris;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;

public class ActiniumDHIrisAccessor implements IIrisAccessor {
    @Override
    public String getModName() {
        return Iris.MODNAME;
    }

    @Override
    public boolean isShaderPackInUse() {
        return Iris.enabled && IrisApiV0Impl.INSTANCE.isShaderPackInUse();
    }

    @Override
    public boolean isRenderingShadowPass() {
        return ShadowRenderingState.areShadowsCurrentlyBeingRendered();
    }
}
