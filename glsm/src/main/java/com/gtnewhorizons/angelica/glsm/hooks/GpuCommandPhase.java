package com.gtnewhorizons.angelica.glsm.hooks;

/**
 * Describes whether a GPU command breadcrumb is instantaneous or a low-frequency boundary.
 */
public enum GpuCommandPhase {
    /** The command is about to be submitted and has no paired return breadcrumb. */
    ISSUED,
    /** A low-frequency native command is about to be submitted. */
    BEGIN,
    /** A low-frequency native command returned to Java. */
    END
}
