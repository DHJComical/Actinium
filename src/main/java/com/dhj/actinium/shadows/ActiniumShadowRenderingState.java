package com.dhj.actinium.shadows;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class ActiniumShadowRenderingState {
    private static final String MODERN_API_CLASS = "net.irisshaders.iris.api.v0.IrisApi";
    private static final String LEGACY_SHADOW_CLASS = "net.coderbot.iris.shadows.ShadowRenderingState";
    private static final String MODERN_SHADOW_CLASS = "net.irisshaders.iris.shadows.ShadowRenderingState";

    private static final @Nullable IrisApiShadowAccess IRIS_API_SHADOW_ACCESS = findIrisApiShadowMethod();
    private static final @Nullable Method SHADOW_STATE_METHOD = findShadowStateMethod();

    private ActiniumShadowRenderingState() {
    }

    public static boolean areShadowsCurrentlyBeingRendered() {
        if (ActiniumInternalShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            return true;
        }

        Object shadowPass = invoke(IRIS_API_SHADOW_ACCESS != null ? IRIS_API_SHADOW_ACCESS.method() : null, IRIS_API_SHADOW_ACCESS != null ? IRIS_API_SHADOW_ACCESS.instance() : null);
        if (shadowPass instanceof Boolean value) {
            return value;
        }

        shadowPass = invoke(SHADOW_STATE_METHOD, null);
        return shadowPass instanceof Boolean value && value;
    }

    private static @Nullable IrisApiShadowAccess findIrisApiShadowMethod() {
        try {
            Class<?> irisApiClass = Class.forName(MODERN_API_CLASS);
            Method getInstance = irisApiClass.getDeclaredMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method shadowMethod = irisApiClass.getDeclaredMethod("isRenderingShadowPass");
            shadowMethod.setAccessible(true);
            return new IrisApiShadowAccess(instance, shadowMethod);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable Method findShadowStateMethod() {
        Method method = findStaticNoArgMethod(MODERN_SHADOW_CLASS, "areShadowsCurrentlyBeingRendered");
        return method != null ? method : findStaticNoArgMethod(LEGACY_SHADOW_CLASS, "areShadowsCurrentlyBeingRendered");
    }

    private static @Nullable Method findStaticNoArgMethod(String className, String name) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable Object invoke(@Nullable Method method, @Nullable Object instance) {
        if (method == null) {
            return null;
        }

        try {
            return method.invoke(instance);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private record IrisApiShadowAccess(Object instance, Method method) {
    }
}
