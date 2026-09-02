package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.jetbrains.annotations.Nullable;

/**
 * Legacy object-graph occlusion node — HBM-CE compatibility seam.
 *
 * <p>Upstream Celeritas represented the visibility graph with object nodes of this type until
 * upstream commit {@code 450dc85f} ("Implement a better storage system for occlusion node
 * data") replaced them with {@link SectionLattice}'s dense cell storage, which Actinium
 * follows. HBM-CE's {@code MixinOcclusionCuller} still targets the object-node API: it wraps
 * the {@code OcclusionCuller.isWithinFrustum(Viewport, OcclusionNode)} and
 * {@code isWithinRenderDistance(CameraTransform, OcclusionNode, float)} call sites and reads
 * node origins plus the backing render section from its handlers.</p>
 *
 * <p>This class is never instantiated by Actinium's render path. It exists purely so the
 * third-party mixin finds its injection targets and applies cleanly; without it the critical
 * injections fail, poison {@link OcclusionCuller}, and break world loading with a
 * {@code NoClassDefFoundError} inside the join task (issue #47).</p>
 */
public final class OcclusionNode {
    private final @Nullable RenderSection section;
    private final int originX;
    private final int originY;
    private final int originZ;

    public OcclusionNode(@Nullable RenderSection section, int originX, int originY, int originZ) {
        this.section = section;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
    }

    /**
     * @return the render section this node was derived from, or {@code null} for a
     * position-only node
     */
    public @Nullable RenderSection getRenderSection() {
        return this.section;
    }

    /**
     * @return block-space X coordinate of the section's minimum corner
     */
    public int getOriginX() {
        return this.originX;
    }

    /**
     * @return block-space Y coordinate of the section's minimum corner
     */
    public int getOriginY() {
        return this.originY;
    }

    /**
     * @return block-space Z coordinate of the section's minimum corner
     */
    public int getOriginZ() {
        return this.originZ;
    }
}
