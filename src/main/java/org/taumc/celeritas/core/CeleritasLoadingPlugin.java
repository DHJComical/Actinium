package org.taumc.celeritas.core;

/**
 * Presence marker that makes third-party "is a Celeritas-family pipeline installed?" probes
 * resolve to true.
 *
 * <p>Actinium succeeds the Celeritas render pipeline and deliberately ships neither the
 * {@code celeritas} mod id nor the {@code org.taumc.celeritas} API surface; Celeritas-family
 * addons bind to Actinium's implementation directly. Some optimiser mods, however, do not ask
 * the mod loader at all: they decide whether their own rendering tweaks may run by probing for
 * the <em>existence</em> of this class with {@code Class.forName}. CensoredASM / Chibi (LoliASM)
 * is one of them — {@code LoliTransformer} derives {@code isCeleritasInstalled} from this exact
 * binary name and then keeps its on-demand animated textures and its BakedQuad squasher
 * disabled. Both features otherwise collide with Actinium on the same vanilla classes
 * ({@code TextureMap.updateAnimations} and {@code BufferBuilder.tex} are overwritten twice, and
 * the squasher rewrites the {@code BakedQuad} fields Actinium shadows), so letting LoliASM take
 * its own documented Celeritas path is the intended coexistence mode.
 *
 * <p>The type is intentionally memberless and must never be used as an API entry point: it
 * exists only so that the probe succeeds. Nothing in Actinium or in an addon may reference it
 * as a loading plugin, and no behaviour is attached to it.
 */
public final class CeleritasLoadingPlugin {
    private CeleritasLoadingPlugin() {
    }
}
