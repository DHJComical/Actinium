package org.taumc.celeritas;

import com.dhj.actinium.runtime.ActiniumRuntime;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import org.taumc.celeritas.compat.CeleritasLegacyEventBridge;

/**
 * Exposes Actinium's Celeritas-compatible API under the legacy Celeritas mod id.
 *
 * <p>The bridge is packaged separately so optional Celeritas addons can keep
 * their original dependency checks without changing Actinium's primary mod id.</p>
 */
@Mod(
        modid = CeleritasVintage.MODID,
        useMetadata = true,
        acceptedMinecraftVersions = "[1.12.2]",
        acceptableRemoteVersions = "*",
        clientSideOnly = true,
        dependencies = "required-after:actinium"
)
public class CeleritasVintage {
    public static final String MODID = "celeritas";
    public static String VERSION;

    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) {
        VERSION = Loader.instance().getIndexedModList().get(MODID).getVersion();
        CeleritasLegacyEventBridge.install();
        logger().info("Loaded Celeritas Compatibility Bridge {} for Actinium", VERSION);
    }

    public static Logger logger() {
        return ActiniumRuntime.logger();
    }

    public static SodiumGameOptions options() {
        return ActiniumRuntime.options();
    }
}
