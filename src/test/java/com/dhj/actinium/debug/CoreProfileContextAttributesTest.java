package com.dhj.actinium.debug;

import net.minecraftforge.common.ForgeEarlyConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreProfileContextAttributesTest {
    @Test
    void configuresTheOriginalContextInRequiredOrder() {
        RecordingContext attributes = new RecordingContext();

        RecordingContext configured = CoreProfileContextAttributes.configure(
            attributes,
            value -> value.record("core"),
            value -> value.record("forward"),
            value -> value.record("debug")
        );

        assertSame(attributes, configured);
        assertEquals(List.of("core", "forward", "debug"), attributes.operations());
    }

    @Test
    void appliesForgeEarlyCoreProfileForLwjglxx() {
        int originalMajor = ForgeEarlyConfig.OPENGL_VERSION_MAJOR;
        int originalMinor = ForgeEarlyConfig.OPENGL_VERSION_MINOR;
        boolean originalCompatProfile = ForgeEarlyConfig.OPENGL_COMPAT_PROFILE;
        boolean originalDebugContext = ForgeEarlyConfig.OPENGL_DEBUG_CONTEXT;

        try {
            CoreProfileContextAttributes.applyForgeEarlyCoreProfile(4, 1, true);

            assertEquals(4, ForgeEarlyConfig.OPENGL_VERSION_MAJOR);
            assertEquals(1, ForgeEarlyConfig.OPENGL_VERSION_MINOR);
            assertFalse(ForgeEarlyConfig.OPENGL_COMPAT_PROFILE);
            assertTrue(ForgeEarlyConfig.OPENGL_DEBUG_CONTEXT);
        } finally {
            ForgeEarlyConfig.OPENGL_VERSION_MAJOR = originalMajor;
            ForgeEarlyConfig.OPENGL_VERSION_MINOR = originalMinor;
            ForgeEarlyConfig.OPENGL_COMPAT_PROFILE = originalCompatProfile;
            ForgeEarlyConfig.OPENGL_DEBUG_CONTEXT = originalDebugContext;
        }
    }

    @Test
    void restoresCompatProfileForLwjglxxAfterCoreContextCreation() {
        int originalMajor = ForgeEarlyConfig.OPENGL_VERSION_MAJOR;
        int originalMinor = ForgeEarlyConfig.OPENGL_VERSION_MINOR;
        boolean originalCompatProfile = ForgeEarlyConfig.OPENGL_COMPAT_PROFILE;
        boolean originalDebugContext = ForgeEarlyConfig.OPENGL_DEBUG_CONTEXT;

        try {
            CoreProfileContextAttributes.applyForgeEarlyCoreProfile(4, 6, true);
            assertFalse(ForgeEarlyConfig.OPENGL_COMPAT_PROFILE);

            CoreProfileContextAttributes.restoreForgeEarlyCompatProfile(4, 6, false);

            assertEquals(4, ForgeEarlyConfig.OPENGL_VERSION_MAJOR);
            assertEquals(6, ForgeEarlyConfig.OPENGL_VERSION_MINOR);
            assertTrue(ForgeEarlyConfig.OPENGL_COMPAT_PROFILE,
                "The compatibility profile must be restored so Cleanroom can start without Actinium");
            assertFalse(ForgeEarlyConfig.OPENGL_DEBUG_CONTEXT);
        } finally {
            ForgeEarlyConfig.OPENGL_VERSION_MAJOR = originalMajor;
            ForgeEarlyConfig.OPENGL_VERSION_MINOR = originalMinor;
            ForgeEarlyConfig.OPENGL_COMPAT_PROFILE = originalCompatProfile;
            ForgeEarlyConfig.OPENGL_DEBUG_CONTEXT = originalDebugContext;
        }
    }

    private static final class RecordingContext {
        private final List<String> operations = new ArrayList<>();

        private void record(String operation) {
            operations.add(operation);
        }

        private List<String> operations() {
            return List.copyOf(operations);
        }
    }
}
