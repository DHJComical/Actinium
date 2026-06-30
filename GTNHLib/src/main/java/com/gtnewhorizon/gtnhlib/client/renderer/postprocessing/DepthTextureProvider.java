package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import net.minecraft.client.shader.Framebuffer;

public interface DepthTextureProvider {
    int getDepthTextureId(Framebuffer framebuffer);
}
