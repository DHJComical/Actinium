package com.gtnewhorizons.angelica.glsm.redirect;

import com.gtnewhorizons.angelica.loading.shared.transformers.AngelicaRedirector;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that JourneyMap's class bytecode (as shipped in the dev runtime,
 * mapped-mcp names) is rewritten by the Angelica redirector so that vanilla
 * {@code GlStateManager} calls reach the GLSM state cache. This is what keeps
 * the FFP shader's {@code u_CurrentColor} uniform in sync with the map grid's
 * {@code GlStateManager.color(...)} calls on the core-profile context.
 *
 * <p>Reads {@code GridSpec.class} straight from the test classpath (the
 * journeymap modImplementation jar is part of the shared Minecraft compile
 * classpath) and runs the exact redirect pipeline used at runtime.</p>
 */
class JourneyMapRedirectorTest {

    private static final String GLSM_GL_STATE_MANAGER = "com/gtnewhorizons/angelica/glsm/GLStateManager";

    @Test
    void journeyMapGridSpecColorCallsAreRedirectedToGlsm() throws IOException {
        byte[] classBytes = readClassBytes("journeymap/client/model/GridSpec.class");
        assertNotNull(classBytes, "journeymap must be on the test classpath (modImplementation)");

        AngelicaRedirector redirector = new AngelicaRedirector();
        assertTrue(redirector.shouldTransform(classBytes), "GridSpec references vanilla GlStateManager and must be a redirect candidate");

        ClassNode cn = new ClassNode();
        new ClassReader(classBytes).accept(cn, 0);
        boolean changed = redirector.transformClassNode("journeymap.client.model.GridSpec", cn);
        assertTrue(changed, "GridSpec's GlStateManager calls must be rewritten");

        boolean glColorRedirected = cn.methods.stream()
            .flatMap(m -> StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(m.instructions.iterator(), 0), false))
            .filter(MethodInsnNode.class::isInstance)
            .map(MethodInsnNode.class::cast)
            .anyMatch(call -> call.owner.equals(GLSM_GL_STATE_MANAGER) && call.name.equals("glColor4f"));
        assertTrue(glColorRedirected, "GridSpec.color(...) must be redirected to GLSM GLStateManager.glColor4f");

        boolean blendRedirected = cn.methods.stream()
            .flatMap(m -> StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(m.instructions.iterator(), 0), false))
            .filter(MethodInsnNode.class::isInstance)
            .map(MethodInsnNode.class::cast)
            .anyMatch(call -> call.owner.equals(GLSM_GL_STATE_MANAGER) && call.name.equals("enableBlend"));
        assertTrue(blendRedirected, "GridSpec.enableBlend() must be redirected to GLSM GLStateManager.enableBlend");
    }

    private static byte[] readClassBytes(String resource) throws IOException {
        try (InputStream in = JourneyMapRedirectorTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        }
    }
}
