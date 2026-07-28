package com.dhj.actinium.compat.ichunutil;

import com.gtnewhorizons.angelica.rendering.RenderingState;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

/**
 * Isolates the camera-state mirrors modified by iChun's recursive world rendering.
 */
public final class PortalRenderState {
    private PortalRenderState() {
    }

    /**
     * Runs one recursive portal render and restores the caller's camera state even when rendering fails.
     */
    public static void preserve(
        FloatBuffer activeProjection,
        FloatBuffer activeModelView,
        FloatBuffer activeObjectCoords,
        IntBuffer activeViewport,
        Runnable portalRender
    ) {
        Objects.requireNonNull(portalRender, "portalRender");
        Snapshot snapshot = Snapshot.capture(
            activeProjection,
            activeModelView,
            activeObjectCoords,
            activeViewport
        );

        try {
            portalRender.run();
        } finally {
            snapshot.restore();
        }
    }

    private record Snapshot(
        Matrix4f renderingProjection,
        Matrix4f renderingModelView,
        FloatBufferSnapshot activeProjection,
        FloatBufferSnapshot activeModelView,
        FloatBufferSnapshot activeObjectCoords,
        IntBufferSnapshot activeViewport
    ) {
        private static Snapshot capture(
            FloatBuffer activeProjection,
            FloatBuffer activeModelView,
            FloatBuffer activeObjectCoords,
            IntBuffer activeViewport
        ) {
            RenderingState renderingState = RenderingState.INSTANCE;
            return new Snapshot(
                new Matrix4f(renderingState.getProjectionMatrix()),
                new Matrix4f(renderingState.getModelViewMatrix()),
                FloatBufferSnapshot.capture(activeProjection, "activeProjection"),
                FloatBufferSnapshot.capture(activeModelView, "activeModelView"),
                FloatBufferSnapshot.capture(activeObjectCoords, "activeObjectCoords"),
                IntBufferSnapshot.capture(activeViewport, "activeViewport")
            );
        }

        private void restore() {
            this.activeProjection.restore();
            this.activeModelView.restore();
            this.activeObjectCoords.restore();
            this.activeViewport.restore();

            RenderingState renderingState = RenderingState.INSTANCE;
            renderingState.setProjectionMatrix(this.renderingProjection);
            renderingState.setModelViewMatrix(this.renderingModelView);
        }
    }

    private record FloatBufferSnapshot(FloatBuffer target, float[] values, int position, int limit) {
        private static FloatBufferSnapshot capture(FloatBuffer target, String name) {
            Objects.requireNonNull(target, name);
            FloatBuffer view = target.duplicate();
            view.clear();
            float[] values = new float[view.remaining()];
            view.get(values);
            return new FloatBufferSnapshot(target, values, target.position(), target.limit());
        }

        private void restore() {
            if (this.target.capacity() != this.values.length) {
                throw new IllegalStateException("Active render float buffer capacity changed during portal rendering");
            }

            FloatBuffer view = this.target.duplicate();
            view.clear();
            view.put(this.values);

            this.target.clear();
            this.target.limit(this.limit);
            this.target.position(this.position);
        }
    }

    private record IntBufferSnapshot(IntBuffer target, int[] values, int position, int limit) {
        private static IntBufferSnapshot capture(IntBuffer target, String name) {
            Objects.requireNonNull(target, name);
            IntBuffer view = target.duplicate();
            view.clear();
            int[] values = new int[view.remaining()];
            view.get(values);
            return new IntBufferSnapshot(target, values, target.position(), target.limit());
        }

        private void restore() {
            if (this.target.capacity() != this.values.length) {
                throw new IllegalStateException("Active render viewport buffer capacity changed during portal rendering");
            }

            IntBuffer view = this.target.duplicate();
            view.clear();
            view.put(this.values);

            this.target.clear();
            this.target.limit(this.limit);
            this.target.position(this.position);
        }
    }
}
