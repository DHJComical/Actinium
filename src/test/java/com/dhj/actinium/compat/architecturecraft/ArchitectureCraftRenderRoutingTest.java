package com.dhj.actinium.compat.architecturecraft;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureCraftRenderRoutingTest {
    @Test
    void architectureCraftBlocksAreForcedOntoTheVanillaDispatcher() {
        assertTrue(ArchitectureCraftRenderRouting.isArchitectureCraftNamespace(
                new ResourceLocation("architecturecraft", "sawbench")));
        assertTrue(ArchitectureCraftRenderRouting.isArchitectureCraftNamespace(
                new ResourceLocation("architecturecraft", "shape")));
    }

    @Test
    void foreignAndMissingRegistryNamesStayOnTheFastPath() {
        assertFalse(ArchitectureCraftRenderRouting.isArchitectureCraftNamespace(
                new ResourceLocation("minecraft", "stone")));
        assertFalse(ArchitectureCraftRenderRouting.isArchitectureCraftNamespace(
                new ResourceLocation("chisel", "marble")));
        assertFalse(ArchitectureCraftRenderRouting.isArchitectureCraftNamespace(null));
    }
}
