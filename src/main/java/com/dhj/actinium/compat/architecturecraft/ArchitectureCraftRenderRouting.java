package com.dhj.actinium.compat.architecturecraft;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Render-path routing decisions for ArchitectureCraft blocks, kept free of live-client
 * dependencies so they can be exercised without an FML bootstrap.
 */
public final class ArchitectureCraftRenderRouting {
    private ArchitectureCraftRenderRouting() {
    }

    /**
     * Returns whether {@code registryName} belongs to the ArchitectureCraft namespace and
     * therefore must stay on the vanilla dispatcher path. Identified by the registry namespace,
     * which covers every block of both maintained releases without referencing any
     * ArchitectureCraft class.
     */
    public static boolean isArchitectureCraftNamespace(@Nullable ResourceLocation registryName) {
        return registryName != null && ArchitectureCraftCompat.MODID.equals(registryName.getNamespace());
    }
}
