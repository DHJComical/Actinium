package com.gtnewhorizons.angelica.glsm;

public final class GLDebug {
    private GLDebug() {
    }

    public static void nameObject(int id, int object, String name) {
        org.embeddedt.embeddium.impl.gl.debug.GLDebug.nameObject(id, object, name);
    }

    public static void pushGroup(int id, String name) {
        org.embeddedt.embeddium.impl.gl.debug.GLDebug.pushGroup(id, name);
    }

    public static void popGroup() {
        org.embeddedt.embeddium.impl.gl.debug.GLDebug.popGroup();
    }
}
