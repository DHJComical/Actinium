package com.dhj.actinium.compat.dh;

import com.gtnewhorizons.angelica.rendering.RenderingState;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.render.RenderBufferHandler;
import com.seibel.distanthorizons.core.util.math.DhMat4f;
import com.seibel.distanthorizons.core.world.IDhClientWorld;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL32;
import org.taumc.celeritas.mixin.core.terrain.AccessorEntityRenderer;

public final class DistantHorizonsCompat {
    private static final Logger LOGGER = LogManager.getLogger("ActiniumDHCompat");
    private static final String MODID = "distanthorizons";

    private static boolean loggedFirstRender;
    private static boolean warnedRenderFailure;
    private static boolean warnedLightmapSyncFailure;
    private static boolean warnedFogColorSyncFailure;
    private static String lastDiagnosticSignature = "";
    private static long lastDiagnosticLogTimeMs;

    private DistantHorizonsCompat() {
    }

    public static void renderVanillaLods(WorldClient world, double partialTicks) {
        if (world == null || !Loader.isModLoaded(MODID)) {
            return;
        }

        try {
            ClientApi.RENDER_STATE.mcProjectionMatrix = copyJomlMatrix(RenderingState.INSTANCE.getProjectionMatrix());
            ClientApi.RENDER_STATE.mcModelViewMatrix = copyJomlMatrix(RenderingState.INSTANCE.getModelViewMatrix());

            ClientApi.RENDER_STATE.partialTickTime = (float) partialTicks;
            ClientApi.RENDER_STATE.clientLevelWrapper = ClientLevelWrapper.getWrapperIfDifferent(ClientApi.RENDER_STATE.clientLevelWrapper, world);
            syncFogColor();
            syncLightmap();

            ClientApi.INSTANCE.renderLods();
            if (!loggedFirstRender) {
                loggedFirstRender = true;
                LOGGER.info("Distant Horizons vanilla LOD bridge called renderLods for the first frame");
            }
            logRenderDiagnostics();

            GL32.glBindVertexArray(0);
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, 0);
            GL32.glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL32.glUseProgram(0);
        } catch (Throwable t) {
            if (!warnedRenderFailure) {
                warnedRenderFailure = true;
                LOGGER.warn("Failed to render Distant Horizons LODs through the Actinium bridge", t);
            }
        }
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

    private static void logRenderDiagnostics() {
        IClientLevelWrapper levelWrapper = ClientApi.RENDER_STATE.clientLevelWrapper;
        IDhClientWorld dhWorld = SharedApi.tryGetDhClientWorld();
        IDhClientLevel dhLevel = dhWorld != null && levelWrapper != null ? dhWorld.getClientLevel(levelWrapper) : null;
        RenderBufferHandler renderBufferHandler = dhLevel != null ? dhLevel.getRenderBufferHandler() : null;

        String validation = ClientApi.INSTANCE.lastRenderParamValidationMessage;
        int bufferCount = renderBufferHandler != null && renderBufferHandler.getColumnRenderBuffers() != null
                ? renderBufferHandler.getColumnRenderBuffers().size()
                : -1;

        String stableSignature =
                "validation=" + validation
                        + ", rendererDisabled=" + ClientApi.INSTANCE.rendererDisabledBecauseOfExceptions
                        + ", quickEnable=" + Config.Client.quickEnableRendering.get()
                        + ", rendererMode=" + Config.Client.Advanced.Debugging.rendererMode.get()
                        + ", hasLevelWrapper=" + (levelWrapper != null)
                        + ", hasDhWorld=" + (dhWorld != null)
                        + ", hasDhLevel=" + (dhLevel != null)
                        + ", hasRenderBufferHandler=" + (renderBufferHandler != null)
                        + ", hasGenericRenderer=" + (dhLevel != null && dhLevel.getGenericRenderer() != null)
                        + ", dhLevelRendering=" + (dhLevel != null && dhLevel.isRendering())
                        + ", vanillaFogEnabled=" + ClientApi.RENDER_STATE.vanillaFogEnabled
                        + ", fbo=" + GL32.glGetInteger(GL32.GL_FRAMEBUFFER_BINDING)
                        + ", drawFbo=" + GL32.glGetInteger(GL32.GL_DRAW_FRAMEBUFFER_BINDING)
                        + ", readFbo=" + GL32.glGetInteger(GL32.GL_READ_FRAMEBUFFER_BINDING)
                        + ", program=" + GL32.glGetInteger(GL32.GL_CURRENT_PROGRAM);
        String message = stableSignature
                + ", bufferCount=" + bufferCount
                + ", entityFogColor=" + getEntityFogColorDiagnostics()
                + ", glFogColor=" + getGlFogColorDiagnostics();

        long now = System.currentTimeMillis();
        if (!stableSignature.equals(lastDiagnosticSignature) || now - lastDiagnosticLogTimeMs > 5000L) {
            lastDiagnosticSignature = stableSignature;
            lastDiagnosticLogTimeMs = now;
            LOGGER.info("Distant Horizons bridge diagnostics: {}", message);
        }
    }

    private static String getEntityFogColorDiagnostics() {
        AccessorEntityRenderer entityRenderer = (AccessorEntityRenderer) Minecraft.getMinecraft().entityRenderer;
        return formatColor(
                entityRenderer.celeritas$getFogColorRed(),
                entityRenderer.celeritas$getFogColorGreen(),
                entityRenderer.celeritas$getFogColorBlue(),
                1.0F);
    }

    private static String getGlFogColorDiagnostics() {
        float[] fogColor = new float[4];
        GL15.glGetFloatv(GL15.GL_FOG_COLOR, fogColor);
        return formatColor(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
    }

    private static String formatColor(float red, float green, float blue, float alpha) {
        return String.format("%.3f/%.3f/%.3f/%.3f", red, green, blue, alpha);
    }
}
