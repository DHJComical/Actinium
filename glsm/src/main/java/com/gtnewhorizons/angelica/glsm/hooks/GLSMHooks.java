package com.gtnewhorizons.angelica.glsm.hooks;

import com.gtnewhorizons.angelica.glsm.hooks.events.AlphaStateChangeEvent;
import com.gtnewhorizons.angelica.glsm.hooks.events.BlendFuncChangeEvent;
import com.gtnewhorizons.angelica.glsm.hooks.events.FogStateChangeEvent;
import com.gtnewhorizons.angelica.glsm.hooks.events.ForeignDrawEndEvent;
import com.gtnewhorizons.angelica.glsm.hooks.events.LightmapCoordsEvent;
import com.gtnewhorizons.angelica.glsm.hooks.events.LoadingCheckpointEvent;
import com.gtnewhorizons.angelica.glsm.hooks.events.ProgramChangeEvent;
import com.gtnewhorizons.angelica.glsm.hooks.events.ProgramDeleteEvent;
import com.gtnewhorizons.angelica.glsm.hooks.events.ShaderColorChangeEvent;
import com.gtnewhorizons.angelica.glsm.hooks.events.TextureBindEvent;
import com.gtnewhorizons.angelica.glsm.hooks.events.TextureDeleteEvent;
import com.gtnewhorizons.angelica.glsm.hooks.events.TextureUnitStateEvent;
import com.gtnewhorizons.angelica.glsm.hooks.events.VanillaBlendChangeEvent;
import net.minecraftforge.eventbus.api.bus.EventBus;

public final class GLSMHooks {
    public static DeferredBlendHandler blendHandler;
    public static DeferredAlphaHandler alphaHandler;
    public static DeferredDepthColorHandler depthColorHandler;
    /** Optional host-owned observer for diagnostics at the native draw boundary. */
    public static DrawCallObserver drawCallObserver;
    public static ShaderWorkSubmitter shaderWorkSubmitter;
    public static ShaderTransformPostProcessor postTransformProcessor;
    public static PerFrameUniformBlock perFrameUniformBlock;
    public static PerFrameUniformBlock perPassUniformBlock;

    /**
     * Optional host-owned callback fired after a GL_TEXTURE_2D binding cache entry changes on any
     * active texture unit. The host keeps the vanilla {@code GlStateManager.TEXTURES[].textureName}
     * mirrors in sync (mods such as Mobends read it reflectively to restore the previous binding;
     * with GLSM owning the binding updates the vanilla copy would otherwise stay stale at 0 and the
     * restore would unbind the texture, unveiling entities as white).
     */
    public static TextureBindSyncCallback textureBindSyncCallback;

    public interface TextureBindSyncCallback {
        void onTextureBound(int textureUnit, int textureId);
    }

    /** Escape hatch: -Dactinium.glsmHooksAlwaysActive=true forces the consumer gate on. */
    private static final boolean ALWAYS_ACTIVE = Boolean.getBoolean("actinium.glsmHooksAlwaysActive");

    /**
     * Cheap gate for per-call snapshot/event work in GLStateManager hot paths. Written by the
     * pipeline host (Iris PipelineManager): true while a consumer with observable side effects
     * (a DeferredWorldRenderingPipeline) is active, false otherwise. Defaults to true so behavior
     * is unchanged when no host maintains it.
     */
    public static volatile boolean consumersActive = true;

    /** Whether hook consumers may act on events, i.e. snapshots and event posts must run. */
    public static boolean hasActiveConsumers() {
        return ALWAYS_ACTIVE || consumersActive;
    }

    public static final EventBus<TextureBindEvent> TEXTURE_BIND = EventBus.create(TextureBindEvent.class);
    public static final EventBus<TextureDeleteEvent> TEXTURE_DELETE = EventBus.create(TextureDeleteEvent.class);
    public static final EventBus<TextureUnitStateEvent> TEXTURE_UNIT_STATE = EventBus.create(TextureUnitStateEvent.class);
    public static final EventBus<ProgramChangeEvent> PROGRAM_CHANGE = EventBus.create(ProgramChangeEvent.class);
    public static final EventBus<ProgramDeleteEvent> PROGRAM_DELETE = EventBus.create(ProgramDeleteEvent.class);
    public static final EventBus<AlphaStateChangeEvent> ALPHA_STATE_CHANGE = EventBus.create(AlphaStateChangeEvent.class);
    public static final EventBus<ShaderColorChangeEvent> SHADER_COLOR_CHANGE = EventBus.create(ShaderColorChangeEvent.class);
    public static final EventBus<ForeignDrawEndEvent> FOREIGN_DRAW_END = EventBus.create(ForeignDrawEndEvent.class);
    public static final EventBus<VanillaBlendChangeEvent> VANILLA_BLEND_CHANGE = EventBus.create(VanillaBlendChangeEvent.class);
    public static final EventBus<BlendFuncChangeEvent> BLEND_FUNC_CHANGE = EventBus.create(BlendFuncChangeEvent.class);
    public static final EventBus<FogStateChangeEvent> FOG_STATE_CHANGE = EventBus.create(FogStateChangeEvent.class);
    public static final EventBus<LightmapCoordsEvent> LIGHTMAP_COORDS = EventBus.create(LightmapCoordsEvent.class);
    public static final EventBus<LoadingCheckpointEvent> LOADING_CHECKPOINT = EventBus.create(LoadingCheckpointEvent.class);

    public static final TextureBindEvent textureBindEvent = new TextureBindEvent();
    public static final TextureDeleteEvent textureDeleteEvent = new TextureDeleteEvent();
    public static final TextureUnitStateEvent textureUnitStateEvent = new TextureUnitStateEvent();
    public static final ProgramChangeEvent programChangeEvent = new ProgramChangeEvent();
    public static final ProgramDeleteEvent programDeleteEvent = new ProgramDeleteEvent();
    public static final AlphaStateChangeEvent alphaStateChangeEvent = new AlphaStateChangeEvent();
    public static final ShaderColorChangeEvent shaderColorChangeEvent = new ShaderColorChangeEvent();
    public static final ForeignDrawEndEvent foreignDrawEndEvent = new ForeignDrawEndEvent();
    public static final VanillaBlendChangeEvent vanillaBlendChangeEvent = new VanillaBlendChangeEvent();
    public static final BlendFuncChangeEvent blendFuncChangeEvent = new BlendFuncChangeEvent();
    public static final FogStateChangeEvent fogStateChangeEvent = new FogStateChangeEvent();
    public static final LightmapCoordsEvent lightmapCoordsEvent = new LightmapCoordsEvent();
    public static final LoadingCheckpointEvent loadingCheckpointEvent = new LoadingCheckpointEvent();

    private GLSMHooks() {
    }
}
