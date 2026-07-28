package com.dhj.actinium.compat.ichunutil;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.rendering.RenderingState;
import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.joml.Matrix4f;

/**
 * Captures the matrices used by iChun's recursive terrain pass.
 */
public final class PortalChunkRenderMatrices {
    private PortalChunkRenderMatrices() {
    }

    /**
     * Uses iChun's live terrain projection while retaining the model-view captured for the recursive camera.
     * Sky and cloud rendering can temporarily alter the live model-view before terrain layers are submitted.
     */
    public static ChunkRenderMatrices capture() {
        return new ChunkRenderMatrices(
            new Matrix4f(GLStateManager.getProjectionMatrix()),
            new Matrix4f(RenderingState.INSTANCE.getModelViewMatrix())
        );
    }
}
