package com.dhj.actinium.celeritas.shader_overrides;

import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat3v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix3f;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix4f;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderOptions;
import org.embeddedt.embeddium.impl.render.chunk.shader.DefaultChunkShaderInterface;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jetbrains.annotations.Nullable;

final class ActiniumChunkShaderInterface extends DefaultChunkShaderInterface {
    private final @Nullable GlUniformMatrix4f irisModelViewMatrix;
    private final @Nullable GlUniformMatrix4f irisModelViewMatrixInverse;
    private final @Nullable GlUniformMatrix4f irisModelViewMatrixInv;
    private final @Nullable GlUniformMatrix4f irisProjectionMatrix;
    private final @Nullable GlUniformMatrix4f irisProjectionMatrixInverse;
    private final @Nullable GlUniformMatrix4f irisProjectionMatrixInv;
    private final @Nullable GlUniformMatrix3f irisNormalMatrix;
    private final @Nullable GlUniformMatrix3f irisNormalMat;
    private final @Nullable GlUniformFloat3v irisRegionOffset;

    private final Matrix4f scratchModelViewInverse = new Matrix4f();
    private final Matrix4f scratchProjectionInverse = new Matrix4f();
    private final Matrix3f scratchNormalMatrix = new Matrix3f();

    public ActiniumChunkShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
        super(context, options);
        this.irisModelViewMatrix = context.bindUniformIfPresent("iris_ModelViewMatrix", GlUniformMatrix4f::new);
        this.irisModelViewMatrixInverse = context.bindUniformIfPresent("iris_ModelViewMatrixInverse", GlUniformMatrix4f::new);
        this.irisModelViewMatrixInv = context.bindUniformIfPresent("iris_ModelViewMatrixInv", GlUniformMatrix4f::new);
        this.irisProjectionMatrix = context.bindUniformIfPresent("iris_ProjectionMatrix", GlUniformMatrix4f::new);
        this.irisProjectionMatrixInverse = context.bindUniformIfPresent("iris_ProjectionMatrixInverse", GlUniformMatrix4f::new);
        this.irisProjectionMatrixInv = context.bindUniformIfPresent("iris_ProjectionMatrixInv", GlUniformMatrix4f::new);
        this.irisNormalMatrix = context.bindUniformIfPresent("iris_NormalMatrix", GlUniformMatrix3f::new);
        this.irisNormalMat = context.bindUniformIfPresent("iris_NormalMat", GlUniformMatrix3f::new);
        this.irisRegionOffset = context.bindUniformIfPresent("iris_RegionOffset", GlUniformFloat3v::new);
    }

    @Override
    public void setProjectionMatrix(Matrix4fc matrix) {
        super.setProjectionMatrix(matrix);

        if (this.irisProjectionMatrix != null) {
            this.irisProjectionMatrix.set(matrix);
        }

        if (this.irisProjectionMatrixInverse != null || this.irisProjectionMatrixInv != null) {
            this.scratchProjectionInverse.set(matrix);
            this.scratchProjectionInverse.invert();

            if (this.irisProjectionMatrixInverse != null) {
                this.irisProjectionMatrixInverse.set(this.scratchProjectionInverse);
            }

            if (this.irisProjectionMatrixInv != null) {
                this.irisProjectionMatrixInv.set(this.scratchProjectionInverse);
            }
        }
    }

    @Override
    public void setModelViewMatrix(Matrix4fc matrix) {
        super.setModelViewMatrix(matrix);

        if (this.irisModelViewMatrix != null) {
            this.irisModelViewMatrix.set(matrix);
        }

        if (this.irisModelViewMatrixInverse != null || this.irisModelViewMatrixInv != null || this.irisNormalMatrix != null || this.irisNormalMat != null) {
            this.scratchModelViewInverse.set(matrix);
            this.scratchModelViewInverse.invert();

            if (this.irisModelViewMatrixInverse != null) {
                this.irisModelViewMatrixInverse.set(this.scratchModelViewInverse);
            }

            if (this.irisModelViewMatrixInv != null) {
                this.irisModelViewMatrixInv.set(this.scratchModelViewInverse);
            }

            if (this.irisNormalMatrix != null || this.irisNormalMat != null) {
                this.scratchNormalMatrix.set(this.scratchModelViewInverse);
                this.scratchNormalMatrix.transpose();

                if (this.irisNormalMatrix != null) {
                    this.irisNormalMatrix.set(this.scratchNormalMatrix);
                }

                if (this.irisNormalMat != null) {
                    this.irisNormalMat.set(this.scratchNormalMatrix);
                }
            }
        }
    }

    @Override
    public void setRegionOffset(float x, float y, float z) {
        super.setRegionOffset(x, y, z);

        if (this.irisRegionOffset != null) {
            this.irisRegionOffset.set(x, y, z);
        }
    }
}
