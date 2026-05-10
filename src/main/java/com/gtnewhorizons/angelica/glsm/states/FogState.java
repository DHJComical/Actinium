package com.gtnewhorizons.angelica.glsm.states;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL11;

public class FogState {
    @Getter @Setter private int fogMode = GL11.GL_LINEAR;
    @Getter @Setter private float density = 1.0F;
    @Getter @Setter private float start = 0.0F;
    @Getter @Setter private float end = 1.0F;
    @Getter @Setter private float fogAlpha = 1.0F;
    @Getter private final Vector3d fogColor = new Vector3d();
}
