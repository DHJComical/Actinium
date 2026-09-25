package dhj.embeddedt.embeddium.api.shader;

import net.minecraft.block.Block;
import dhj.embeddedt.embeddium.impl.gl.shader.GlProgram;
import dhj.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import dhj.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import dhj.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import dhj.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface ShaderProvider {
    boolean isShadersEnabled();

    boolean isShadowPass();

    boolean shouldUseFaceCulling();

    @Nullable
    GlProgram<? extends ChunkShaderInterface> getShaderOverride(TerrainRenderPass pass);

    ChunkVertexType getVertexType(ChunkVertexType defaultType);

    void setRenderPassConfiguration(RenderPassConfiguration<?> configuration);

    default int getBlockStateId(Block block, int metadata) {
        return Block.getIdFromBlock(block);
    }

    @Nullable
    Map<Block, BlockRenderLayer> getBlockTypeIds();

    default void deleteShaders() {
    }
}
