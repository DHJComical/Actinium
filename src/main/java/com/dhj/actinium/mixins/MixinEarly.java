package com.dhj.actinium.mixins;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.Name("Actinium")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class MixinEarly implements IFMLLoadingPlugin, IEarlyMixinLoader {
    private static final List<String> MIXIN_CONFIGS = Arrays.asList(
        "mixins.actinium.vintage.json",
        "mixins.actinium.iris.json"
    );

    @Override
    public @Nullable String[] getASMTransformerClass() {
        return new String[] {
            "com.gtnewhorizons.angelica.loading.fml.transformers.EarlyRedirectorTransformer"
        };
    }

    @Override
    public @Nullable String getModContainerClass() {
        return null;
    }

    @Override
    public @Nullable String getSetupClass() {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void injectData(Map<String, Object> data) {
        List<String> tweaks = GlobalProperties.get(MixinServiceLaunchWrapper.BLACKBOARD_KEY_TWEAKCLASSES);
        if (tweaks == null) {
            Object value = Launch.blackboard.get("TweakClasses");
            if (value instanceof List<?>) {
                tweaks = (List<String>) value;
            }
        }
        if (tweaks == null) {
            tweaks = new ArrayList<>();
            Launch.blackboard.put("TweakClasses", tweaks);
        }
        if (!tweaks.contains("com.gtnewhorizons.angelica.loading.fml.tweakers.AngelicaLateTweaker")) {
            tweaks.add("com.gtnewhorizons.angelica.loading.fml.tweakers.AngelicaLateTweaker");
        }
    }

    @Override
    public @Nullable String getAccessTransformerClass() {
        return null;
    }

    @Override
    public List<String> getMixinConfigs() {
        return getEarlyMixinConfigs();
    }

    public static List<String> getEarlyMixinConfigs() {
        return MIXIN_CONFIGS;
    }
}
