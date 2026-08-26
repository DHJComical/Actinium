package net.coderbot.iris.gui.screen;

import org.jetbrains.annotations.Nullable;

/**
 * Pure decision logic for {@link ShaderPackScreen#applyChanges()}.
 *
 * <p>Extracted from the screen class so the reload-trigger rules can be unit tested
 * without loading the GUI class (whose static initializers require a running
 * Minecraft instance).</p>
 */
public final class ShaderPackApplyLogic {

    private ShaderPackApplyLogic() {
    }

    /**
     * Decides whether closing the shader pack screen must trigger a shader reload.
     *
     * <p>A reload is needed when the pack would be different from before, shaders were
     * toggled, options were changed, options are about to be reset, or shaders are
     * enabled but the pack isn't loaded yet (which happens when opening the screen from
     * the main menu, where pack loading is deferred until the render system is ready).</p>
     *
     * <p>A reload is also needed when a previous load attempt fell back to fixed-function
     * rendering: the pack object still exists and the config still says "enabled", so
     * without this rule the screen would show "Shaders: Enabled" forever while nothing
     * is actually loaded, and simply closing the screen would never retry.</p>
     *
     * @param packName           the pack selected in the list
     * @param previousPackName   the pack stored in the config, or null if none
     * @param enabled            the state of the enable switch in the UI
     * @param previouslyEnabled  the state of the enable flag in the config
     * @param optionQueueEmpty   whether no shader option changes are queued
     * @param resetPending       whether an option reset was requested for the next reload
     * @param currentPackLoaded  whether a pack object is currently loaded
     * @param fallbackActive     whether a previous load attempt fell back to vanilla rendering
     * @return true if applying the changes requires a shader reload
     */
    public static boolean shouldReloadOnApply(String packName, @Nullable String previousPackName,
                                              boolean enabled, boolean previouslyEnabled,
                                              boolean optionQueueEmpty, boolean resetPending,
                                              boolean currentPackLoaded, boolean fallbackActive) {
        if (!packName.equals(previousPackName) || enabled != previouslyEnabled
                || !optionQueueEmpty || resetPending) {
            return true;
        }

        return (enabled && !currentPackLoaded) || fallbackActive;
    }
}
