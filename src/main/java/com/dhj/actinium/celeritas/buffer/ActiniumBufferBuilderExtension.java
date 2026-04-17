package com.dhj.actinium.celeritas.buffer;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ActiniumBufferBuilderExtension {
    void actinium$setActiveQuadContext(@Nullable ActiniumVanillaQuadContext context);

    List<ActiniumVanillaQuadContext> actinium$consumeQuadContexts();
}
