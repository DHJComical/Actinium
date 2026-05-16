package com.gtnewhorizon.gtnhlib.client.model;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.gtnewhorizon.gtnhlib.client.renderer.LegacyTessellator;

public final class NormalHelper {
    private NormalHelper() {}

    public static Matrix3f getNormalMatrix(Matrix4f transformationMatrix, Matrix3f dest) {
        return dest.set(transformationMatrix).invert().transpose();
    }

    public static Matrix3f getNormalMatrix(Matrix4f transformationMatrix) {
        return new Matrix3f(transformationMatrix).invert().transpose();
    }

    public static Vector3f setNormalTransformed(Vector3fc normal, Vector3f dest, Matrix3f normalMatrix) {
        normalMatrix.transform(normal, dest).normalize();
        return dest;
    }

    public static Vector3f setNormalTransformed(Vector3f normal, Matrix3f normalMatrix) {
        return setNormalTransformed(normal, normal, normalMatrix);
    }

    public static void setNormalTransformed(LegacyTessellator tessellator, Vector3f normal, Matrix3f normalMatrix) {
        setNormalTransformed(normal, normalMatrix);
        tessellator.setNormal(normal.x, normal.y, normal.z);
    }

    public static void setNormalTransformed(LegacyTessellator tessellator, Vector3fc normal, Vector3f dest,
            Matrix3f normalMatrix) {
        setNormalTransformed(normal, dest, normalMatrix);
        tessellator.setNormal(dest.x, dest.y, dest.z);
    }
}
