package org.taumc.celeritas.api.options.structure;

/**
 * Legacy option change flags.
 */
public enum OptionFlag {
    REQUIRES_RENDERER_RELOAD,
    REQUIRES_RENDERER_UPDATE,
    REQUIRES_ASSET_RELOAD,
    REQUIRES_GAME_RESTART,
    REQUIRES_SHADER_PIPELINE_RELOAD
}
