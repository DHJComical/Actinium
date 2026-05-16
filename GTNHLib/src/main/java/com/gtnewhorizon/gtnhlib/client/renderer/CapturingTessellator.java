package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.COLOR_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.LIGHT_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.NORMAL_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.TEX_X_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.TEX_Y_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.VERTEX_SIZE;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.X_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.Y_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.Z_INDEX;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.util.math.MathHelper;

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.gtnhlib.client.model.NormalHelper;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.line.ModelLine;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.primitive.ModelPrimitiveView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.tri.ModelTriangle;
import com.gtnewhorizon.gtnhlib.client.renderer.stacks.Vector3dStack;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;
import com.gtnewhorizon.gtnhlib.util.ObjectPooler;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@SuppressWarnings("unused")
public class CapturingTessellator extends LegacyTessellator implements ITessellatorInstance {

    boolean active = false;
    private final Vector3dStack storedTranslation = new Vector3dStack();

    final ObjectPooler<ModelQuad> quadPool = new ObjectPooler<>(ModelQuad::new);
    final ObjectPooler<ModelTriangle> triPool = new ObjectPooler<>(ModelTriangle::new);
    final ObjectPooler<ModelLine> linePool = new ObjectPooler<>(ModelLine::new);

    final List<ModelQuadViewMutable> collectedQuads = new ObjectArrayList<>();
    final List<ModelPrimitiveView> collectedPrimitives = new ObjectArrayList<>();

    final List<ModelLine> lineListCache = new ArrayList<>();
    final List<ModelTriangle> triangleListCache = new ArrayList<>();
    final List<ModelQuadViewMutable> quadListCache = new ArrayList<>();

    int shaderBlockId = -1;
    final BlockPos offset = new BlockPos();
    final Flags flags = new Flags(true, true, true, true);

    public void storeTranslation() {
        storedTranslation.push();
        storedTranslation.set(xOffset, yOffset, zOffset);
    }

    public void restoreTranslation() {
        xOffset = storedTranslation.x;
        yOffset = storedTranslation.y;
        zOffset = storedTranslation.z;
        storedTranslation.pop();
    }

    public void setOffset(BlockPos pos) {
        this.offset.set(pos);
    }

    public void resetOffset() {
        this.offset.zero();
    }

    @Override
    public int draw() {
        final boolean compiling = gtnhlib$isCompiling();
        final int result = this.rawBufferIndex * Integer.BYTES;

        if (this.vertexCount > 0) {
            if (compiling) {
                if (this.drawMode == GL11.GL_QUADS) {
                    QuadExtractor.buildQuadsFromBuffer(
                            this.rawBuffer,
                            this.vertexCount,
                            this.drawMode,
                            this.hasTexture,
                            this.hasBrightness,
                            this.hasColor,
                            this.hasNormals,
                            -this.offset.x,
                            -this.offset.y,
                            -this.offset.z,
                            this.shaderBlockId,
                            this.quadPool,
                            this.collectedQuads,
                            this.flags);
                } else {
                    PrimitiveExtractor.buildPrimitivesFromBuffer(
                            this.rawBuffer,
                            this.vertexCount,
                            this.drawMode,
                            this.hasTexture,
                            this.hasBrightness,
                            this.hasColor,
                            this.hasNormals,
                            -this.offset.x,
                            -this.offset.y,
                            -this.offset.z,
                            this.shaderBlockId,
                            this.quadPool,
                            this.triPool,
                            this.linePool,
                            this.collectedPrimitives,
                            this.flags);
                }
            } else {
                if (this.drawMode == GL11.GL_QUADS || this.drawMode == GL11.GL_TRIANGLES) {
                    QuadExtractor.buildQuadsFromBuffer(
                            this.rawBuffer,
                            this.vertexCount,
                            this.drawMode,
                            this.hasTexture,
                            this.hasBrightness,
                            this.hasColor,
                            this.hasNormals,
                            -this.offset.x,
                            -this.offset.y,
                            -this.offset.z,
                            this.shaderBlockId,
                            this.quadPool,
                            this.collectedQuads,
                            this.flags);
                } else {
                    PrimitiveExtractor.buildPrimitivesFromBuffer(
                            this.rawBuffer,
                            this.vertexCount,
                            this.drawMode,
                            this.hasTexture,
                            this.hasBrightness,
                            this.hasColor,
                            this.hasNormals,
                            -this.offset.x,
                            -this.offset.y,
                            -this.offset.z,
                            this.shaderBlockId,
                            this.quadPool,
                            this.triPool,
                            this.linePool,
                            this.collectedPrimitives,
                            this.flags);
                }
            }
        }

        discard();
        return result;
    }

    @Override
    public boolean gtnhlib$isCompiling() {
        return false;
    }

    @Override
    public void gtnhlib$setCompiling(boolean compiling) {
    }

    public List<ModelQuadViewMutable> getQuads() {
        return collectedQuads;
    }

    public List<ModelPrimitiveView> getPrimitives() {
        return collectedPrimitives;
    }

    public void clearQuads() {
        for (int i = 0; i < collectedQuads.size(); i++) {
            final var quad = collectedQuads.get(i);
            if (quad instanceof ModelQuad modelQuad) {
                quadPool.releaseInstance(modelQuad);
            }
        }
        collectedQuads.clear();
    }

    public void clearPrimitives() {
        for (int i = 0; i < collectedPrimitives.size(); i++) {
            final var primitive = collectedPrimitives.get(i);
            if (primitive instanceof ModelQuad modelQuad) {
                quadPool.releaseInstance(modelQuad);
            } else if (primitive instanceof ModelTriangle triangle) {
                triPool.releaseInstance(triangle);
            } else if (primitive instanceof ModelLine line) {
                linePool.releaseInstance(line);
            }
        }
        collectedPrimitives.clear();
    }

    public static ByteBuffer quadsToBuffer(List<ModelQuadViewMutable> quads, VertexFormat format) {
        final ByteBuffer byteBuffer = BufferUtils.createByteBuffer(format.getVertexSize() * quads.size() * 4);
        format.writeQuads(quads, byteBuffer);
        byteBuffer.flip();
        return byteBuffer;
    }

    public static int createBrightness(int sky, int block) {
        return sky << 20 | block << 4;
    }

    public CapturingTessellator pos(double x, double y, double z) {
        ensureBuffer(VERTEX_SIZE);
        this.rawBuffer[this.rawBufferIndex + X_INDEX] = Float.floatToRawIntBits((float) (x + this.xOffset));
        this.rawBuffer[this.rawBufferIndex + Y_INDEX] = Float.floatToRawIntBits((float) (y + this.yOffset));
        this.rawBuffer[this.rawBufferIndex + Z_INDEX] = Float.floatToRawIntBits((float) (z + this.zOffset));
        return this;
    }

    public CapturingTessellator tex(double u, double v) {
        ensureBuffer(VERTEX_SIZE);
        this.rawBuffer[this.rawBufferIndex + TEX_X_INDEX] = Float.floatToRawIntBits((float) u);
        this.rawBuffer[this.rawBufferIndex + TEX_Y_INDEX] = Float.floatToRawIntBits((float) v);
        this.hasTexture = true;
        return this;
    }

    public CapturingTessellator color(float red, float green, float blue, float alpha) {
        return color((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
    }

    public CapturingTessellator color(int red, int green, int blue, int alpha) {
        if (this.isColorDisabled) {
            return this;
        }

        ensureBuffer(VERTEX_SIZE);
        red = MathHelper.clamp(red, 0, 255);
        green = MathHelper.clamp(green, 0, 255);
        blue = MathHelper.clamp(blue, 0, 255);
        alpha = MathHelper.clamp(alpha, 0, 255);

        final int color;
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            color = alpha << 24 | blue << 16 | green << 8 | red;
        } else {
            color = red << 24 | green << 16 | blue << 8 | alpha;
        }

        this.rawBuffer[this.rawBufferIndex + COLOR_INDEX] = color;
        this.hasColor = true;
        return this;
    }

    public CapturingTessellator normal(float x, float y, float z) {
        ensureBuffer(VERTEX_SIZE);
        final byte b0 = (byte) ((int) (x * 127.0F));
        final byte b1 = (byte) ((int) (y * 127.0F));
        final byte b2 = (byte) ((int) (z * 127.0F));
        this.rawBuffer[this.rawBufferIndex + NORMAL_INDEX] = b0 & 255 | (b1 & 255) << 8 | (b2 & 255) << 16;
        this.hasNormals = true;
        return this;
    }

    @Override
    public void setNormal(float x, float y, float z) {
        normal(x, y, z);
    }

    public CapturingTessellator setNormalTransformed(Vector3f normal, Vector3f dest, Matrix3f normalMatrix) {
        NormalHelper.setNormalTransformed(this, normal, dest, normalMatrix);
        return this;
    }

    public CapturingTessellator setNormalTransformed(Vector3f normal, Matrix3f normalMatrix) {
        NormalHelper.setNormalTransformed(this, normal, normalMatrix);
        return this;
    }

    public CapturingTessellator lightmap(int skyLight, int blockLight) {
        return brightness(createBrightness(skyLight, blockLight));
    }

    public CapturingTessellator brightness(int brightness) {
        ensureBuffer(VERTEX_SIZE);
        this.rawBuffer[this.rawBufferIndex + LIGHT_INDEX] = brightness;
        this.hasBrightness = true;
        return this;
    }

    public CapturingTessellator endVertex() {
        ensureBuffer(VERTEX_SIZE);
        this.rawBufferIndex += VERTEX_SIZE;
        this.vertexCount++;
        return this;
    }

    public void setShaderBlockId(int blockId) {
        if (isDrawing && this.vertexCount > 0) {
            draw();
            this.isDrawing = true;
            this.drawMode = this.flags.drawMode;
        }
        this.shaderBlockId = blockId;
    }

    @Override
    public void reset() {
        super.reset();
    }

    public static class Flags {

        public boolean hasTexture;
        public boolean hasBrightness;
        public boolean hasColor;
        public boolean hasNormals;
        @Deprecated
        public int drawMode = GL11.GL_QUADS;

        public Flags(boolean hasTexture, boolean hasBrightness, boolean hasColor, boolean hasNormals) {
            this.hasTexture = hasTexture;
            this.hasBrightness = hasBrightness;
            this.hasColor = hasColor;
            this.hasNormals = hasNormals;
        }

        public Flags(Flags other) {
            this.hasTexture = other.hasTexture;
            this.hasBrightness = other.hasBrightness;
            this.hasColor = other.hasColor;
            this.hasNormals = other.hasNormals;
            this.drawMode = other.drawMode;
        }

        public void copyFrom(boolean hasTexture, boolean hasBrightness, boolean hasColor, boolean hasNormals) {
            this.hasTexture = hasTexture;
            this.hasBrightness = hasBrightness;
            this.hasColor = hasColor;
            this.hasNormals = hasNormals;
        }

        public void copyFrom(boolean hasTexture, boolean hasBrightness, boolean hasColor, boolean hasNormals,
                int drawMode) {
            this.hasTexture = hasTexture;
            this.hasBrightness = hasBrightness;
            this.hasColor = hasColor;
            this.hasNormals = hasNormals;
            this.drawMode = drawMode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Flags flags)) return false;
            return hasTexture == flags.hasTexture && hasBrightness == flags.hasBrightness
                    && hasColor == flags.hasColor
                    && hasNormals == flags.hasNormals
                    && drawMode == flags.drawMode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(hasTexture, hasBrightness, hasColor, hasNormals, drawMode);
        }
    }
}
