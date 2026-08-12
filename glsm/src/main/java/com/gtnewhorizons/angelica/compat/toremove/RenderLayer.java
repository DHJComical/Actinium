package com.gtnewhorizons.angelica.compat.toremove;

public final class RenderLayer {
    private final String name;

    private RenderLayer(String name) {
        this.name = name;
    }

    public static RenderLayer solid() {
        return new RenderLayer("solid");
    }

    public static RenderLayer cutout() {
        return new RenderLayer("cutout");
    }

    public static RenderLayer translucent() {
        return new RenderLayer("translucent");
    }

    @Override
    public String toString() {
        return this.name;
    }
}
