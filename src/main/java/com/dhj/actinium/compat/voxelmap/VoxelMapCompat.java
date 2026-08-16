package com.dhj.actinium.compat.voxelmap;

import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mamiyaotaru.voxelmap.util.GLUtils;

/**
 * Compatibility bridge that routes the VoxelMap minimap (mod id {@code voxelmap}) onto the plain
 * CPU-texture render path used by vanilla-style OpenGL contexts.
 *
 * <p>VoxelMap decides its minimap pipeline from two flags probed at class-load time:</p>
 *
 * <ul>
 *   <li>{@code GLUtils.hasAlphaBits} - true when the default framebuffer carries an alpha channel;
 *   VoxelMap then draws the minimap texture directly to the screen.</li>
 *   <li>{@code GLUtils.fboEnabled} - when there is no alpha channel VoxelMap falls back to an
 *   {@code EXT_framebuffer_object} render target and fixed-function rasterization.</li>
 * </ul>
 *
 * <p>Under Actinium the window is created by the LWJGL2 compatibility layer without alpha bits, so
 * {@code hasAlphaBits} is false and VoxelMap selects the legacy FBO path, which produces a black
 * minimap on the core profile. {@link #forceCpuMinimapPath()} re-selects the direct CPU-texture
 * path, which is the same path vanilla VoxelMap uses on ordinary systems.</p>
 *
 * <p>That path also sets {@code GL_TEXTURE_MIN_FILTER} to {@code GL_LINEAR_MIPMAP_LINEAR} and relies
 * on the legacy {@code GL_GENERATE_MIPMAP} texture parameter to build the mipmap chain. Core
 * profiles ignore {@code GL_GENERATE_MIPMAP}, leaving the map texture with only level 0, and
 * sampling an incomplete texture returns opaque black. {@link #needsLinearMinFilter} /
 * {@link #linearMinFilter} downgrade that filter to plain {@code GL_LINEAR}.</p>
 */
public final class VoxelMapCompat {
    /** {@code GL_TEXTURE_MIN_FILTER} - texture parameter name for minification filtering. */
    private static final int GL_TEXTURE_MIN_FILTER = 10241;
    /** {@code GL_LINEAR_MIPMAP_LINEAR} - minification filter that requires a full mipmap chain. */
    private static final int GL_LINEAR_MIPMAP_LINEAR = 9987;
    /** {@code GL_LINEAR} - plain linear minification filter that works without mipmaps. */
    private static final int GL_LINEAR = 9729;

    /**
     * True while {@code Map.renderMap} has scoped the minimap alpha clear to the map rectangle
     * (see {@code MixinVoxelMapRenderMap}). Shared between the renderMap and GLShim mixins so the
     * mixin classes themselves stay free of cross-class state.
     */
    public static boolean mapScissorActive;

    private VoxelMapCompat() {
    }

    /**
     * Forces VoxelMap onto the direct CPU-texture minimap path regardless of the probed
     * framebuffer capabilities. Safe on any context: this is the path VoxelMap uses by default
     * whenever the framebuffer has alpha bits, and the FBO path is never required by it.
     */
    public static void forceCpuMinimapPath() {
        GLUtils.hasAlphaBits = true;
        GLUtils.fboEnabled = false;
    }

    /**
     * Whether a texture parameter update selects the mipmap-based minification filter that cannot
     * work in core-profile contexts (no {@code GL_GENERATE_MIPMAP}, so the texture has no mipmap
     * chain and sampling returns opaque black).
     *
     * @param pname texture parameter name
     * @param param requested parameter value
     * @return true when the update must be downgraded to plain linear filtering
     */
    public static boolean needsLinearMinFilter(int pname, int param) {
        return pname == GL_TEXTURE_MIN_FILTER && param == GL_LINEAR_MIPMAP_LINEAR;
    }

    /** @return the plain linear minification filter used as the downgrade target. */
    public static int linearMinFilter() {
        return GL_LINEAR;
    }
}
