package me.flashyreese.mods.reeses_sodium_options.client.gui.option;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FmlRsoModMetadataResolverTest {
    @Test
    void resolvesRegisteredModFromInfoLookupWithOptionalIcon() {
        FmlRsoModMetadataResolver resolver = resolver(
                modId -> "celeritas".equals(modId) ? info("Celeritas", "2.4.0-dev") : null,
                location -> location.toString().equals("celeritas:textures/gui/config-icon.png"),
                () -> "alpha-0.0.5");

        RsoModMetadata metadata = resolver.resolve("celeritas");

        assertEquals("Celeritas", metadata.name());
        assertEquals("2.4.0-dev", metadata.version());
        assertNotNull(metadata.icon());
        assertEquals(new ResourceLocation("celeritas", "textures/gui/config-icon.png"), metadata.icon());
        assertFalse(metadata.iconMonochrome());
    }

    @Test
    void fallsBackToEmbeddedNamesWithHostVersion() {
        FmlRsoModMetadataResolver resolver = resolver(
                modId -> null,
                location -> false,
                () -> "alpha-0.0.5");

        RsoModMetadata iris = resolver.resolve("iris");
        assertEquals("Iris", iris.name());
        assertEquals("alpha-0.0.5", iris.version());
        assertNull(iris.icon());

        RsoModMetadata rso = resolver.resolve("reeses-sodium-options");
        assertEquals("Reese's Sodium Options", rso.name());
        assertEquals("alpha-0.0.5", rso.version());
    }

    @Test
    void unknownModIdFallsBackToConfigIdWithoutVersionOrIcon() {
        FmlRsoModMetadataResolver resolver = resolver(
                modId -> null,
                location -> true,
                () -> "alpha-0.0.5");

        RsoModMetadata metadata = resolver.resolve("unknownmod");

        assertEquals("unknownmod", metadata.name());
        assertEquals("", metadata.version());
        assertNull(metadata.icon());
    }

    @Test
    void rsoUsesRootIconWhileRegisteredModsProbeConfigIcon() {
        Predicate<ResourceLocation> tracking = location -> {
            if (location.toString().equals("reeses-sodium-options:icon.png")) {
                return true;
            }
            return location.toString().equals("celeritas:textures/gui/config-icon.png");
        };
        FmlRsoModMetadataResolver resolver = resolver(
                modId -> "celeritas".equals(modId) ? info("Celeritas", "2.4.0-dev") : null,
                tracking, () -> "alpha-0.0.5");

        assertEquals(new ResourceLocation("reeses-sodium-options", "icon.png"),
                resolver.resolve("reeses-sodium-options").icon());
        assertEquals(new ResourceLocation("celeritas", "textures/gui/config-icon.png"),
                resolver.resolve("celeritas").icon());
    }

    @Test
    void fallsBackToGuiIconWhenConfigIconIsMissing() {
        Predicate<ResourceLocation> tracking = location ->
                location.toString().equals("celeritas:textures/gui/icon.png");
        FmlRsoModMetadataResolver resolver = resolver(
                modId -> "celeritas".equals(modId) ? info("Celeritas", "2.4.0-dev") : null,
                tracking, () -> "alpha-0.0.5");

        assertEquals(new ResourceLocation("celeritas", "textures/gui/icon.png"),
                resolver.resolve("celeritas").icon());
    }

    private static FmlRsoModMetadataResolver resolver(Function<String, RsoModInfo> lookup,
                                                      Predicate<ResourceLocation> iconAvailable,
                                                      Supplier<String> hostVersion) {
        return new FmlRsoModMetadataResolver(lookup, iconAvailable, hostVersion);
    }

    private static RsoModInfo info(String name, String version) {
        return new RsoModInfo() {
            @Override
            public String displayName() {
                return name;
            }

            @Override
            public String version() {
                return version;
            }
        };
    }
}
