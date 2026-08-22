package com.dhj.actinium.mixin.mod.oldresearch;

import com.dhj.actinium.compat.oldresearch.OldResearchTessellatorCompat;
import com.gtnewhorizons.angelica.glsm.ITessellatorData;
import com.wonginnovations.oldresearch.tc4legacy.client.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces Old Research's client-side vertex-array draw with Actinium's streaming VBO draw.
 *
 * <p>The target is conditionally loaded because the Old Research class is absent in installations
 * that do not include TC4 Research Port: Reborn.</p>
 */
@Mixin(value = Tessellator.class, remap = false)
public abstract class MixinOldResearchTessellator implements ITessellatorData {
    @Shadow
    private boolean isDrawing;
    @Shadow
    private int rawBufferSize;
    @Shadow
    private int[] rawBuffer;
    @Shadow
    private int vertexCount;
    @Shadow
    private boolean hasColor;
    @Shadow
    private boolean hasTexture;
    @Shadow
    private boolean hasBrightness;
    @Shadow
    private boolean hasNormals;
    @Shadow
    private int rawBufferIndex;
    @Shadow
    private int drawMode;
    @Shadow
    protected abstract void reset();

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, remap = false)
    private void actinium$drawWithStreaming(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(OldResearchTessellatorCompat.draw(this));
    }

    @Override
    public boolean isDrawing() {
        return this.isDrawing;
    }

    @Override
    public void setDrawing(boolean drawing) {
        this.isDrawing = drawing;
    }

    @Override
    public int getVertexCount() {
        return this.vertexCount;
    }

    @Override
    public int[] getRawBuffer() {
        return this.rawBuffer;
    }

    @Override
    public int getRawBufferIndex() {
        return this.rawBufferIndex;
    }

    @Override
    public int getRawBufferSize() {
        return this.rawBufferSize;
    }

    @Override
    public void setRawBufferSize(int size) {
        this.rawBufferSize = size;
    }

    @Override
    public void setRawBuffer(int[] buffer) {
        this.rawBuffer = buffer;
    }

    @Override
    public int getDrawMode() {
        return this.drawMode;
    }

    @Override
    public boolean hasTexture() {
        return this.hasTexture;
    }

    @Override
    public boolean hasColor() {
        return this.hasColor;
    }

    @Override
    public boolean hasNormals() {
        return this.hasNormals;
    }

    @Override
    public boolean hasBrightness() {
        return this.hasBrightness;
    }

    @Override
    @Unique
    public void angelica$reset() {
        this.reset();
    }
}
