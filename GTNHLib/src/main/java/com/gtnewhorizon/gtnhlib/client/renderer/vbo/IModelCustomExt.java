package com.gtnewhorizon.gtnhlib.client.renderer.vbo;

import static net.minecraftforge.fml.relauncher.Side.CLIENT;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

import net.minecraftforge.fml.relauncher.SideOnly;

public interface IModelCustomExt {

    // There's no reason for another mod to call this, it should only be used internally.
    @Deprecated
    @SideOnly(CLIENT)
    void rebuildVBO();

    @SideOnly(CLIENT)
    void renderAllVBO();

    @Deprecated // Same as renderAllVBO
    @SideOnly(CLIENT)
    void renderAllVAO();

    // These will likely get removed/changed sooner or later
    @Beta
    @SideOnly(CLIENT)
    void setVertexFormat(VertexFormat format);

    @Beta
    @SideOnly(CLIENT)
    void setVertexFormat(VertexFormat format, boolean vao);
}
