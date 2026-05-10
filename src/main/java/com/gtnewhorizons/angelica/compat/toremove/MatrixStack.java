package com.gtnewhorizons.angelica.compat.toremove;

import com.google.common.collect.Queues;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Deque;

public class MatrixStack {
    private final Deque<Entry> matrixStack;

    public MatrixStack() {
        this.matrixStack = Queues.newArrayDeque();
        this.matrixStack.add(new Entry(new Matrix4f().identity(), new Matrix3f().identity()));
    }

    public MatrixStack(Matrix4f initial) {
        this.matrixStack = Queues.newArrayDeque();
        Matrix3f normal = new Matrix3f();
        this.matrixStack.add(new Entry(initial, initial.normal(normal)));
    }

    public Entry peek() {
        return this.matrixStack.getLast();
    }

    public void push() {
        Entry entry = this.matrixStack.getLast();
        this.matrixStack.addLast(new Entry(new Matrix4f(entry.model), new Matrix3f(entry.normal)));
    }

    public void pop() {
        this.matrixStack.removeLast();
    }

    public boolean isEmpty() {
        return this.matrixStack.size() == 1;
    }

    public void reset() {
        while (this.matrixStack.size() > 1) {
            this.matrixStack.removeLast();
        }
        loadIdentity();
    }

    public void translate(double x, double y, double z) {
        this.matrixStack.getLast().model.translate((float) x, (float) y, (float) z);
    }

    public void rotateX(float angle) {
        Entry entry = this.matrixStack.getLast();
        entry.model.rotateX(angle);
        entry.normal.rotateX(angle);
    }

    public void rotateY(float angle) {
        Entry entry = this.matrixStack.getLast();
        entry.model.rotateY(angle);
        entry.normal.rotateY(angle);
    }

    public void rotateZ(float angle) {
        Entry entry = this.matrixStack.getLast();
        entry.model.rotateZ(angle);
        entry.normal.rotateZ(angle);
    }

    public void scale(float x, float y, float z) {
        Entry entry = this.matrixStack.getLast();
        entry.model.scale(x, y, z);

        if (x == y && y == z) {
            if (x > 0.0F) {
                return;
            }
            entry.normal.scale(-1.0F);
        }

        float invX = 1.0F / x;
        float invY = 1.0F / y;
        float invZ = 1.0F / z;
        float scale = invSqrt(invX * invY * invZ);
        entry.normal.scale(scale * invX, scale * invY, scale * invZ);
    }

    public void multiply(Quaternionf quaternion) {
        Entry entry = this.matrixStack.getLast();
        entry.model.rotate(quaternion);
        entry.normal.rotate(quaternion);
    }

    public void multiply(Quaternionf quaternion, float originX, float originY, float originZ) {
        Entry entry = this.matrixStack.getLast();
        entry.model.rotateAround(quaternion, originX, originY, originZ);
        entry.normal.rotate(quaternion);
    }

    public void loadIdentity() {
        Entry entry = this.matrixStack.getLast();
        entry.model.identity();
        entry.normal.identity();
    }

    public void multiplyPositionMatrix(Matrix4f matrix) {
        this.matrixStack.getLast().model.mul(matrix);
    }

    private static float invSqrt(float x) {
        float half = 0.5F * x;
        int bits = Float.floatToIntBits(x);
        bits = 0x5f3759df - (bits >> 1);
        x = Float.intBitsToFloat(bits);
        x *= 1.5F - half * x * x;
        return x;
    }

    public static final class Entry {
        private final Matrix4f model;
        private final Matrix3f normal;

        private Entry(Matrix4f model, Matrix3f normal) {
            this.model = model;
            this.normal = normal;
        }

        public Matrix4f getModel() {
            return this.model;
        }

        public Matrix3f getNormal() {
            return this.normal;
        }
    }
}
