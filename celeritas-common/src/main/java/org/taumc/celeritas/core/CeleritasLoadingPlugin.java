package org.taumc.celeritas.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.launchwrapper.Launch;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.Name("Celeritas")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class CeleritasLoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {
    @Override
    public @Nullable String[] getASMTransformerClass() {
        return new String[] {
            "com.gtnewhorizons.angelica.loading.fml.transformers.EarlyRedirectorTransformer",
            "org.taumc.celeritas.core.CeleritasLWJGLRelocationTransformer"
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
    public void injectData(Map<String, Object> map) {
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
        return Arrays.asList(
            "mixins.celeritas.json",
            "mixins.actinium.json",
            "mixins.gnetum.early.json"
        );
    }
}
