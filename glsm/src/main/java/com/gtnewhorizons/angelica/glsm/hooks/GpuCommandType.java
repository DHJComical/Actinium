package com.gtnewhorizons.angelica.glsm.hooks;

/**
 * Identifies GPU commands emitted through GLSM for crash-resilient diagnostics.
 */
public enum GpuCommandType {
    /** Changes the active shader program. */
    PROGRAM_USE(1),
    /** Changes a draw or read framebuffer binding. */
    FRAMEBUFFER_BIND(2),
    /** Changes a texture binding. */
    TEXTURE_BIND(3),
    /** Submits a non-indexed draw. */
    DRAW_ARRAYS(4),
    /** Submits an indexed draw. */
    DRAW_ELEMENTS(5),
    /** Clears one or more framebuffer attachments. */
    CLEAR(6),
    /** Copies framebuffer pixels into a new texture image. */
    COPY_TEX_IMAGE_2D(7),
    /** Copies framebuffer pixels into an existing texture image. */
    COPY_TEX_SUB_IMAGE_2D(8),
    /** Defines a two-dimensional texture image. */
    TEX_IMAGE_2D(9),
    /** Updates a region of a two-dimensional texture image. */
    TEX_SUB_IMAGE_2D(10),
    /** Copies pixels between framebuffers. */
    BLIT_FRAMEBUFFER(11),
    /** Dispatches compute work groups. */
    DISPATCH_COMPUTE(12),
    /** Generates texture mipmaps. */
    GENERATE_MIPMAP(13),
    /** Marks a completed semantic render-pass boundary. */
    PASS_END(14),
    /** Clears a texture image without drawing. */
    CLEAR_TEX_IMAGE(15),
    /** Allocates immutable two-dimensional texture storage. */
    TEX_STORAGE_2D(16),
    /** Copies texels directly between image objects. */
    COPY_IMAGE_SUB_DATA(17),
    /** Dispatches compute work groups from an indirect buffer. */
    DISPATCH_COMPUTE_INDIRECT(18),
    /** Defines a three-dimensional texture image. */
    TEX_IMAGE_3D(19),
    /** Updates a region of a three-dimensional texture image. */
    TEX_SUB_IMAGE_3D(20),
    /** Allocates immutable three-dimensional texture storage. */
    TEX_STORAGE_3D(21);

    private final int code;

    GpuCommandType(int code) {
        this.code = code;
    }

    /**
     * Returns the stable numeric code persisted in a flight recording.
     *
     * @return stable command code
     */
    public int code() {
        return code;
    }

    /**
     * Resolves a persisted command code during offline analysis.
     *
     * @param code persisted command code
     * @return matching command type
     * @throws IllegalArgumentException when the code is unknown
     */
    public static GpuCommandType fromCode(long code) {
        for (GpuCommandType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown GPU command code: " + code);
    }
}
