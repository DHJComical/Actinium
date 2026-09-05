package org.embeddedt.embeddium.impl.render.chunk;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@code RenderSectionManager.update(Viewport, int, boolean)} keeps the
 * {@code CameraTransform.x/y/z} getfields inline in its own method body.
 *
 * <p>HBM-CE's {@code MixinRenderSectionManager} (from {@code hbm.mod.mixin.json}) applies
 * {@code @Redirect} injections on that exact method which rewrite those getfields to its
 * unsafe accessors ({@code CeleritasCameraTransformAccess}). A redirector fails with a
 * hard {@code InjectionError} ({@code require >= 1}) when it scans zero targets, which
 * poisons the whole {@code RenderSectionManager} class and crashes mod construction with
 * a {@code NoClassDefFoundError} — so the getfields must not be delegated to a helper
 * method like {@code updateCameraPosition}.</p>
 *
 * <p>A unit test cannot load {@code RenderSectionManager} because the class is rewritten
 * by third-party mixins at load time, so the contract is asserted against the compiled
 * class bytes instead (same pattern as {@code GLStateManagerRedirectContractTest}).</p>
 */
class HbmCameraRedirectContractTest {
    private static final String RENDER_SECTION_MANAGER_CLASS =
        "org/embeddedt/embeddium/impl/render/chunk/RenderSectionManager";
    private static final String CAMERA_TRANSFORM_CLASS =
        "org/embeddedt/embeddium/impl/render/viewport/CameraTransform";
    private static final String UPDATE_DESC =
        "(Lorg/embeddedt/embeddium/impl/render/viewport/Viewport;IZ)V";

    /**
     * Motivation: HBM-CE redirects each of the three camera getfields in {@code update};
     * a missing one aborts mixin application and breaks world loading.
     */
    @Test
    void updateMethodBodyKeepsInlineCameraGetfields() throws IOException {
        MethodNode update = findUpdateMethod();
        assertGetfieldPresent(update, "x", "HBM-CE's useUnsafeCameraX redirect must find CameraTransform.x in update(Viewport, int, boolean)");
        assertGetfieldPresent(update, "y", "HBM-CE's useUnsafeCameraY redirect must find CameraTransform.y in update(Viewport, int, boolean)");
        assertGetfieldPresent(update, "z", "HBM-CE's useUnsafeCameraZ redirect must find CameraTransform.z in update(Viewport, int, boolean)");
    }

    /**
     * Motivation: the redirector is registered against {@code update} by name; if that
     * method disappears or is renamed the mixin fails regardless of the getfields.
     */
    @Test
    void updateMethodWithNameUsedByHbmRedirectStillExists() throws IOException {
        assertNotNull(findUpdateMethod(), "RenderSectionManager.update(Viewport, int, boolean) must exist: "
            + "HBM-CE's MixinRenderSectionManager registers its camera redirects on that method name");
    }

    /**
     * Loads the compiled {@code RenderSectionManager} class bytes and returns its
     * {@code update(Viewport, int, boolean)} method node.
     */
    private static MethodNode findUpdateMethod() throws IOException {
        String classFile = RENDER_SECTION_MANAGER_CLASS + ".class";
        ClassNode classNode = new ClassNode();
        try (InputStream in = HbmCameraRedirectContractTest.class.getClassLoader()
            .getResourceAsStream(classFile)) {
            if (in == null) {
                throw new IOException("Could not find " + classFile + " on the test classpath");
            }
            new ClassReader(in).accept(classNode, 0);
        }
        return classNode.methods.stream()
            .filter(method -> method.name.equals("update"))
            .filter(method -> method.desc.equals(UPDATE_DESC))
            .findFirst()
            .orElse(null);
    }

    /**
     * Asserts that the method body contains a {@code GETFIELD CameraTransform.<fieldName>:D}
     * instruction, the injection target of HBM-CE's camera redirect.
     */
    private static void assertGetfieldPresent(MethodNode update, String fieldName, String message) {
        boolean found = false;
        for (AbstractInsnNode insn : update.instructions) {
            if (insn instanceof FieldInsnNode field
                && field.getOpcode() == Opcodes.GETFIELD
                && field.owner.equals(CAMERA_TRANSFORM_CLASS)
                && field.name.equals(fieldName)
                && field.desc.equals("D")) {
                found = true;
                break;
            }
        }
        assertTrue(found, message);
    }
}
