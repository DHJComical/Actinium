package me.flashyreese.mods.reeses_sodium_options.client.gui.option;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * FML-backed metadata resolver: prefers the real name and version from a
 * {@link ModContainer}; embedded components (Iris, RSO itself) fall back to
 * the table below and report the host Actinium version. Icon convention: the
 * config-icon resource under the modId (RSO itself uses icon.png); no icon
 * when the resource is missing.
 */
public final class FmlRsoModMetadataResolver implements RsoModMetadataResolver {
    private static final String HOST_MOD_ID = "actinium";
    private static final Map<String, String> EMBEDDED_NAMES = Map.of(
            "iris", "Iris",
            "reeses-sodium-options", "Reese's Sodium Options");
    private static final String[] ICON_CANDIDATES = {
            "textures/gui/config-icon.png",
            "textures/gui/icon.png",
            "icon.png"
    };

    private final Function<String, RsoModInfo> infoLookup;
    private final Predicate<ResourceLocation> iconAvailable;
    private final Supplier<String> hostVersion;

    /** Production entry: queries FML loaded mods and the resource manager. */
    public static FmlRsoModMetadataResolver forClient(IResourceManager resourceManager) {
        return new FmlRsoModMetadataResolver(
                modId -> {
                    ModContainer container = Loader.instance().getIndexedModList().get(modId);
                    if (container == null) {
                        return null;
                    }
                    return new RsoModInfo() {
                        @Override
                        public String displayName() {
                            return container.getMetadata().name;
                        }

                        @Override
                        public String version() {
                            return container.getVersion();
                        }
                    };
                },
                location -> {
                    try {
                        resourceManager.getResource(location);
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                },
                () -> {
                    ModContainer host = Loader.instance().getIndexedModList().get(HOST_MOD_ID);
                    return host == null ? "" : host.getVersion();
                });
    }

    /** Injectable constructor: lets logic tests replace the FML boundary. */
    public FmlRsoModMetadataResolver(Function<String, RsoModInfo> infoLookup,
                                     Predicate<ResourceLocation> iconAvailable,
                                     Supplier<String> hostVersion) {
        this.infoLookup = Objects.requireNonNull(infoLookup, "infoLookup");
        this.iconAvailable = Objects.requireNonNull(iconAvailable, "iconAvailable");
        this.hostVersion = Objects.requireNonNull(hostVersion, "hostVersion");
    }

    @Override
    public RsoModMetadata resolve(String configId) {
        Objects.requireNonNull(configId, "configId");
        RsoModInfo info = this.infoLookup.apply(configId);
        if (info != null) {
            return new RsoModMetadata(info.displayName(), info.version(), this.icon(configId), false);
        }

        String embeddedName = EMBEDDED_NAMES.get(configId);
        if (embeddedName != null) {
            return new RsoModMetadata(embeddedName, this.hostVersion.get(), this.icon(configId), false);
        }

        return new RsoModMetadata(configId, "", null, false);
    }

    /**
     * Probes the conventional icon locations in order: the config icon under
     * textures/gui, the legacy gui icon, and the mod-list icon at the root.
     * RSO itself only ships a root icon.png.
     */
    private ResourceLocation icon(String configId) {
        for (String path : ICON_CANDIDATES) {
            ResourceLocation location = new ResourceLocation(configId, path);
            if (this.iconAvailable.test(location)) {
                return location;
            }
        }
        return null;
    }
}
