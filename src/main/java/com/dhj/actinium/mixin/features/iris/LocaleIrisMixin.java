package com.dhj.actinium.mixin.features.iris;

import net.coderbot.iris.Iris;
import net.coderbot.iris.shaderpack.LanguageMap;
import net.coderbot.iris.shaderpack.ShaderPack;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Mixin(Locale.class)
public class LocaleIrisMixin {
    @Shadow
    Map<String, String> properties;

    @Shadow
    private boolean unicode;

    @Unique
    private static final List<String> actinium$languageCodes = new ArrayList<>();

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

    @Inject(method = "loadLocaleDataFiles(Lnet/minecraft/client/resources/IResourceManager;Ljava/util/List;)V", at = @At("HEAD"))
    private void actinium$trackLanguageCodes(IResourceManager resourceManager, List<String> languageList, CallbackInfo ci) {
        actinium$languageCodes.clear();
        new LinkedList<>(languageList).descendingIterator().forEachRemaining(actinium$languageCodes::add);
    }

    @Inject(method = "checkUnicode()V", at = @At("HEAD"), cancellable = true)
    private void actinium$disableShaderpackUnicodeOverride(CallbackInfo ci) {
        this.unicode = false;
        ci.cancel();
    }

    @Unique
    private String actinium$lookupShaderpackEntry(String key) {
        ShaderPack pack = Iris.getCurrentPack().orElse(null);
        if (pack == null || this.properties.containsKey(key)) {
            return null;
        }

        LanguageMap languageMap = pack.getLanguageMap();
        for (String code : actinium$languageCodes) {
            Map<String, String> translations = languageMap.getTranslations(code);
            if (translations == null) {
                translations = languageMap.getTranslations(actinium$normalizeShaderpackLanguageCode(code));
            }
            if (translations == null) {
                continue;
            }

            String translation = translations.get(key);
            if (translation != null) {
                return translation;
            }
        }

        Map<String, String> fallback = languageMap.getTranslations("en_US");
        return fallback == null ? null : fallback.get(key);
    }

    @Unique
    private static String actinium$normalizeShaderpackLanguageCode(String code) {
        int separator = code.indexOf('_');
        if (separator < 0 || separator == code.length() - 1) {
            return code;
        }

        return code.substring(0, separator).toLowerCase(java.util.Locale.ROOT) + "_" + code.substring(separator + 1).toUpperCase(java.util.Locale.ROOT);
    }
}
