package org.taumc.celeritas.compat.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyRendererAccessorContractTest {
    private static final String ACCESSOR_ANNOTATION =
            "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER_ANNOTATION =
            "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String MIXIN_CLASS =
            "org/taumc/celeritas/compat/mixin/LegacyRendererAccessMixin.class";
    private static final String TARGET_CLASS =
            "com/dhj/actinium/render/terrain/compile/pipeline/VintageBlockRenderer.class";

    @Test
    void accessorDescriptorsExactlyMatchRendererFields() throws IOException {
        ClassNode mixin = readClass(MIXIN_CLASS);
        ClassNode target = readClass(TARGET_CLASS);
        Map<String, String> targetFields = new HashMap<>();
        int checkedAccessors = 0;
        for (FieldNode field : target.fields) {
            targetFields.put(field.name, field.desc);
        }

        for (MethodNode method : mixin.methods) {
            AnnotationNode accessor = findAnnotation(method.visibleAnnotations, ACCESSOR_ANNOTATION);
            if (accessor == null) {
                continue;
            }

            checkedAccessors++;
            String fieldName = annotationStringValue(accessor, "value");
            String fieldDescriptor = targetFields.get(fieldName);
            assertNotNull(fieldDescriptor, "Missing target field " + fieldName);
            assertEquals(fieldDescriptor, accessorFieldDescriptor(method),
                    method.name + " must use the exact descriptor of " + fieldName);
        }
        assertTrue(checkedAccessors > 0, "The renderer bridge must declare Accessors");
    }

    @Test
    void invokerDescriptorsExactlyMatchRendererMethods() throws IOException {
        ClassNode mixin = readClass(MIXIN_CLASS);
        ClassNode target = readClass(TARGET_CLASS);
        Map<String, List<String>> targetMethods = new HashMap<>();
        int checkedInvokers = 0;
        for (MethodNode method : target.methods) {
            targetMethods.computeIfAbsent(method.name, ignored -> new ArrayList<>()).add(method.desc);
        }

        for (MethodNode method : mixin.methods) {
            AnnotationNode invoker = findAnnotation(method.visibleAnnotations, INVOKER_ANNOTATION);
            if (invoker == null) {
                continue;
            }

            checkedInvokers++;
            String targetName = annotationStringValue(invoker, "value");
            List<String> descriptors = targetMethods.get(targetName);
            assertNotNull(descriptors, "Missing target method " + targetName);
            assertTrue(descriptors.contains(method.desc),
                    method.name + " must use an exact descriptor of " + targetName);
        }
        assertTrue(checkedInvokers > 0, "The renderer bridge must declare Invokers");
    }

    private static String accessorFieldDescriptor(MethodNode method) {
        Type methodType = Type.getMethodType(method.desc);
        Type returnType = methodType.getReturnType();
        if (returnType.getSort() != Type.VOID) {
            assertEquals(0, methodType.getArgumentTypes().length,
                    method.name + " is an invalid getter Accessor");
            return returnType.getDescriptor();
        }

        assertEquals(1, methodType.getArgumentTypes().length,
                method.name + " is an invalid setter Accessor");
        return methodType.getArgumentTypes()[0].getDescriptor();
    }

    private static AnnotationNode findAnnotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) {
            return null;
        }
        return annotations.stream()
                .filter(annotation -> annotation.desc.equals(descriptor))
                .findFirst()
                .orElse(null);
    }

    private static String annotationStringValue(AnnotationNode annotation, String key) {
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (annotation.values.get(index).equals(key)) {
                return (String) annotation.values.get(index + 1);
            }
        }
        throw new AssertionError("Missing annotation value " + key);
    }

    private static ClassNode readClass(String resourceName) throws IOException {
        ClassLoader classLoader = LegacyRendererAccessorContractTest.class.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(resourceName)) {
            assertNotNull(stream, "Missing compiled class " + resourceName);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG
                    | ClassReader.SKIP_FRAMES);
            return node;
        }
    }
}
