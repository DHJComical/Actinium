package net.irisshaders.iris.api.v0;

public interface IrisApiConfig {
    boolean areShadersEnabled();
    void setShadersEnabledAndApply(boolean enabled);
}
