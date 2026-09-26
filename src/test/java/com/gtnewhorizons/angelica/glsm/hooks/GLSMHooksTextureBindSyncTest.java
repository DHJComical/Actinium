package com.gtnewhorizons.angelica.glsm.hooks;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GLSMHooksTextureBindSyncTest {
    @Test
    void doesNothingWhenNoHostCallbackIsRegistered() {
        GLSMHooks.TextureBindSyncCallback previous = GLSMHooks.textureBindSyncCallback;
        try {
            GLSMHooks.textureBindSyncCallback = null;

            assertDoesNotThrow(() -> GLSMHooks.notifyTextureBindSync(2, 41));
        } finally {
            GLSMHooks.textureBindSyncCallback = previous;
        }
    }

    @Test
    void forwardsTextureUnitAndIdToTheRegisteredHostCallback() {
        GLSMHooks.TextureBindSyncCallback previous = GLSMHooks.textureBindSyncCallback;
        AtomicReference<Binding> received = new AtomicReference<>();
        try {
            GLSMHooks.textureBindSyncCallback = (textureUnit, textureId) ->
                received.set(new Binding(textureUnit, textureId));

            GLSMHooks.notifyTextureBindSync(3, 57);

            assertEquals(new Binding(3, 57), received.get());
        } finally {
            GLSMHooks.textureBindSyncCallback = previous;
        }
    }

    private record Binding(int textureUnit, int textureId) {
    }
}
