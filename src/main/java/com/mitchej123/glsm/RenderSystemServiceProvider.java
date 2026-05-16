package com.mitchej123.glsm;

import com.mitchej123.glsm.impl.PassThroughRenderSystem;

import java.util.ServiceLoader;

public final class RenderSystemServiceProvider {
    public static final RenderSystemService RENDER_SYSTEM = loadService();

    private RenderSystemServiceProvider() {
    }

    private static RenderSystemService loadService() {
        RenderSystemService best = null;
        for (RenderSystemService service : ServiceLoader.load(RenderSystemService.class, RenderSystemService.class.getClassLoader())) {
            if (best == null || service.getPriority() > best.getPriority()) {
                best = service;
            }
        }

        if (best != null) {
            return best;
        }

        return new PassThroughRenderSystem() {
        };
    }
}
