package com.mitchej123.glsm;

import com.mitchej123.glsm.impl.PassThroughGLStateManager;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public final class GLStateManagerServiceProvider {
    public static final GLStateManagerService GL_STATE_MANAGER = loadService();

    private GLStateManagerServiceProvider() {
    }

    private static GLStateManagerService loadService() {
        GLStateManagerService best = null;
        for (GLStateManagerService service : ServiceLoader.load(GLStateManagerService.class, GLStateManagerService.class.getClassLoader())) {
            if (best == null || service.getPriority() > best.getPriority()) {
                best = service;
            }
        }

        if (best != null) {
            return best;
        }

        try {
            return new PassThroughGLStateManager();
        } catch (ServiceConfigurationError error) {
            throw error;
        }
    }
}
