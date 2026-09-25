package dhj.embeddedt.embeddium.impl.render.chunk.vertex.format;

import dhj.embeddedt.embeddium.impl.render.chunk.vertex.format.impl.CompactChunkVertex;
import dhj.embeddedt.embeddium.impl.render.chunk.vertex.format.impl.VanillaLikeChunkVertex;

public class ChunkMeshFormats {
    public static final ChunkVertexType COMPACT = new CompactChunkVertex();
    public static final ChunkVertexType VANILLA_LIKE = new VanillaLikeChunkVertex();
}
