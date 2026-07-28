package com.dhj.actinium.compat.ichunutil;

import com.gtnewhorizons.angelica.rendering.RenderingState;
import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortalRenderStateTest {
    private Matrix4f previousProjection;
    private Matrix4f previousModelView;

    @BeforeEach
    void captureRenderingState() {
        this.previousProjection = new Matrix4f(RenderingState.INSTANCE.getProjectionMatrix());
        this.previousModelView = new Matrix4f(RenderingState.INSTANCE.getModelViewMatrix());
    }

    @AfterEach
    void restoreRenderingState() {
        RenderingState.INSTANCE.setProjectionMatrix(this.previousProjection);
        RenderingState.INSTANCE.setModelViewMatrix(this.previousModelView);
    }

    @Test
    void restoresEachNestedPortalCallToItsEntryState() {
        FloatBuffer projection = createBuffer(1.0F, 2, 15);
        FloatBuffer modelView = createBuffer(21.0F, 3, 14);
        FloatBuffer objectCoords = createBuffer(121.0F, 1, 12);
        IntBuffer viewport = createIntBuffer(201, 1, 3);
        BufferExpectation outerProjection = BufferExpectation.capture(projection);
        BufferExpectation outerModelView = BufferExpectation.capture(modelView);
        BufferExpectation outerObjectCoords = BufferExpectation.capture(objectCoords);
        IntBufferExpectation outerViewport = IntBufferExpectation.capture(viewport);
        Matrix4f outerRenderingProjection = matrix(2.0F, 3.0F, 4.0F);
        Matrix4f outerRenderingModelView = matrix(5.0F, 6.0F, 7.0F);
        setRenderingState(outerRenderingProjection, outerRenderingModelView);

        PortalRenderState.preserve(projection, modelView, objectCoords, viewport, () -> {
            overwrite(projection, 41.0F, 4, 13);
            overwrite(modelView, 61.0F, 5, 12);
            overwrite(objectCoords, 141.0F, 2, 11);
            overwrite(viewport, 221, 0, 4);
            Matrix4f recursiveProjection = matrix(8.0F, 9.0F, 10.0F);
            Matrix4f recursiveModelView = matrix(11.0F, 12.0F, 13.0F);
            setRenderingState(recursiveProjection, recursiveModelView);
            BufferExpectation recursiveActiveProjection = BufferExpectation.capture(projection);
            BufferExpectation recursiveActiveModelView = BufferExpectation.capture(modelView);
            BufferExpectation recursiveActiveObjectCoords = BufferExpectation.capture(objectCoords);
            IntBufferExpectation recursiveActiveViewport = IntBufferExpectation.capture(viewport);

            PortalRenderState.preserve(projection, modelView, objectCoords, viewport, () -> {
                overwrite(projection, 81.0F, 0, 16);
                overwrite(modelView, 101.0F, 1, 11);
                overwrite(objectCoords, 161.0F, 3, 10);
                overwrite(viewport, 241, 2, 4);
                setRenderingState(matrix(14.0F, 15.0F, 16.0F), matrix(17.0F, 18.0F, 19.0F));
            });

            recursiveActiveProjection.assertMatches(projection);
            recursiveActiveModelView.assertMatches(modelView);
            recursiveActiveObjectCoords.assertMatches(objectCoords);
            recursiveActiveViewport.assertMatches(viewport);
            assertRenderingState(recursiveProjection, recursiveModelView);
        });

        outerProjection.assertMatches(projection);
        outerModelView.assertMatches(modelView);
        outerObjectCoords.assertMatches(objectCoords);
        outerViewport.assertMatches(viewport);
        assertRenderingState(outerRenderingProjection, outerRenderingModelView);
    }

    @Test
    void restoresStateWhenPortalRenderingThrows() {
        FloatBuffer projection = createBuffer(1.0F, 2, 15);
        FloatBuffer modelView = createBuffer(21.0F, 3, 14);
        FloatBuffer objectCoords = createBuffer(121.0F, 1, 12);
        IntBuffer viewport = createIntBuffer(201, 1, 3);
        BufferExpectation expectedProjection = BufferExpectation.capture(projection);
        BufferExpectation expectedModelView = BufferExpectation.capture(modelView);
        BufferExpectation expectedObjectCoords = BufferExpectation.capture(objectCoords);
        IntBufferExpectation expectedViewport = IntBufferExpectation.capture(viewport);
        Matrix4f expectedRenderingProjection = matrix(2.0F, 3.0F, 4.0F);
        Matrix4f expectedRenderingModelView = matrix(5.0F, 6.0F, 7.0F);
        setRenderingState(expectedRenderingProjection, expectedRenderingModelView);
        IllegalStateException failure = new IllegalStateException("portal render failed");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
            PortalRenderState.preserve(projection, modelView, objectCoords, viewport, () -> {
                overwrite(projection, 41.0F, 4, 13);
                overwrite(modelView, 61.0F, 5, 12);
                overwrite(objectCoords, 141.0F, 2, 11);
                overwrite(viewport, 221, 0, 4);
                setRenderingState(matrix(8.0F, 9.0F, 10.0F), matrix(11.0F, 12.0F, 13.0F));
                throw failure;
            })
        );

        assertSame(failure, thrown);
        expectedProjection.assertMatches(projection);
        expectedModelView.assertMatches(modelView);
        expectedObjectCoords.assertMatches(objectCoords);
        expectedViewport.assertMatches(viewport);
        assertRenderingState(expectedRenderingProjection, expectedRenderingModelView);
    }

    private static FloatBuffer createBuffer(float firstValue, int position, int limit) {
        FloatBuffer buffer = FloatBuffer.allocate(16);
        overwrite(buffer, firstValue, position, limit);
        return buffer;
    }

    private static void overwrite(FloatBuffer buffer, float firstValue, int position, int limit) {
        FloatBuffer view = buffer.duplicate();
        view.clear();
        for (int index = 0; index < view.capacity(); index++) {
            view.put(index, firstValue + index);
        }
        buffer.clear();
        buffer.limit(limit);
        buffer.position(position);
    }

    private static IntBuffer createIntBuffer(int firstValue, int position, int limit) {
        IntBuffer buffer = IntBuffer.allocate(4);
        overwrite(buffer, firstValue, position, limit);
        return buffer;
    }

    private static void overwrite(IntBuffer buffer, int firstValue, int position, int limit) {
        IntBuffer view = buffer.duplicate();
        view.clear();
        for (int index = 0; index < view.capacity(); index++) {
            view.put(index, firstValue + index);
        }
        buffer.clear();
        buffer.limit(limit);
        buffer.position(position);
    }

    private static Matrix4f matrix(float x, float y, float z) {
        return new Matrix4f().identity().translate(x, y, z);
    }

    private static void setRenderingState(Matrix4f projection, Matrix4f modelView) {
        RenderingState.INSTANCE.setProjectionMatrix(projection);
        RenderingState.INSTANCE.setModelViewMatrix(modelView);
    }

    private static void assertRenderingState(Matrix4f projection, Matrix4f modelView) {
        assertEquals(projection, RenderingState.INSTANCE.getProjectionMatrix());
        assertEquals(modelView, RenderingState.INSTANCE.getModelViewMatrix());
    }

    private record BufferExpectation(float[] values, int position, int limit) {
        private static BufferExpectation capture(FloatBuffer buffer) {
            FloatBuffer view = buffer.duplicate();
            view.clear();
            float[] values = new float[view.remaining()];
            view.get(values);
            return new BufferExpectation(values, buffer.position(), buffer.limit());
        }

        private void assertMatches(FloatBuffer buffer) {
            assertEquals(this.position, buffer.position());
            assertEquals(this.limit, buffer.limit());
            FloatBuffer view = buffer.duplicate();
            view.clear();
            for (int index = 0; index < this.values.length; index++) {
                assertEquals(this.values[index], view.get(index));
            }
        }
    }

    private record IntBufferExpectation(int[] values, int position, int limit) {
        private static IntBufferExpectation capture(IntBuffer buffer) {
            IntBuffer view = buffer.duplicate();
            view.clear();
            int[] values = new int[view.remaining()];
            view.get(values);
            return new IntBufferExpectation(values, buffer.position(), buffer.limit());
        }

        private void assertMatches(IntBuffer buffer) {
            assertEquals(this.position, buffer.position());
            assertEquals(this.limit, buffer.limit());
            IntBuffer view = buffer.duplicate();
            view.clear();
            for (int index = 0; index < this.values.length; index++) {
                assertEquals(this.values[index], view.get(index));
            }
        }
    }
}
