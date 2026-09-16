package com.dhj.actinium.render.entity;

import net.minecraft.world.chunk.Chunk;

/**
 * Source of the chunks that {@link EntityGatherer} should scan for renderable entities.
 *
 * <p>The gatherer needs to know which chunks can contribute an entity to the current frame, but
 * that answer depends on world, render distance and view state. Keeping the choice of chunks
 * behind this seam lets the gatherer own only the pass-splitting logic, while the caller keeps
 * the world access: the renderer supplies the visible, render-distance scoped iteration, and a
 * caller without a live client world can supply any other chunk set.</p>
 */
public interface EntitySource {
    /**
     * Returns the chunks to scan for entities that should be rendered in one of the render passes.
     *
     * <p>The result is consumed once per frame, so an implementation must not materialise a key
     * set or a copy of the underlying chunk collection merely to answer this call; a lazy view
     * over the provider's loaded-chunk map is the intended implementation. Chunks that currently
     * hold no entities may be included, because the gatherer rejects those cheaply through
     * {@code hasEntities}.</p>
     *
     * @return the chunks to scan, in no particular order
     */
    Iterable<Chunk> entityChunks();
}
