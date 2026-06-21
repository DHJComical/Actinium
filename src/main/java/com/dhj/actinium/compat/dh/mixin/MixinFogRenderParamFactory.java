package com.dhj.actinium.compat.dh.mixin;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiFogColorMode;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.render.renderer.FogRenderParamFactory;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.taumc.celeritas.mixin.core.terrain.AccessorEntityRenderer;

import java.awt.Color;

@Mixin(value = FogRenderParamFactory.class, remap = false)
public class MixinFogRenderParamFactory {
    @Inject(method = "getFogColor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void actinium$useVanillaFogColorFields(float partialTicks, CallbackInfoReturnable<Color> cir) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.entityRenderer == null) {
            return;
        }

        boolean usingSkyColor = Config.Client.Advanced.Graphics.Fog.colorMode.get() == EDhApiFogColorMode.USE_SKY_COLOR
                && !MinecraftRenderWrapper.INSTANCE.isFogStateSpecial();
        if (usingSkyColor) {
            return;
        }

        AccessorEntityRenderer entityRenderer = (AccessorEntityRenderer) minecraft.entityRenderer;
        cir.setReturnValue(new Color(
                actinium$clampColor(entityRenderer.celeritas$getFogColorRed()),
                actinium$clampColor(entityRenderer.celeritas$getFogColorGreen()),
                actinium$clampColor(entityRenderer.celeritas$getFogColorBlue()),
                1.0F));
    }

    @Unique
    private static float actinium$clampColor(float value) {
        if (Float.isNaN(value)) {
            return 0.0F;
        }
        return Math.clamp(value, 0.0F, 1.0F);
    }
}
