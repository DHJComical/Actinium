package com.dhj.actinium.config;

import com.dhj.actinium.runtime.ActiniumRuntime;

public final class ActiniumRuntimeOptions {
    private static final String ALLOW_DIRECT_MEMORY_ACCESS_PROPERTY = "actinium.allowDirectMemoryAccess";
    private static final String MODEL_RENDERER_BATCHING_PROPERTY = "actinium.modelRendererBatching";
    private static final String MODEL_RENDERER_DISPLAY_LISTS_PROPERTY = "actinium.modelRendererDisplayLists";
    private static final String FAST_LIT_ITEM_RENDERING_PROPERTY = "actinium.fastLitItemRendering";
    private static final String FAST_LIT_ITEM_DISPLAY_LISTS_PROPERTY = "actinium.fastLitItemDisplayLists";

    private ActiniumRuntimeOptions() {
    }

    public static boolean allowDirectMemoryAccess() {
        String override = System.getProperty(ALLOW_DIRECT_MEMORY_ACCESS_PROPERTY);
        if (override != null) {
            return Boolean.parseBoolean(override);
        }

        try {
            return ActiniumRuntime.options().advanced.allowDirectMemoryAccess;
        } catch (RuntimeException | LinkageError ignored) {
            return true;
        }
    }

    public static boolean useModelRendererBatching() {
        String override = System.getProperty(MODEL_RENDERER_BATCHING_PROPERTY);
        if (override != null) {
            return Boolean.parseBoolean(override);
        }

        return true;
    }

    public static boolean useModelRendererDisplayLists() {
        String override = System.getProperty(MODEL_RENDERER_DISPLAY_LISTS_PROPERTY);
        if (override != null) {
            return Boolean.parseBoolean(override);
        }

        try {
            return ActiniumRuntime.options().advanced.useModelRendererDisplayLists;
        } catch (RuntimeException | LinkageError ignored) {
            return true;
        }
    }

    public static boolean useFastLitItemRendering() {
        String override = System.getProperty(FAST_LIT_ITEM_RENDERING_PROPERTY);
        if (override != null) {
            return Boolean.parseBoolean(override);
        }

        try {
            return ActiniumRuntime.options().advanced.useFastLitItemRendering;
        } catch (RuntimeException | LinkageError ignored) {
            return true;
        }
    }

    public static boolean useFastLitItemDisplayLists() {
        String override = System.getProperty(FAST_LIT_ITEM_DISPLAY_LISTS_PROPERTY);
        if (override != null) {
            return Boolean.parseBoolean(override);
        }

        try {
            return ActiniumRuntime.options().advanced.useFastLitItemDisplayLists;
        } catch (RuntimeException | LinkageError ignored) {
            return true;
        }
    }
}
