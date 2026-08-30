package com.dhj.actinium.compat.hbm;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

/**
 * Adapts HBM's private attribute-mask format to the GLSM state model.
 *
 * <p>HBM's {@code RenderUtil} stores a vanilla-GlStateManager snapshot. That snapshot is not
 * authoritative after GLSM redirects the renderer, so HBM state scopes must use the same stack
 * as the rest of the client.</p>
 */
public final class HbmRenderStateCompat {
    private static final String HBM_PACKAGE_PREFIX = "com.hbm.";
    private static final int HBM_ENABLE_BIT = 0x02000;
    private static final int HBM_LIGHTING_BIT = 0x00040;
    private static final int HBM_TEXTURE_BIT = 0x40000;
    private static final int HBM_COLOR_BUFFER_BIT = 0x04000;
    private static final int HBM_DEPTH_BUFFER_BIT = 0x00100;
    private static final int HBM_POLYGON_BIT = 0x00008;
    private static final int HBM_FOG_BIT = 0x00080;
    private static final int HBM_ALL_BITS = 0xFFFFF;
    private static final int HBM_SUPPORTED_BITS = 0x461C8;

    private HbmRenderStateCompat() {
    }

    /** Pushes the equivalent GLSM state for an HBM attribute scope. */
    public static void pushAttrib(int hbmMask) {
        GLStateManager.glPushAttrib(toGlMask(hbmMask));
    }

    /** Pops the GLSM state saved for an HBM attribute scope. */
    public static void popAttrib() {
        GLStateManager.glPopAttrib();
    }

    /** Returns whether the tile entity belongs to HBM's renderer family. */
    public static boolean isHbmTile(TileEntity tileEntity) {
        return tileEntity.getClass().getName().startsWith(HBM_PACKAGE_PREFIX);
    }

    /**
     * Sets the current lightmap coordinate from a block entity's world position.
     *
     * <p>The dispatcher skips vanilla's lightmap update for fast renderers. HBM has renderers
     * that issue raw GL draws without a vertex lightmap attribute, so the current coordinate must
     * be valid before either renderer kind is entered.</p>
     */
    public static void setWorldLightmap(TileEntity tileEntity) {
        int combinedLight = tileEntity.getWorld().getCombinedLight(tileEntity.getPos(), 0);
        GLStateManager.setLightmapTextureCoords(
            OpenGlHelper.lightmapTexUnit,
            blockLight(combinedLight),
            skyLight(combinedLight));
    }

    static int toGlMask(int hbmMask) {
        int normalizedMask = hbmMask == HBM_ALL_BITS ? HBM_SUPPORTED_BITS : hbmMask;
        int unsupportedBits = normalizedMask & ~HBM_SUPPORTED_BITS;
        if (unsupportedBits != 0) {
            throw new IllegalArgumentException(
                "Unsupported HBM RenderUtil attribute bits: 0x" + Integer.toHexString(unsupportedBits));
        }

        // HBM always captures shade model, which GLSM groups with GL_LIGHTING_BIT.
        int glMask = GL11.GL_LIGHTING_BIT;
        if ((normalizedMask & HBM_ENABLE_BIT) != 0) glMask |= GL11.GL_ENABLE_BIT;
        if ((normalizedMask & HBM_LIGHTING_BIT) != 0) glMask |= GL11.GL_LIGHTING_BIT;
        if ((normalizedMask & HBM_TEXTURE_BIT) != 0) glMask |= GL11.GL_TEXTURE_BIT;
        // HBM's color snapshot also contains the current RGBA vertex color. Vanilla OpenGL keeps
        // that state in GL_CURRENT_BIT, so include it or HBM color changes leak into later draws.
        if ((normalizedMask & HBM_COLOR_BUFFER_BIT) != 0) {
            glMask |= GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT;
        }
        if ((normalizedMask & HBM_DEPTH_BUFFER_BIT) != 0) glMask |= GL11.GL_DEPTH_BUFFER_BIT;
        if ((normalizedMask & HBM_POLYGON_BIT) != 0) glMask |= GL11.GL_POLYGON_BIT;
        if ((normalizedMask & HBM_FOG_BIT) != 0) glMask |= GL11.GL_FOG_BIT;
        return glMask;
    }

    static int blockLight(int combinedLight) {
        return combinedLight & 0xFFFF;
    }

    static int skyLight(int combinedLight) {
        return combinedLight >>> 16;
    }
}
