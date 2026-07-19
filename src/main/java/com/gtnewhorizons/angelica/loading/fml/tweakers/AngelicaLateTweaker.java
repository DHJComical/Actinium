package com.gtnewhorizons.angelica.loading.fml.tweakers;

import com.dhj.actinium.compat.MixinReEntranceLockFix;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.outlands.foundation.TransformerDelegate;

import java.io.File;
import java.util.List;

public class AngelicaLateTweaker implements ITweaker {

    private static final Logger LOGGER = LogManager.getLogger("ActiniumRedirector");
    private static final String FULL_REDIRECTOR_CLASS = "com.gtnewhorizons.angelica.loading.fml.transformers.AngelicaRedirectorTransformer";

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        try {
            TransformerDelegate.unRegisterTransformer("com.gtnewhorizons.angelica.loading.fml.transformers.EarlyRedirectorTransformer");
            boolean alreadyRegistered = TransformerDelegate.getTransformers().stream()
                .anyMatch(t -> t.getClass().getName().equals(FULL_REDIRECTOR_CLASS));
            if (!alreadyRegistered) {
                LOGGER.debug("Registering transformer {}", FULL_REDIRECTOR_CLASS);
                TransformerDelegate.registerTransformer(new com.gtnewhorizons.angelica.loading.fml.transformers.AngelicaRedirectorTransformer());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to install Angelica redirector late", e);
        } finally {
            // Instantiating the redirector above forces the first class loads through the
            // mixin transformer, so mixin select/prepare has completed by this point. If a
            // legacy coremod transformer re-entered mixin during prepare (e.g. Techguns
            // resolving supertypes), the processor's re-entrance lock depth is now leaked
            // and every later applyMixins would throw; restore the balanced state here.
            MixinReEntranceLockFix.clearLeakedLock();
        }
        return new String[0];
    }
}
