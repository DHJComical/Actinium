package com.dhj.actinium.vertices;

import com.dhj.actinium.vertices.views.ActiniumQuadView;
import org.joml.Math;
import org.joml.Vector3f;

public final class ActiniumNormalHelper {
    private ActiniumNormalHelper() {
    }

    public static void computeFaceNormal(Vector3f saveTo, ActiniumQuadView quad) {
        final float x0 = quad.x(0);
        final float y0 = quad.y(0);
        final float z0 = quad.z(0);
        final float x1 = quad.x(1);
        final float y1 = quad.y(1);
        final float z1 = quad.z(1);
        final float x2 = quad.x(2);
        final float y2 = quad.y(2);
        final float z2 = quad.z(2);
        final float x3 = quad.x(3);
        final float y3 = quad.y(3);
        final float z3 = quad.z(3);

        final float dx0 = x2 - x0;
        final float dy0 = y2 - y0;
        final float dz0 = z2 - z0;
        final float dx1 = x3 - x1;
        final float dy1 = y3 - y1;
        final float dz1 = z3 - z1;

        float normX = dy0 * dz1 - dz0 * dy1;
        float normY = dz0 * dx1 - dx0 * dz1;
        float normZ = dx0 * dy1 - dy0 * dx1;

        final float length = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);
        if (length != 0.0f) {
            normX /= length;
            normY /= length;
            normZ /= length;
        }

        saveTo.set(normX, normY, normZ);
    }

    public static int computeTangent(float normalX, float normalY, float normalZ, ActiniumQuadView quad) {
        final float x0 = quad.x(0);
        final float y0 = quad.y(0);
        final float z0 = quad.z(0);
        final float x1 = quad.x(1);
        final float y1 = quad.y(1);
        final float z1 = quad.z(1);
        final float x2 = quad.x(2);
        final float y2 = quad.y(2);
        final float z2 = quad.z(2);

        final float edge1x = x1 - x0;
        final float edge1y = y1 - y0;
        final float edge1z = z1 - z0;
        final float edge2x = x2 - x0;
        final float edge2y = y2 - y0;
        final float edge2z = z2 - z0;

        final float u0 = quad.u(0);
        final float v0 = quad.v(0);
        final float u1 = quad.u(1);
        final float v1 = quad.v(1);
        final float u2 = quad.u(2);
        final float v2 = quad.v(2);

        final float deltaU1 = u1 - u0;
        final float deltaV1 = v1 - v0;
        final float deltaU2 = u2 - u0;
        final float deltaV2 = v2 - v0;

        final float denominator = deltaU1 * deltaV2 - deltaU2 * deltaV1;
        final float factor = denominator == 0.0f ? 1.0f : 1.0f / denominator;

        float tangentX = factor * (deltaV2 * edge1x - deltaV1 * edge2x);
        float tangentY = factor * (deltaV2 * edge1y - deltaV1 * edge2y);
        float tangentZ = factor * (deltaV2 * edge1z - deltaV1 * edge2z);
        final float tangentScale = rsqrt(tangentX * tangentX + tangentY * tangentY + tangentZ * tangentZ);
        tangentX *= tangentScale;
        tangentY *= tangentScale;
        tangentZ *= tangentScale;

        float bitangentX = factor * (-deltaU2 * edge1x + deltaU1 * edge2x);
        float bitangentY = factor * (-deltaU2 * edge1y + deltaU1 * edge2y);
        float bitangentZ = factor * (-deltaU2 * edge1z + deltaU1 * edge2z);
        final float bitangentScale = rsqrt(bitangentX * bitangentX + bitangentY * bitangentY + bitangentZ * bitangentZ);
        bitangentX *= bitangentScale;
        bitangentY *= bitangentScale;
        bitangentZ *= bitangentScale;

        final float predictedBitangentX = tangentY * normalZ - tangentZ * normalY;
        final float predictedBitangentY = tangentZ * normalX - tangentX * normalZ;
        final float predictedBitangentZ = tangentX * normalY - tangentY * normalX;
        final float dot = bitangentX * predictedBitangentX + bitangentY * predictedBitangentY + bitangentZ * predictedBitangentZ;
        final float tangentW = dot < 0.0f ? -1.0f : 1.0f;

        return ActiniumPackedNormal.pack(tangentX, tangentY, tangentZ, tangentW);
    }

    private static float rsqrt(float value) {
        if (value == 0.0f) {
            return 1.0f;
        }

        return (float) (1.0 / Math.sqrt(value));
    }
}
