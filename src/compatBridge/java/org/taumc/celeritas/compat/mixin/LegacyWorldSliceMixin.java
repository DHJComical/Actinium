package org.taumc.celeritas.compat.mixin;

import com.dhj.actinium.world.WorldSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.taumc.celeritas.impl.world.cloned.CeleritasBlockAccess;

/**
 * Adds the legacy block-access descriptor to the live Actinium world slice.
 */
@Mixin(value = WorldSlice.class, remap = false)
public abstract class LegacyWorldSliceMixin implements CeleritasBlockAccess {
}
