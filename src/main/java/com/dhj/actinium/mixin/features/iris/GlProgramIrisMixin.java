package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.gl.blending.DepthColorStorage;
import org.embeddedt.embeddium.impl.gl.GlObject;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(value = GlProgram.class, remap = false)
public abstract class GlProgramIrisMixin extends GlObject {
    @Inject(method = "<init>(ILjava/util/function/Function;)V", at = @At("TAIL"))
    private void actinium$registerOwnedProgram(int program, Function<ShaderBindingContext, ?> interfaceFactory, CallbackInfo ci) {
        DepthColorStorage.registerOwnedProgram(program);
    }

    @Inject(method = "destroyInternal()V", at = @At("HEAD"))
    private void actinium$unregisterOwnedProgram(CallbackInfo ci) {
        DepthColorStorage.unregisterOwnedProgram(this.handle());
    }
}
