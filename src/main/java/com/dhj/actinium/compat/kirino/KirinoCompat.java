package com.dhj.actinium.compat.kirino;

import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Co-existence support for Kirino Engine (shipped with Cleanroom Loader,
 * mod id {@code kirino_engine}; sub-modules {@code kirino_ecs}/{@code kirino_gl}).
 *
 * <p>Kirino and Actinium are mutually exclusive render owners: in Graphics mode
 * Kirino replaces {@code EntityRenderer#renderWorld} altogether, which starves
 * every Actinium render hook anchored to vanilla {@code renderWorldPass}
 * (iris pipeline, terrain renderer, entity batching, ...). The co-existence path
 * ("path 1") is Kirino's Headless mode: {@code isEnable()} stays {@code true} so
 * Kirino's ECS/analysis runtime initialises, while the render delegate is pinned
 * off so vanilla {@code renderWorld} runs and Actinium owns the pipeline.
 *
 * <p>The pinning itself is done by {@code MixinKirinoConfigHub}
 * ({@code mixins.actinium.kirino.json}, gated on {@code kirino_engine}): wherever
 * Kirino asks {@code isEnableRenderDelegate()} the answer is always
 * {@code false}. This class only reports the resulting co-existence state for
 * diagnostics (logs and the F3 debug screen); it performs no state mutation and
 * references no Kirino class.
 *
 * <p>The {@link Loader} query is deferred to first use instead of the static
 * initialiser: class loading must never fail just because a Forge environment
 * initialisation detail is missing, and nothing here may leak into
 * {@code Actinium}'s lifecycle class.
 */
public final class KirinoCompat {
    /**
     * The mod ID of Kirino Engine (parent module; {@code kirino_ecs}/{@code kirino_gl}
     * declare it as their parent, so all three are present or absent together).
     */
    public static final String MODID = "kirino_engine";

    private static final Logger LOGGER = LogManager.getLogger("ActiniumKirinoCompat");

    private KirinoCompat() {
    }

    /**
     * Returns whether Kirino Engine is installed at runtime.
     */
    public static boolean isKirinoPresent() {
        return Loader.isModLoaded(MODID);
    }

    /**
     * Returns whether the render delegate is pinned off (Kirino Headless mode).
     * When Kirino is absent this is {@code false}: there is nothing to pin.
     */
    public static boolean isHeadlessPinned() {
        return isKirinoPresent();
    }

    /**
     * Installs the co-existence state: when Kirino is present, logs that the
     * render delegate has been pinned off so Actinium remains the sole render
     * pipeline owner. Call once from {@code Actinium#onInit} (after the late
     * mixin config has been queued; the pin itself is enforced by the mixin).
     */
    public static void install() {
        if (!isKirinoPresent()) {
            return;
        }
        LOGGER.info(
            "Kirino Engine detected: pinning render delegate OFF (Headless mode). "
                + "Kirino's ECS/analysis runtime stays active, while Actinium remains the "
                + "sole owner of the rendering pipeline."
        );
    }

    /**
     * Returns the one-line status shown on the F3 debug screen (or {@code null}
     * when Kirino is absent, so the caller can skip the line entirely).
     */
    public static String debugStatus() {
        if (!isKirinoPresent()) {
            return null;
        }
        return "Kirino: headless (render delegate pinned off)";
    }
}
