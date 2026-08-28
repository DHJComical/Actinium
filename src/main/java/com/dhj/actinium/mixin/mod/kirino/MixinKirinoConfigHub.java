package com.dhj.actinium.mixin.mod.kirino;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Headless co-existence bridge for Kirino Engine's {@code KirinoConfigHub}.
 *
 * <p>Kirino Engine (shipped with Cleanroom Loader, mod ids
 * {@code kirino_engine}/{@code kirino_ecs}/{@code kirino_gl}) has two mutually
 * exclusive render runs: in Graphics mode it replaces
 * {@code EntityRenderer#renderWorld} entirely (its own {@code EntityRenderer$renderWorld}),
 * which starves every Actinium render hook that anchors to vanilla
 * {@code renderWorldPass} (iris pipeline, terrain renderer, entity batching, ...).
 * Its {@code enableRenderDelegate} toggle is initialised to {@code true} and re-forced
 * to {@code true} in {@code KirinoCommonCore#onKirinoOneTimeConfig}, and it has no user
 * facing configuration writer, so the switch can never be turned off by a user.
 *
 * <p>Actinium therefore pins the toggle at its read points: wherever Kirino asks
 * {@code isEnableRenderDelegate()}, this mixin always answers {@code false}, which makes
 * Kirino run in Headless mode ("path 1"): the engine and its ECS/analysis runtime are
 * initialised ({@code isEnable()} is untouched, so no GL resources are allocated and
 * vanilla {@code renderWorld} keeps running), while Actinium stays the single owner of
 * the rendering pipeline.
 *
 * <p>Injected via a late/conditional mixin config gated on the {@code kirino_engine}
 * mod id (see {@code MixinLate} and {@code mixins.actinium.kirino.json}); the config is
 * never queued when Kirino is absent, and the target class is referenced by string so
 * no compile-time dependency on Kirino exists.
 */
@Mixin(targets = "com.cleanroommc.kirino.config.KirinoConfigHub", remap = false)
public abstract class MixinKirinoConfigHub {

    /**
     * @author Actinium
     * @reason Force Kirino into Headless mode so vanilla {@code renderWorld} (and therefore
     * the Actinium render pipeline) stays in charge; {@code KIRINO_CONFIG_HUB.enable} remains
     * untouched so Kirino's ECS/analysis runtime still initialises.
     */
    @Overwrite
    public boolean isEnableRenderDelegate() {
        return false;
    }
}
