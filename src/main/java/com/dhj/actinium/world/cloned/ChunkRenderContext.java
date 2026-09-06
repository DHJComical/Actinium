package com.dhj.actinium.world.cloned;

import org.embeddedt.embeddium.impl.util.position.SectionPos;

/**
 * Carries a per-task snapshot from {@code WorldSlice.prepare} to the chunk build task: the origin
 * section coordinates plus one cloned section per slice slot. The array is fully populated by
 * prepare on the main thread and only read afterwards, so it can be shared with the worker thread
 * through the task queue; each pending task owns its own array instance. The snapshot volume is not
 * carried here because it is derived from the origin by the consuming WorldSlice, which reuses a
 * single bounding box across tasks.
 */
public class ChunkRenderContext {
    private final SectionPos sectionCoord;
    private final ClonedChunkSection[] sections;

    public ChunkRenderContext(SectionPos sectionCoord, ClonedChunkSection[] sections) {
        this.sectionCoord = sectionCoord;
        this.sections = sections;
    }

    public ClonedChunkSection[] getSections() {
        return this.sections;
    }

    public SectionPos getOrigin() {
        return this.sectionCoord;
    }
}
