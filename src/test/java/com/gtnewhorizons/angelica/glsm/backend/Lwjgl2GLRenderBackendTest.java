package com.gtnewhorizons.angelica.glsm.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Lwjgl2GLRenderBackendTest {
    @Test
    void selectsImplementedDebugOutputApisInPriorityOrder() {
        assertEquals(
            Lwjgl2GLRenderBackend.DebugOutputApi.OPENGL_43,
            Lwjgl2GLRenderBackend.selectDebugOutputApi(true, true, true, true)
        );
        assertEquals(
            Lwjgl2GLRenderBackend.DebugOutputApi.KHR_DEBUG,
            Lwjgl2GLRenderBackend.selectDebugOutputApi(false, true, true, true)
        );
    }

    @Test
    void doesNotAdvertiseUnimplementedArbOrAmdCallbacks() {
        assertEquals(
            Lwjgl2GLRenderBackend.DebugOutputApi.NONE,
            Lwjgl2GLRenderBackend.selectDebugOutputApi(false, false, true, false)
        );
        assertEquals(
            Lwjgl2GLRenderBackend.DebugOutputApi.NONE,
            Lwjgl2GLRenderBackend.selectDebugOutputApi(false, false, false, true)
        );
    }
}
