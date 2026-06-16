package com.dhj.actinium.mixin.features.iris;

import com.dhj.actinium.config.ActiniumRuntimeOptions;
import com.gtnewhorizons.angelica.mixins.interfaces.IModelRenderer;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ModelRenderer.class)
public abstract class ModelRendererIrisMixin implements IModelRenderer {
    @Shadow public boolean isHidden;
    @Shadow public boolean showModel;
    @Shadow public float offsetX;
    @Shadow public float offsetY;
    @Shadow public float offsetZ;
    @Shadow public float rotationPointX;
    @Shadow public float rotationPointY;
    @Shadow public float rotationPointZ;
    @Shadow public float rotateAngleX;
    @Shadow public float rotateAngleY;
    @Shadow public float rotateAngleZ;
    @Shadow public List<ModelBox> cubeList;
    @Shadow public List<ModelRenderer> childModels;
    @Shadow private boolean compiled;
    @Shadow private int displayList;

    @Inject(method = "render(F)V", at = @At("HEAD"), cancellable = true)
    private void actinium$renderWithoutDisplayList(float scale, CallbackInfo ci) {
        ci.cancel();
        if (this.isHidden || !this.showModel) {
            return;
        }

        GlStateManager.translate(this.offsetX, this.offsetY, this.offsetZ);

        if (this.rotateAngleX != 0.0F || this.rotateAngleY != 0.0F || this.rotateAngleZ != 0.0F) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
            this.actinium$applyRotation();
            this.actinium$drawModel(scale);
            this.actinium$renderChildren(scale);
            GlStateManager.popMatrix();
        } else if (this.rotationPointX == 0.0F && this.rotationPointY == 0.0F && this.rotationPointZ == 0.0F) {
            this.actinium$drawModel(scale);
            this.actinium$renderChildren(scale);
        } else {
            GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
            this.actinium$drawModel(scale);
            this.actinium$renderChildren(scale);
            GlStateManager.translate(-this.rotationPointX * scale, -this.rotationPointY * scale, -this.rotationPointZ * scale);
        }

        GlStateManager.translate(-this.offsetX, -this.offsetY, -this.offsetZ);
    }

    @Inject(method = "renderWithRotation(F)V", at = @At("HEAD"), cancellable = true)
    private void actinium$renderWithRotationWithoutDisplayList(float scale, CallbackInfo ci) {
        ci.cancel();
        if (this.isHidden || !this.showModel) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);

        if (this.rotateAngleY != 0.0F) {
            GlStateManager.rotate(this.rotateAngleY * (180.0F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
        }

        if (this.rotateAngleX != 0.0F) {
            GlStateManager.rotate(this.rotateAngleX * (180.0F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
        }

        if (this.rotateAngleZ != 0.0F) {
            GlStateManager.rotate(this.rotateAngleZ * (180.0F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
        }

        this.actinium$drawModel(scale);
        GlStateManager.popMatrix();
    }

    @Override
    public void angelica$resetDisplayList() {
        if (this.displayList > 0) {
            com.gtnewhorizons.angelica.glsm.GLStateManager.glDeleteLists(this.displayList, 1);
        }
        this.compiled = false;
        this.displayList = 0;
    }

    private void actinium$applyRotation() {
        if (this.rotateAngleZ != 0.0F) {
            GlStateManager.rotate(this.rotateAngleZ * (180.0F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
        }

        if (this.rotateAngleY != 0.0F) {
            GlStateManager.rotate(this.rotateAngleY * (180.0F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
        }

        if (this.rotateAngleX != 0.0F) {
            GlStateManager.rotate(this.rotateAngleX * (180.0F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
        }
    }

    private void actinium$drawModel(float scale) {
        if (this.cubeList.isEmpty()) {
            return;
        }

        boolean batchModelQuads = ActiniumRuntimeOptions.useModelRendererBatching();
        boolean useDisplayLists = batchModelQuads && ActiniumRuntimeOptions.useModelRendererDisplayLists();

        if (useDisplayLists) {
            if (!this.compiled) {
                this.actinium$compileDisplayList(scale);
            }
            com.gtnewhorizons.angelica.glsm.GLStateManager.glCallList(this.displayList);
            return;
        }

        if (this.compiled && this.displayList > 0) {
            this.angelica$resetDisplayList();
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        if (batchModelQuads) {
            buffer.begin(7, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
            for (ModelBox cube : this.cubeList) {
                for (TexturedQuad quad : ((ModelBoxAccessor) cube).actinium$getQuadList()) {
                    actinium$appendQuad(buffer, quad, scale);
                }
            }
            tessellator.draw();
            return;
        }

        for (ModelBox cube : this.cubeList) {
            cube.render(buffer, scale);
        }
    }

    private void actinium$compileDisplayList(float scale) {
        this.displayList = com.gtnewhorizons.angelica.glsm.GLStateManager.glGenLists(1);
        if (this.displayList == 0) {
            throw new IllegalStateException("glGenLists returned 0 while compiling a ModelRenderer display list");
        }

        com.gtnewhorizons.angelica.glsm.GLStateManager.glNewList(this.displayList, GL11.GL_COMPILE);

        try {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
            for (ModelBox cube : this.cubeList) {
                for (TexturedQuad quad : ((ModelBoxAccessor) cube).actinium$getQuadList()) {
                    actinium$appendQuad(buffer, quad, scale);
                }
            }
            tessellator.draw();
        } finally {
            com.gtnewhorizons.angelica.glsm.GLStateManager.glEndList();
        }

        this.compiled = true;
    }

    private static void actinium$appendQuad(BufferBuilder buffer, TexturedQuad quad, float scale) {
        Vec3d vec3d = quad.vertexPositions[1].vector3D.subtractReverse(quad.vertexPositions[0].vector3D);
        Vec3d vec3d1 = quad.vertexPositions[1].vector3D.subtractReverse(quad.vertexPositions[2].vector3D);
        Vec3d normal = vec3d1.crossProduct(vec3d).normalize();
        float normalX = (float) normal.x;
        float normalY = (float) normal.y;
        float normalZ = (float) normal.z;

        if (((TexturedQuadAccessor) quad).actinium$isInvertNormal()) {
            normalX = -normalX;
            normalY = -normalY;
            normalZ = -normalZ;
        }

        for (int i = 0; i < quad.nVertices; i++) {
            PositionTextureVertex vertex = quad.vertexPositions[i];
            buffer.pos(
                    vertex.vector3D.x * scale,
                    vertex.vector3D.y * scale,
                    vertex.vector3D.z * scale
                )
                .tex(vertex.texturePositionX, vertex.texturePositionY)
                .normal(normalX, normalY, normalZ)
                .endVertex();
        }
    }

    private void actinium$renderChildren(float scale) {
        if (this.childModels == null) {
            return;
        }

        for (ModelRenderer child : this.childModels) {
            child.render(scale);
        }
    }
}
