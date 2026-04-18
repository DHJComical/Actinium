package com.dhj.actinium.shadows;

import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;

public final class ActiniumShadowMatrixAccess {
    private static final String[] SHADOW_RENDERER_CLASSES = {
            "net.irisshaders.iris.shadows.ShadowRenderer",
            "net.coderbot.iris.pipeline.ShadowRenderer",
            "net.coderbot.iris.shadows.ShadowRenderer"
    };

    private static final @Nullable Field MODELVIEW_FIELD = findField("MODELVIEW");
    private static final @Nullable Field PROJECTION_FIELD = findField("PROJECTION");

    private ActiniumShadowMatrixAccess() {
    }

    public static boolean fillShadowMatrices(Matrix4f modelView, Matrix4f projection) {
        if (ActiniumInternalShadowRenderingState.fillShadowMatrices(modelView, projection)) {
            return true;
        }

        boolean hasModelView = fill(modelView, MODELVIEW_FIELD);
        boolean hasProjection = fill(projection, PROJECTION_FIELD);
        return hasModelView && hasProjection;
    }

    private static boolean fill(Matrix4f destination, @Nullable Field field) {
        if (field == null) {
            return false;
        }

        Object value;
        try {
            value = field.get(null);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }

        if (value == null) {
            return false;
        }

        if (value instanceof org.joml.Matrix4fc matrix) {
            destination.set(matrix);
            return true;
        }

        if (value instanceof FloatBuffer buffer) {
            FloatBuffer copy = buffer.duplicate();
            copy.clear();
            destination.set(copy);
            return true;
        }

        if (value instanceof float[] values && values.length >= 16) {
            destination.set(values);
            return true;
        }

        if (invokeGetFloatBuffer(value, destination)) {
            return true;
        }

        return invokeGetFloatArray(value, destination);
    }

    private static boolean invokeGetFloatBuffer(Object value, Matrix4f destination) {
        try {
            Method method = value.getClass().getMethod("get", FloatBuffer.class);
            FloatBuffer buffer = FloatBuffer.wrap(new float[16]);
            method.invoke(value, buffer);
            buffer.rewind();
            destination.set(buffer);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean invokeGetFloatArray(Object value, Matrix4f destination) {
        try {
            Method method = value.getClass().getMethod("get", float[].class);
            float[] values = new float[16];
            method.invoke(value, (Object) values);
            destination.set(values);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static @Nullable Field findField(String name) {
        for (String className : SHADOW_RENDERER_CLASSES) {
            try {
                Class<?> type = Class.forName(className);
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        return null;
    }
}
