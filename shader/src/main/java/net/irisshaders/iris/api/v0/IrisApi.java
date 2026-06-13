package net.irisshaders.iris.api.v0;

import net.coderbot.iris.apiimpl.IrisApiV0Impl;

public interface IrisApi {
    static IrisApi getInstance() {
        return IrisApiV0Impl.INSTANCE;
    }

    int getMinorApiRevision();

    boolean isShaderPackInUse();

    boolean isRenderingShadowPass();

    Object openMainIrisScreenObj(Object parent);

    String getMainScreenLanguageKey();

    IrisApiConfig getConfig();
}
