package com.gtnewhorizons.angelica.glsm.hooks;

/**
 * Initialization configuration for the GLSM (GL State Manager).
 * Controls which features are enabled during GLSM setup.
 * Ported from Angelica.
 */
public class GLSMInitConfig {
    private final boolean enableFfpEmulation;
    private final boolean enableDisplayListRecording;
    private final boolean enableShaderTransformation;
    private final boolean enableStateCaching;
    private final boolean enableDsaSupport;

    private GLSMInitConfig(Builder builder) {
        this.enableFfpEmulation = builder.enableFfpEmulation;
        this.enableDisplayListRecording = builder.enableDisplayListRecording;
        this.enableShaderTransformation = builder.enableShaderTransformation;
        this.enableStateCaching = builder.enableStateCaching;
        this.enableDsaSupport = builder.enableDsaSupport;
    }

    public boolean isFfpEmulationEnabled() { return enableFfpEmulation; }
    public boolean isDisplayListRecordingEnabled() { return enableDisplayListRecording; }
    public boolean isShaderTransformationEnabled() { return enableShaderTransformation; }
    public boolean isStateCachingEnabled() { return enableStateCaching; }
    public boolean isDsaSupportEnabled() { return enableDsaSupport; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enableFfpEmulation = false;
        private boolean enableDisplayListRecording = false;
        private boolean enableShaderTransformation = true;
        private boolean enableStateCaching = true;
        private boolean enableDsaSupport = false;

        public Builder enableFfpEmulation(boolean v) { this.enableFfpEmulation = v; return this; }
        public Builder enableDisplayListRecording(boolean v) { this.enableDisplayListRecording = v; return this; }
        public Builder enableShaderTransformation(boolean v) { this.enableShaderTransformation = v; return this; }
        public Builder enableStateCaching(boolean v) { this.enableStateCaching = v; return this; }
        public Builder enableDsaSupport(boolean v) { this.enableDsaSupport = v; return this; }

        public GLSMInitConfig build() {
            return new GLSMInitConfig(this);
        }
    }

    public static GLSMInitConfig defaults() {
        return builder().build();
    }
}
