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
                continue;
            }

            String translation = translations.get(key);
            if (translation != null) {
                return translation;
            }
        }

        return null;
    }
}
