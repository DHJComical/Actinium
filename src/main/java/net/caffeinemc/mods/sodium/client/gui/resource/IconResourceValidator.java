package net.caffeinemc.mods.sodium.client.gui.resource;

import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/** Fail-fast validation for every explicitly registered configuration icon. */
public final class IconResourceValidator {
    private static final Logger LOGGER = LogManager.getLogger("SodiumGui");

    private IconResourceValidator() {
    }

    public static void validate(List<ModOptions> owners, ResourceValidator validator) {
        if (owners == null || validator == null) {
            throw new IllegalArgumentException("Config owners and resource validator are required");
        }
        for (ModOptions owner : owners) {
            ResourceLocation icon = owner.icon();
            if (icon == null) {
                continue;
            }
            try {
                validator.require(icon);
            } catch (RuntimeException exception) {
                LOGGER.error("Missing Sodium GUI icon resource '{}' for config '{}'", icon, owner.configId(), exception);
                throw new IllegalStateException("Missing Sodium GUI icon resource '" + icon
                        + "' for config '" + owner.configId() + "'", exception);
            }
        }
    }
}
