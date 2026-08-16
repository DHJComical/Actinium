package com.gtnewhorizons.angelica.glsm.redirect;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that every {@code glGetActiveUniform} signature the GL redirector can
 * produce (it rewrites matching {@code org.lwjgl.opengl.GL20} calls by method name,
 * preserving the caller's descriptor) actually exists on the GLSM
 * {@code GLStateManager}.
 *
 * <p>HammerLib calls {@code GL20.glGetActiveUniform(int, int, int)} returning
 * {@code String} (issue #40); the redirector rewrites that to
 * {@code GLStateManager.glGetActiveUniform(III)Ljava/lang/String;}, which was
 * missing and failed on mod init with {@code NoSuchMethodError}. A unit test
 * cannot load {@code GLStateManager} because its static initializer requires a
 * live GL context, so the contract is asserted against the compiled class bytes
 * instead (same pattern as {@code MixinConfigurationTest}).</p>
 */
class GLStateManagerRedirectContractTest {
    private static final String GL_STATE_MANAGER = "com/gtnewhorizons.angelica.glsm.GLStateManager";
    private static final String GL_STATE_MANAGER_FILE = GL_STATE_MANAGER.replace('.', '/') + ".class";

    private static final String LEGACY_STRING_FORM = "(III)Ljava/lang/String;";
    private static final String BUFFER_FORM =
        "(IILjava/nio/IntBuffer;Ljava/nio/IntBuffer;Ljava/nio/IntBuffer;Ljava/nio/ByteBuffer;)V";

    @Test
    void glGetActiveUniformLegacyStringFormExists() throws IOException {
        assertTrue(
            glGetActiveUniformDescriptors().contains(LEGACY_STRING_FORM),
            "GLStateManager must expose glGetActiveUniform(int, int, int) returning String: "
                + "the GL redirector rewrites HammerLib's GL20.glGetActiveUniform(III)Ljava/lang/String; "
                + "call to it (issue #40)"
        );
    }

    @Test
    void glGetActiveUniformBufferFormStillExists() throws IOException {
        assertTrue(
            glGetActiveUniformDescriptors().contains(BUFFER_FORM),
            "GLStateManager must keep the buffer-based glGetActiveUniform overload"
        );
    }

    private static Set<String> glGetActiveUniformDescriptors() throws IOException {
        ClassNode classNode = new ClassNode();
        try (InputStream in = GLStateManagerRedirectContractTest.class.getClassLoader()
            .getResourceAsStream(GL_STATE_MANAGER_FILE)) {
            if (in == null) {
                throw new IOException("Could not find " + GL_STATE_MANAGER_FILE + " on the test classpath");
            }
            new ClassReader(in).accept(classNode, 0);
        }
        return classNode.methods.stream()
            .filter(method -> method.name.equals("glGetActiveUniform"))
            .map(method -> method.desc)
            .collect(Collectors.toSet());
    }
}