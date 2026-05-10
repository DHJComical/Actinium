package com.dhj.actinium.mixin.core.terrain;

import com.dhj.actinium.celeritas.buffer.BufferBuilderExtension;
import com.dhj.actinium.celeritas.buffer.ShaderMaterialOverrideState;
import com.dhj.actinium.celeritas.buffer.VanillaQuadContext;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(BufferBuilder.class)
public class BufferBuilderMixin implements BufferBuilderExtension {
    @Shadow
    private int vertexCount;

    @Shadow
    private int drawMode;

    @Unique
    private final List<VanillaQuadContext> actinium$quadContexts = new ArrayList<>();

    @Unique
    private @Nullable VanillaQuadContext actinium$activeQuadContext;

    @Inject(method = "begin", at = @At("HEAD"))
    private void actinium$begin(int glMode, VertexFormat format, CallbackInfo ci) {
        this.actinium$quadContexts.clear();
        this.actinium$activeQuadContext = null;
    }

    @Inject(method = "addVertexData", at = @At("TAIL"))
    private void actinium$addVertexData(int[] vertexData, CallbackInfo ci) {
        if (this.actinium$activeQuadContext != null) {
            this.actinium$quadContexts.add(this.actinium$snapshotQuadContext());
        }
    }

    @Inject(method = "endVertex", at = @At("TAIL"))
    private void actinium$endVertex(CallbackInfo ci) {
        if (this.actinium$activeQuadContext != null && this.drawMode == GL11.GL_QUADS && (this.vertexCount & 3) == 0) {
            this.actinium$quadContexts.add(this.actinium$snapshotQuadContext());
        }
    }

    @Unique
    private VanillaQuadContext actinium$snapshotQuadContext() {
        int shaderOverrideBlockId = ShaderMaterialOverrideState.getBlockId();
        return shaderOverrideBlockId >= 0
                ? this.actinium$activeQuadContext.withBlockStateId(shaderOverrideBlockId)
                : this.actinium$activeQuadContext;
    }

    @Override
    public void actinium$setActiveQuadContext(@Nullable VanillaQuadContext context) {
        this.actinium$activeQuadContext = context;
    }

    @Override
    public List<VanillaQuadContext> actinium$consumeQuadContexts() {
        List<VanillaQuadContext> copy = new ArrayList<>(this.actinium$quadContexts);
        this.actinium$quadContexts.clear();
        this.actinium$activeQuadContext = null;
        return copy;
    }
}
