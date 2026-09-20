package dhj.embeddedt.embeddium.impl.render.chunk.data;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import dhj.embeddedt.embeddium.impl.gl.util.VertexRange;
import dhj.embeddedt.embeddium.impl.common.util.NativeBuffer;
import dhj.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import dhj.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentQuadAnalyzer;
import dhj.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record BuiltSectionMeshParts(NativeBuffer vertexBuffer, @Nullable NativeBuffer indexBuffer, @Nullable TranslucentQuadAnalyzer.SortState sortState, Map<ModelQuadFacing, VertexRange> ranges) {
    public void free() {
        vertexBuffer.free();
        if (indexBuffer != null) {
            indexBuffer.free();
        }
    }

    public static Reference2ReferenceMap<TerrainRenderPass, BuiltSectionMeshParts> groupFromBuildBuffers(ChunkBuildBuffers buffers, float relativeCameraX, float relativeCameraY, float relativeCameraZ) {
        Reference2ReferenceMap<TerrainRenderPass, BuiltSectionMeshParts> meshes = new Reference2ReferenceOpenHashMap<>();

        for (TerrainRenderPass pass : buffers.getBuilderPasses()) {
            BuiltSectionMeshParts mesh = buffers.createMesh(pass, relativeCameraX, relativeCameraY, relativeCameraZ);

            if (mesh != null) {
                meshes.put(pass, mesh);
            }
        }

        return meshes;
    }
}
