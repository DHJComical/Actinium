package com.gtnewhorizons.angelica.glsm.hooks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class GLSMHooks {
    public static DeferredBlendHandler blendHandler;
    public static DeferredAlphaHandler alphaHandler;
    public static DeferredDepthColorHandler depthColorHandler;

    public static final Event<BlendFuncChangeEvent> BLEND_FUNC_CHANGE = new Event<>();
    public static final Event<FogStateChangeEvent> FOG_STATE_CHANGE = new Event<>();
    public static final Event<TextureBindEvent> TEXTURE_BIND = new Event<>();
    public static final Event<TextureDeleteEvent> TEXTURE_DELETE = new Event<>();
    public static final Event<TextureUnitStateEvent> TEXTURE_UNIT_STATE = new Event<>();
    public static final Event<ProgramChangeEvent> PROGRAM_CHANGE = new Event<>();

    private GLSMHooks() {
    }

    public static final class Event<T> {
        private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

        public void addListener(Consumer<T> listener) {
            listeners.add(listener);
        }

        public void fire(T event) {
            for (Consumer<T> listener : listeners) {
                listener.accept(event);
            }
        }
    }

    public record BlendFuncChangeEvent(boolean enabled, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
    }

    public record FogStateChangeEvent(int mode, float density, float start, float end) {
    }

    public record TextureBindEvent(int unit, int target, int textureId) {
    }

    public record TextureDeleteEvent(int textureId) {
    }

    public record TextureUnitStateEvent(int unit, int target, boolean enabled) {
    }

    public record ProgramChangeEvent(int previousProgram, int newProgram) {
    }
}
