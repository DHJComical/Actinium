package com.gtnewhorizons.angelica.glsm.states;

import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Material state per-face (ambient, diffuse, specular, emission, shininess).
 * Ported from Angelica.
 */
public class MaterialState implements ISettableState<MaterialState> {
    public int face;

    public final Vector4f ambient;
    public final Vector4f diffuse;
    public final Vector4f specular;
    public final Vector4f emission;
    public float shininess;
    public final Vector3f colorIndexes;

    public MaterialState(int face) {
        this.face = face;
        ambient = new Vector4f(0.2F, 0.2F, 0.2F, 1.0F);
        diffuse = new Vector4f(0.8F, 0.8F, 0.8F, 1.0F);
        specular = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
        emission = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
        shininess = 0.0F;
        colorIndexes = new Vector3f(0.0F, 1.0F, 1.0F);
    }

    @Override
    public MaterialState set(MaterialState state) {
        this.face = state.face;
        this.ambient.set(state.ambient);
        this.diffuse.set(state.diffuse);
        this.specular.set(state.specular);
        this.emission.set(state.emission);
        this.shininess = state.shininess;
        this.colorIndexes.set(state.colorIndexes);
        return this;
    }

    @Override
    public boolean sameAs(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MaterialState ms)) return false;
        return face == ms.face
            && ambient.equals(ms.ambient)
            && diffuse.equals(ms.diffuse)
            && specular.equals(ms.specular)
            && emission.equals(ms.emission)
            && Float.compare(shininess, ms.shininess) == 0
            && colorIndexes.equals(ms.colorIndexes);
    }

    @Override
    public MaterialState copy() {
        return new MaterialState(face).set(this);
    }
}
