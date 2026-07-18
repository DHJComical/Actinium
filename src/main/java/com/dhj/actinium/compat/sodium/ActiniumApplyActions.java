package com.dhj.actinium.compat.sodium;

/**
 * Defines the Minecraft-side actions triggered by applied Config flags so the mapping remains directly testable.
 */
public interface ActiniumApplyActions {
    /** Rebuilds the renderer when an option invalidates renderer resources. */
    void reloadRenderer();

    /** Marks renderer state dirty when a full rebuild is unnecessary. */
    void updateRenderer();

    /** Reloads assets after texture-related options change. */
    void reloadAssets();

    /** Presents the existing restart-required notification. */
    void showRestartRequired();
}
