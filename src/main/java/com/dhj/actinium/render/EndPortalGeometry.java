package com.dhj.actinium.render;

import net.minecraft.util.EnumFacing;

import java.util.EnumMap;
import java.util.List;

/**
 * Emits complete portal vertex attributes from visible projective geometry.
 */
final class EndPortalGeometry {
    private static final float ALPHA = 1.0F;
    private static final int FULL_BRIGHT = 240;

    private EndPortalGeometry() {
    }

    /**
     * Emits the original face quads with homogeneous texture coordinates for fixed-function rendering.
     *
     * @param faces visible face quads
     * @param projection active draw projection
     * @param renderX render-relative block X
     * @param renderY render-relative block Y
     * @param renderZ render-relative block Z
     * @param layer current portal layer
     * @param sink homogeneous vertex destination
     */
    static void emitHomogeneous(
        List<EndPortalMesh.FaceQuad> faces,
        EndPortalProjection projection,
        double renderX,
        double renderY,
        double renderZ,
        EndPortalLayers.Layer layer,
        ProjectiveVertexSink sink
    ) {
        for (EndPortalMesh.FaceQuad face : faces) {
            projectiveVertex(sink, projection, layer, face.direction(), renderX, renderY, renderZ, face.first());
            projectiveVertex(sink, projection, layer, face.direction(), renderX, renderY, renderZ, face.second());
            projectiveVertex(sink, projection, layer, face.direction(), renderX, renderY, renderZ, face.third());
            projectiveVertex(sink, projection, layer, face.direction(), renderX, renderY, renderZ, face.fourth());
        }
    }

    /**
     * Emits the clipped adaptive shader mesh with CPU-divided texture coordinates.
     *
     * @param triangles shared shader topology
     * @param layer current portal layer
     * @param renderX render-relative block X
     * @param renderY render-relative block Y
     * @param renderZ render-relative block Z
     * @param sink divided vertex destination
     */
    static void emitDivided(
        List<EndPortalMesh.Triangle> triangles,
        EndPortalLayers.Layer layer,
        double renderX,
        double renderY,
        double renderZ,
        VertexSink sink
    ) {
        EnumMap<EnumFacing, UvRebase> rebases = new EnumMap<>(EnumFacing.class);
        for (EndPortalMesh.Triangle triangle : triangles) {
            UvRebase rebase = rebases.computeIfAbsent(
                triangle.face(),
                face -> rebase(layer, triangle.first().clip())
            );
            dividedVertex(sink, layer, triangle.face(), triangle.first(), renderX, renderY, renderZ, rebase);
            dividedVertex(sink, layer, triangle.face(), triangle.second(), renderX, renderY, renderZ, rebase);
            dividedVertex(sink, layer, triangle.face(), triangle.third(), renderX, renderY, renderZ, rebase);
        }
    }

    private static UvRebase rebase(EndPortalLayers.Layer layer, EndPortalProjection.ClipPosition clip) {
        EndPortalProjection.ProjectiveTexCoord texture = layer.projective(clip);
        return new UvRebase(Math.floor(texture.dividedU()), Math.floor(texture.dividedV()));
    }

    private static void projectiveVertex(
        ProjectiveVertexSink sink,
        EndPortalProjection projection,
        EndPortalLayers.Layer layer,
        EnumFacing face,
        double renderX,
        double renderY,
        double renderZ,
        EndPortalMesh.LocalPosition local
    ) {
        double x = renderX + local.x();
        double y = renderY + local.y();
        double z = renderZ + local.z();
        EndPortalProjection.ProjectiveTexCoord texture = layer.projective(projection.clip(x, y, z));
        sink.vertex(
            face,
            x,
            y,
            z,
            (float) texture.s(),
            (float) texture.t(),
            (float) texture.r(),
            (float) texture.q(),
            layer.red(),
            layer.green(),
            layer.blue(),
            ALPHA,
            FULL_BRIGHT,
            FULL_BRIGHT,
            face.getXOffset(),
            face.getYOffset(),
            face.getZOffset()
        );
    }

    private static void dividedVertex(
        VertexSink sink,
        EndPortalLayers.Layer layer,
        EnumFacing face,
        EndPortalMesh.MeshVertex vertex,
        double renderX,
        double renderY,
        double renderZ,
        UvRebase rebase
    ) {
        EndPortalMesh.LocalPosition local = vertex.local();
        EndPortalProjection.ProjectiveTexCoord texture = layer.projective(vertex.clip());
        sink.vertex(
            face,
            renderX + local.x(),
            renderY + local.y(),
            renderZ + local.z(),
            (float) (texture.dividedU() - rebase.u()),
            (float) (texture.dividedV() - rebase.v()),
            layer.red(),
            layer.green(),
            layer.blue(),
            ALPHA,
            FULL_BRIGHT,
            FULL_BRIGHT,
            face.getXOffset(),
            face.getYOffset(),
            face.getZOffset()
        );
    }

    private record UvRebase(double u, double v) {
    }

    /** Receives one complete ordinary UV2 portal vertex. */
    @FunctionalInterface
    interface VertexSink {
        /** Writes one divided portal vertex. */
        void vertex(
            EnumFacing face,
            double x,
            double y,
            double z,
            float u,
            float v,
            float red,
            float green,
            float blue,
            float alpha,
            int lightU,
            int lightV,
            float normalX,
            float normalY,
            float normalZ
        );
    }

    /** Receives one complete homogeneous UV4 portal vertex. */
    @FunctionalInterface
    interface ProjectiveVertexSink {
        /** Writes one projective portal vertex. */
        void vertex(
            EnumFacing face,
            double x,
            double y,
            double z,
            float s,
            float t,
            float r,
            float q,
            float red,
            float green,
            float blue,
            float alpha,
            int lightU,
            int lightV,
            float normalX,
            float normalY,
            float normalZ
        );
    }
}
