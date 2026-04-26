package org.taumc.actinium.gradle;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.lang.reflect.Method;

public final class ActiniumUniminedHelper {
    private ActiniumUniminedHelper() {
    }

    public static void configureProductionRemap(Project project) {
        project.getTasks().named("remapJar").configure(task -> {
            if (!hasMethod(task.getClass(), "mixinRemap", Function1.class)) {
                return;
            }

            invokeMethod(task, "mixinRemap", new Class<?>[]{Function1.class}, new Function1<Object, Unit>() {
                @Override
                public Unit invoke(Object mixin) {
                    invokeMethod(mixin, "enableBaseMixin");
                    invokeMethod(mixin, "enableMixinExtra");
                    invokeMethod(mixin, "disableRefmap");
                    return Unit.INSTANCE;
                }
            });
        });
    }

    public static void configureSourceAccessTransformers(Project project, String accessTransformerPath) {
        project.getTasks().matching(task -> task.getName().equals("applySourceAccessTransformers")).configureEach(task -> {
            Object accessTransformerFiles = invokeMethod(task, "getAccessTransformerFiles");
            invokeMethod(accessTransformerFiles, "from", new Class<?>[]{Object[].class}, (Object) new Object[]{project.file(accessTransformerPath)});
        });
    }

    private static boolean hasMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            type.getMethod(name, parameterTypes);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static Object invokeMethod(Object target, String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(name, parameterTypes);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke " + name + " on " + target.getClass().getName(), e);
        }
    }

    private static Object invokeMethod(Object target, String name) {
        return invokeMethod(target, name, new Class<?>[0]);
    }
}
