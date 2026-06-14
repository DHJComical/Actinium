package com.dhj.actinium.celeritas.buffer;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface BufferBuilderExtension {
    void actinium$setActiveQuadContext(@Nullable VanillaQuadContext context);

    List<VanillaQuadContext> actinium$consumeQuadContexts();

    boolean actinium$isDrawing();

    void actinium$discard();
}
