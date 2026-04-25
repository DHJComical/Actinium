package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.shader.uniform.ActiniumCapturedRenderingState;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat4v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix4f;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

final class ActiniumShadowShaderInterface {
    private final @Nullable GlUniformInt texSampler;
    private final @Nullable GlUniformInt entityId;
    private final @Nullable GlUniformFloat4v entityColor;
    private final @Nullable GlUniformMatrix4f shadowModelView;
    private final @Nullable GlUniformMatrix4f shadowModelViewInverse;
    private final @Nullable GlUniformMatrix4f shadowProjection;
    private final @Nullable GlUniformMatrix4f shadowProjectionInverse;

    ActiniumShadowShaderInterface(ShaderBindingContext context) {
        this.texSampler = context.bindUniformIfPresent("tex", GlUniformInt::new);
        this.entityId = context.bindUniformIfPresent("entityId", GlUniformInt::new);
        this.entityColor = context.bindUniformIfPresent("entityColor", GlUniformFloat4v::new);
        this.shadowModelView = context.bindUniformIfPresent("shadowModelView", GlUniformMatrix4f::new);
        this.shadowModelViewInverse = context.bindUniformIfPresent("shadowModelViewInverse", GlUniformMatrix4f::new);
        this.shadowProjection = context.bindUniformIfPresent("shadowProjection", GlUniformMatrix4f::new);
        this.shadowProjectionInverse = context.bindUniformIfPresent("shadowProjectionInverse", GlUniformMatrix4f::new);
    }

    public void setupState(Matrix4fc modelView, Matrix4fc modelViewInverse, Matrix4fc projection, Matrix4fc projectionInverse) {
        if (this.texSampler != null) {
            this.texSampler.setInt(0);
        }

        if (this.entityId != null) {
            this.entityId.setInt(ActiniumCapturedRenderingState.getCurrentRenderedEntity());
        }

        if (this.entityColor != null) {
            Vector4f capturedEntityColor = ActiniumCapturedRenderingState.getCurrentEntityColor();
            this.entityColor.set(new float[]{capturedEntityColor.x, capturedEntityColor.y, capturedEntityColor.z, capturedEntityColor.w});
        }

        if (this.shadowModelView != null) {
            this.shadowModelView.set(modelView);
        }

        if (this.shadowModelViewInverse != null) {
            this.shadowModelViewInverse.set(modelViewInverse);
        }

        if (this.shadowProjection != null) {
            this.shadowProjection.set(projection);
        }

        if (this.shadowProjectionInverse != null) {
            this.shadowProjectionInverse.set(projectionInverse);
        }
    }
}
