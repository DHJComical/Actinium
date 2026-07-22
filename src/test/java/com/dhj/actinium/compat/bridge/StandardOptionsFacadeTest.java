package com.dhj.actinium.compat.bridge;

import org.junit.jupiter.api.Test;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.StandardOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardOptionsFacadeTest {
    @Test
    void preservesLegacyIdentifierAbiSemantics() {
        assertSame(void.class, StandardOptions.Group.WINDOW.getType());
        assertSame(OptionIdentifier.create("embeddium", "compat_identity"),
                OptionIdentifier.create("celeritas", "compat_identity"));

        OptionIdentifier<String> typed = OptionIdentifier.create("compat_test", "typed", String.class);
        assertThrows(IllegalArgumentException.class,
                () -> OptionIdentifier.create("compat_test", "typed", Integer.class));
        assertFalse(OptionIdentifier.isPresent(null));
        assertFalse(OptionIdentifier.isPresent(OptionIdentifier.EMPTY));
        assertTrue(OptionIdentifier.isPresent(typed));
    }

    @Test
    void matchesCanonicalIdentifiersUsedByCeleritasExtra() {
        assertTrue(StandardOptions.Group.WINDOW.matches(OptionIdentifier.create("minecraft", "window")));
        assertTrue(StandardOptions.Option.FULLSCREEN.matches(OptionIdentifier.create("minecraft", "fullscreen")));
        assertTrue(StandardOptions.Option.VSYNC.matches(OptionIdentifier.create("minecraft", "vsync")));
        assertIdentifier(StandardOptions.Option.FULLSCREEN_RESOLUTION, "minecraft", "fullscreen_resolution");
    }

    @Test
    void exposesEveryLegacyGroupAndPageIdentifier() {
        assertIdentifiers("minecraft",
                StandardOptions.Group.RENDERING, "rendering",
                StandardOptions.Group.WINDOW, "window",
                StandardOptions.Group.INDICATORS, "indicators",
                StandardOptions.Group.GRAPHICS, "graphics",
                StandardOptions.Group.MIPMAPS, "mipmaps",
                StandardOptions.Group.DETAILS, "details");
        assertIdentifiers("celeritas",
                StandardOptions.Group.CHUNK_UPDATES, "chunk_updates",
                StandardOptions.Group.RENDERING_CULLING, "rendering_culling",
                StandardOptions.Group.CPU_SAVING, "cpu_saving",
                StandardOptions.Group.SORTING, "sorting",
                StandardOptions.Group.LIGHTING, "lighting",
                StandardOptions.Pages.GENERAL, "general",
                StandardOptions.Pages.QUALITY, "quality",
                StandardOptions.Pages.PERFORMANCE, "performance",
                StandardOptions.Pages.ADVANCED, "advanced",
                StandardOptions.Pages.SHADERS, "shaders");
    }

    @Test
    void exposesEveryLegacyMinecraftOptionIdentifier() {
        assertIdentifiers("minecraft",
                StandardOptions.Option.RENDER_DISTANCE, "render_distance",
                StandardOptions.Option.SIMULATION_DISTANCE, "simulation_distance",
                StandardOptions.Option.BRIGHTNESS, "brightness",
                StandardOptions.Option.GUI_SCALE, "gui_scale",
                StandardOptions.Option.FULLSCREEN, "fullscreen",
                StandardOptions.Option.FULLSCREEN_RESOLUTION, "fullscreen_resolution",
                StandardOptions.Option.VSYNC, "vsync",
                StandardOptions.Option.MAX_FRAMERATE, "max_frame_rate",
                StandardOptions.Option.VIEW_BOBBING, "view_bobbing",
                StandardOptions.Option.INACTIVITY_FPS_LIMIT, "inactivity_fps_limit",
                StandardOptions.Option.ATTACK_INDICATOR, "attack_indicator",
                StandardOptions.Option.AUTOSAVE_INDICATOR, "autosave_indicator",
                StandardOptions.Option.GRAPHICS_MODE, "graphics_mode",
                StandardOptions.Option.CLOUDS, "clouds",
                StandardOptions.Option.WEATHER, "weather",
                StandardOptions.Option.LEAVES, "leaves",
                StandardOptions.Option.PARTICLES, "particles",
                StandardOptions.Option.SMOOTH_LIGHT, "smooth_lighting",
                StandardOptions.Option.BIOME_BLEND, "biome_blend",
                StandardOptions.Option.ENTITY_DISTANCE, "entity_distance",
                StandardOptions.Option.ENTITY_SHADOWS, "entity_shadows",
                StandardOptions.Option.VIGNETTE, "vignette",
                StandardOptions.Option.MIPMAP_LEVEL, "mipmap_levels");
    }

    @Test
    void exposesEveryLegacyCeleritasOptionIdentifier() {
        assertIdentifiers("celeritas",
                StandardOptions.Option.CHUNK_UPDATE_THREADS, "chunk_update_threads",
                StandardOptions.Option.DEFFER_CHUNK_UPDATES, "defer_chunk_updates",
                StandardOptions.Option.BLOCK_FACE_CULLING, "block_face_culling",
                StandardOptions.Option.COMPACT_VERTEX_FORMAT, "compact_vertex_format",
                StandardOptions.Option.FOG_OCCLUSION, "fog_occlusion",
                StandardOptions.Option.ENTITY_CULLING, "entity_culling",
                StandardOptions.Option.ANIMATE_VISIBLE_TEXTURES, "animate_only_visible_textures",
                StandardOptions.Option.NO_ERROR_CONTEXT, "no_error_context",
                StandardOptions.Option.PERSISTENT_MAPPING, "persistent_mapping",
                StandardOptions.Option.CPU_FRAMES_AHEAD, "cpu_render_ahead_limit",
                StandardOptions.Option.TRANSLUCENT_FACE_SORTING, "translucent_face_sorting",
                StandardOptions.Option.USE_QUAD_NORMALS_FOR_LIGHTING, "use_quad_normals_for_lighting",
                StandardOptions.Option.RENDER_PASS_OPTIMIZATION, "render_pass_optimization",
                StandardOptions.Option.RENDER_PASS_CONSOLIDATION, "render_pass_consolidation",
                StandardOptions.Option.USE_FASTER_CLOUDS, "use_faster_clouds",
                StandardOptions.Option.ASYNC_GRAPH_SEARCH, "async_graph_search",
                StandardOptions.Option.CHUNK_FADE_IN_DURATION, "chunk_fade_in_duration");
    }

    @Test
    void exposesNonConflictingActiniumExtensions() {
        assertIdentifiers("actinium",
                StandardOptions.Group.ACTINIUM_DEBUG, "debug",
                StandardOptions.Pages.DEBUG, "debug",
                StandardOptions.Option.FULLSCREEN_MODE, "fullscreen_mode",
                StandardOptions.Option.STREAMING_UPLOAD_STRATEGY, "streaming_upload_strategy",
                StandardOptions.Option.DEFERRED_PARTICLE_BATCHING, "deferred_particle_batching",
                StandardOptions.Option.ALLOW_DIRECT_MEMORY_ACCESS, "allow_direct_memory_access",
                StandardOptions.Option.MODEL_RENDERER_BATCHING, "model_renderer_batching",
                StandardOptions.Option.MODEL_RENDERER_DISPLAY_LISTS, "model_renderer_display_lists",
                StandardOptions.Option.FAST_LIT_ITEM_RENDERING, "fast_lit_item_rendering",
                StandardOptions.Option.FAST_LIT_ITEM_DISPLAY_LISTS, "fast_lit_item_display_lists",
                StandardOptions.Option.ACTINIUM_PRODUCTION_DIAGNOSTICS, "production_diagnostics",
                StandardOptions.Option.ACTINIUM_IGNORE_FRAMEBUFFER_ERRORS, "ignore_framebuffer_errors",
                StandardOptions.Option.ACTINIUM_GL_DEBUG, "gl_debug",
                StandardOptions.Option.ACTINIUM_CLOUD_CONTROL_DEBUG, "cloud_control_debug",
                StandardOptions.Option.ACTINIUM_PERF_DEBUG, "perf_debug",
                StandardOptions.Option.ACTINIUM_GPU_PERF_DEBUG, "gpu_perf_debug",
                StandardOptions.Option.ACTINIUM_FRAME_GL_ERROR_CHECK, "frame_gl_error_check",
                StandardOptions.Option.ACTINIUM_POST_RENDER_GL_ERROR_CHECK, "post_render_gl_error_check",
                StandardOptions.Option.ACTINIUM_REDIRECTOR_DEBUG, "redirector_debug",
                StandardOptions.Option.ACTINIUM_REDIRECTOR_LOG_SPAM, "redirector_log_spam",
                StandardOptions.Option.ACTINIUM_REDIRECTOR_CLASS_DUMP, "redirector_class_dump");
        assertIdentifier(StandardOptions.Option.MULTIDRAW_MODE, "celeritas", "multidraw_mode");
    }

    private static void assertIdentifiers(String namespace, Object... identifiersAndPaths) {
        for (int index = 0; index < identifiersAndPaths.length; index += 2) {
            assertIdentifier((OptionIdentifier<?>) identifiersAndPaths[index], namespace,
                    (String) identifiersAndPaths[index + 1]);
        }
    }

    private static void assertIdentifier(OptionIdentifier<?> identifier, String namespace, String path) {
        assertEquals(namespace, identifier.getModId());
        assertEquals(path, identifier.getPath());
        assertSame(identifier, OptionIdentifier.create(namespace, path, identifier.getType()));
    }
}
