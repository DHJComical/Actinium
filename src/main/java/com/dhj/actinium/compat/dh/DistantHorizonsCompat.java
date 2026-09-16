package com.dhj.actinium.compat.dh;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.rendering.RenderingState;
import com.gtnewhorizon.gtnhlib.compat.Mods;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.util.math.DhMat4f;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL15;
import com.dhj.actinium.mixin.vintage.core.terrain.AccessorEntityRenderer;

/**
 * Render-state bridge between Actinium's terrain pass and Distant Horizons' LOD renderer.
 *
 * <p>Distant Horizons owns its own Actinium/Iris integration (the {@code IIrisAccessor} binding and
 * the deferred transparent LOD toggle); Actinium no longer injects into DH and only prepares its
 * render state, then drives DH's deferred pass from its own {@code RenderGlobal} override.</p>
 */
public final class DistantHorizonsCompat {
    private static final Logger LOGGER = LogManager.getLogger("ActiniumDHCompat");

    private static boolean loggedFirstDeferredRender;
    private static boolean warnedRenderFailure;
    private static boolean warnedLightmapSyncFailure;
    private static boolean warnedFogColorSyncFailure;

    private DistantHorizonsCompat() {
    }

    /**
     * Prepares Actinium's render state and runs Distant Horizons' deferred transparent LOD pass.
     * Called from Actinium's {@code RenderGlobal.renderBlockLayer} override after Iris entered its
     * translucent phase; the call is inert while DH keeps its deferred toggle disabled.
     */
    public static void renderDeferredLodsForShaders(WorldClient world, double partialTicks) {
        if (world == null || !Mods.DISTANTHORIZONS) {
            return;
        }

        try {
            if (!prepareLodState(world, partialTicks)) {
                return;
            }

            ClientApi.INSTANCE.renderDeferredLodsForShaders();
            if (!loggedFirstDeferredRender) {
                loggedFirstDeferredRender = true;
                LOGGER.info("Distant Horizons shader LOD bridge called renderDeferredLodsForShaders for the first frame");
            }

            GLStateManager.glBindVertexArray(0);
            GLStateManager.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GLStateManager.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GLStateManager.glUseProgram(0);
        } catch (Throwable t) {
            logRenderFailure("render Distant Horizons deferred LODs", t);
        }
    }

    /**
     * Copies Actinium's live GLSM matrices into Distant Horizons' render state and syncs the fog
     * color and lightmap DH samples while drawing LODs.
     *
     * @return whether DH's deferred transparent LOD toggle is enabled for this frame
     */
    private static boolean prepareLodState(WorldClient world, double partialTicks) {
        if (!isDeferredLodRenderingEnabledForShaders()) {
            return false;
        }

        ClientApi.RENDER_STATE.mcProjectionMatrix = copyJomlMatrix(RenderingState.INSTANCE.getProjectionMatrix());
        ClientApi.RENDER_STATE.mcModelViewMatrix = copyJomlMatrix(RenderingState.INSTANCE.getModelViewMatrix());
        ClientApi.RENDER_STATE.partialTickTime = (float) partialTicks;
        ClientApi.RENDER_STATE.clientLevelWrapper = ClientLevelWrapper.getWrapperIfDifferent(
            ClientApi.RENDER_STATE.clientLevelWrapper,
            world
        );
        syncFogColor();
        syncLightmap();
        return true;
    }

    private static void logRenderFailure(String operation, Throwable throwable) {
        if (!warnedRenderFailure) {
            warnedRenderFailure = true;
            LOGGER.warn("Failed to {} through the Actinium bridge", operation, throwable);
        }
    }

    /**
     * Reads the Distant Horizons deferred transparent LOD toggle. DH owns that flag (its own config
     * decides it); Actinium only observes it to know whether the deferred pass must be driven this
     * frame, and never writes it back.
     */
    private static boolean isDeferredLodRenderingEnabledForShaders() {
        return DhApi.Delayed.renderProxy != null && DhApi.Delayed.renderProxy.getDeferTransparentRendering();
    }

    private static DhMat4f copyJomlMatrix(Matrix4f sourceMatrix) {
        DhMat4f matrix = new DhMat4f();

        matrix.m00 = sourceMatrix.m00();
        matrix.m01 = sourceMatrix.m10();
        matrix.m02 = sourceMatrix.m20();
        matrix.m03 = sourceMatrix.m30();

        matrix.m10 = sourceMatrix.m01();
        matrix.m11 = sourceMatrix.m11();
        matrix.m12 = sourceMatrix.m21();
        matrix.m13 = sourceMatrix.m31();

        matrix.m20 = sourceMatrix.m02();
        matrix.m21 = sourceMatrix.m12();
        matrix.m22 = sourceMatrix.m22();
        matrix.m23 = sourceMatrix.m32();

        matrix.m30 = sourceMatrix.m03();
        matrix.m31 = sourceMatrix.m13();
        matrix.m32 = sourceMatrix.m23();
        matrix.m33 = sourceMatrix.m33();

        return matrix;
    }

    private static void syncLightmap() {
        try {
            DynamicTexture lightmapTexture = ((AccessorEntityRenderer) Minecraft.getMinecraft().entityRenderer).getLightmapTexture();
            MinecraftRenderWrapper renderWrapper = (MinecraftRenderWrapper) SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
            if (lightmapTexture != null && renderWrapper != null) {
                renderWrapper.setLightmapId(lightmapTexture.getGlTextureId());
            }
        } catch (Throwable t) {
            if (!warnedLightmapSyncFailure) {
                warnedLightmapSyncFailure = true;
                LOGGER.warn("Failed to sync Distant Horizons lightmap before LOD rendering", t);
            }
        }
    }

    private static void syncFogColor() {
        try {
            Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
        } catch (Throwable t) {
            if (!warnedFogColorSyncFailure) {
                warnedFogColorSyncFailure = true;
                LOGGER.warn("Failed to sync vanilla fog color before Distant Horizons LOD rendering", t);
            }
        }
    }
}
