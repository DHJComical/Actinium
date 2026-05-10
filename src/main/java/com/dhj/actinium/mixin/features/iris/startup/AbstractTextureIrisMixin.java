package com.dhj.actinium.mixin.features.iris.startup;

import com.gtnewhorizons.angelica.client.rendering.TextureTracker;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractTexture.class)
public class AbstractTextureIrisMixin {
    @Shadow
    protected int glTextureId;

    @Inject(
        method = "getGlTextureId()I",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/client/renderer/texture/AbstractTexture;glTextureId:I",
            shift = At.Shift.AFTER
        )
    )
    private void actinium$trackGeneratedTexture(CallbackInfoReturnable<Integer> cir) {
        TextureTracker.INSTANCE.trackTexture(this.glTextureId, (AbstractTexture) (Object) this);
    }
}
