package com.dhj.actinium.mixin.features.iris;

import com.dhj.actinium.gui.ShaderPackTranslationLookup;
import net.coderbot.iris.Iris;
import net.coderbot.iris.shaderpack.ShaderPack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wires shader pack lang directory translations into the vanilla I18n lookup chain so the
 * shader pack option GUI (option names, values, comments and sub-screen titles) follows the
 * game language. The lookup logic lives in {@link ShaderPackTranslationLookup}; this class
 * only injects and collects the language order.
 */
@Mixin(Locale.class)
public class LocaleIrisMixin {
    @Shadow
    Map<String, String> properties;

    @Shadow
    private boolean unicode;

    /**
     * Fallback code of the language chain: en_us is the vanilla fallback language, so packs
     * without a lang file for the current game language degrade to English instead of raw
     * keys or numeric values.
     */
    @Unique
    private static final String actinium$FALLBACK_LANGUAGE_CODE = "en_us";

    @Inject(method = "translateKeyPrivate(Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void actinium$overrideShaderpackLanguageEntry(String key, CallbackInfoReturnable<String> cir) {
        String override = this.actinium$lookupShaderpackEntry(key);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "hasKey(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
    private void actinium$hasShaderpackLanguageEntry(String key, CallbackInfoReturnable<Boolean> cir) {
        if (this.actinium$lookupShaderpackEntry(key) != null) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "checkUnicode()V", at = @At("HEAD"), cancellable = true)
    private void actinium$disableShaderpackUnicodeOverride(CallbackInfo ci) {
        this.unicode = false;
        ci.cancel();
    }

    @Unique
    private String actinium$lookupShaderpackEntry(String key) {
        ShaderPack pack = Iris.getCurrentPack().orElse(null);
        if (pack == null) {
            return null;
        }

        return ShaderPackTranslationLookup.lookup(
            pack.getLanguageMap(),
            this.properties,
            key,
            actinium$preferredLanguageCodes());
    }

    /**
     * Collects the language lookup order: current game language first, en_us fallback, matching
     * the vanilla Locale load order. Motivation: the order must be read live (gameSettings.language
     * changes whenever the language settings screen is used); caching a snapshot from the resource
     * reload callback breaks the whole chain when the snapshot goes stale or out of sync.
     */
    @Unique
    private static List<String> actinium$preferredLanguageCodes() {
        List<String> codes = new ArrayList<>(2);
        codes.add(Minecraft.getMinecraft().gameSettings.language);
        if (!codes.contains(actinium$FALLBACK_LANGUAGE_CODE)) {
            codes.add(actinium$FALLBACK_LANGUAGE_CODE);
        }
        return codes;
    }
}
