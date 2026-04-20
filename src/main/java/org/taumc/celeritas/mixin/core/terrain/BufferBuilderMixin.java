package org.taumc.celeritas.mixin.core.terrain;

import com.dhj.actinium.celeritas.buffer.ActiniumBufferBuilderExtension;
import com.dhj.actinium.celeritas.buffer.ActiniumVanillaQuadContext;
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
public class BufferBuilderMixin implements ActiniumBufferBuilderExtension {
    @Shadow
    private int vertexCount;

    @Shadow
    private int drawMode;

    @Unique
    private final List<ActiniumVanillaQuadContext> actinium$quadContexts = new ArrayList<>();

    @Unique
    private @Nullable ActiniumVanillaQuadContext actinium$activeQuadContext;

    @Inject(method = "begin", at = @At("HEAD"))
    private void actinium$begin(int glMode, VertexFormat format, CallbackInfo ci) {
        this.actinium$quadContexts.clear();
        this.actinium$activeQuadContext = null;
    }

    @Inject(method = "addVertexData", at = @At("TAIL"))
    private void actinium$addVertexData(int[] vertexData, CallbackInfo ci) {
        if (this.actinium$activeQuadContext != null) {
            this.actinium$quadContexts.add(this.actinium$activeQuadContext);
        }
    }

    @Inject(method = "endVertex", at = @At("TAIL"))
    private void actinium$endVertex(CallbackInfo ci) {
        if (this.actinium$activeQuadContext != null && this.drawMode == GL11.GL_QUADS && (this.vertexCount & 3) == 0) {
            this.actinium$quadContexts.add(this.actinium$activeQuadContext);
        }
    }

    @Override
    public void actinium$setActiveQuadContext(@Nullable ActiniumVanillaQuadContext context) {
        this.actinium$activeQuadContext = context;
    }

    @Override
    public List<ActiniumVanillaQuadContext> actinium$consumeQuadContexts() {
        List<ActiniumVanillaQuadContext> copy = new ArrayList<>(this.actinium$quadContexts);
        this.actinium$quadContexts.clear();
        this.actinium$activeQuadContext = null;
        return copy;
    }
}
