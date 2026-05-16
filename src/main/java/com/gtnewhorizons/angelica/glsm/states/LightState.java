package com.gtnewhorizons.angelica.glsm.states;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

/**
 * Individual light source state (ambient, diffuse, specular, position, spot, attenuation).
 * Ported from Angelica.
 */
public class LightState implements ISettableState<LightState> {
    public int light;

    public final Vector4f ambient;
    public final Vector4f diffuse;
    public final Vector4f specular;
    public final Vector4f position;
    public final Vector3f spotDirection;
    public float spotExponent;
    public float spotCutoff;
    public float spotCosCutoff;
    public float constantAttenuation;
    public float linearAttenuation;
    public float quadraticAttenuation;

    public LightState(int light) {
        this.light = light;
        this.ambient = new Vector4f(0F, 0F, 0F, 1F);
        this.diffuse = new Vector4f(0F, 0F, 0F, 1F);
        this.specular = new Vector4f(0F, 0F, 0F, 1F);
        this.position = new Vector4f(0F, 0F, 1F, 0F);
        this.spotDirection = new Vector3f(0F, 0F, -1F);
        this.spotExponent = 0F;
        this.spotCutoff = 180F;
        this.spotCosCutoff = (float) Math.cos(Math.toRadians(180));
        this.constantAttenuation = 1.0F;
        this.linearAttenuation = 0.0F;
        this.quadraticAttenuation = 0.0F;

        if (light == GL11.GL_LIGHT0) {
            this.diffuse.set(1F, 1F, 1F, 1F);
            this.specular.set(1F, 1F, 1F, 1F);
        }
    }

    @Override
    public LightState set(LightState state) {
        this.light = state.light;
        this.ambient.set(state.ambient);
        this.diffuse.set(state.diffuse);
        this.specular.set(state.specular);
        this.position.set(state.position);
        this.spotDirection.set(state.spotDirection);
        this.spotExponent = state.spotExponent;
        this.spotCutoff = state.spotCutoff;
        this.spotCosCutoff = state.spotCosCutoff;
        this.constantAttenuation = state.constantAttenuation;
        this.linearAttenuation = state.linearAttenuation;
        this.quadraticAttenuation = state.quadraticAttenuation;
        return this;
    }

    @Override
    public boolean sameAs(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LightState ls)) return false;
        return light == ls.light
            && ambient.equals(ls.ambient)
            && diffuse.equals(ls.diffuse)
            && specular.equals(ls.specular)
            && position.equals(ls.position)
            && spotDirection.equals(ls.spotDirection)
            && Float.compare(spotExponent, ls.spotExponent) == 0
            && Float.compare(spotCutoff, ls.spotCutoff) == 0
            && Float.compare(constantAttenuation, ls.constantAttenuation) == 0
            && Float.compare(linearAttenuation, ls.linearAttenuation) == 0
            && Float.compare(quadraticAttenuation, ls.quadraticAttenuation) == 0;
    }

    @Override
    public LightState copy() {
        return new LightState(light).set(this);
    }
}
