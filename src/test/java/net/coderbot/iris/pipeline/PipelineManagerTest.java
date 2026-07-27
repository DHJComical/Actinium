package net.coderbot.iris.pipeline;

import net.minecraft.block.Block;
import org.embeddedt.embeddium.api.shader.BlockRenderLayer;
import org.embeddedt.embeddium.api.shader.ShaderProvider;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PipelineManagerTest {
    @Test
    void terrainShaderCleanupDeletesRegisteredProviderShaders() {
        RecordingShaderProvider provider = new RecordingShaderProvider();

        PipelineManager.deleteTerrainShaders(provider);

        assertEquals(1, provider.deleteCount);
    }

    @Test
    void terrainShaderCleanupAcceptsMissingProvider() {
        assertDoesNotThrow(() -> PipelineManager.deleteTerrainShaders(null));
    }

    private static final class RecordingShaderProvider implements ShaderProvider {
        private int deleteCount;

        @Override
        public boolean isShadersEnabled() {
            return false;
        }

        @Override
        public boolean isShadowPass() {
            return false;
        }

        @Override
        public boolean shouldUseFaceCulling() {
            return true;
        }

        @Override
        public GlProgram<? extends ChunkShaderInterface> getShaderOverride(TerrainRenderPass pass) {
            return null;
        }

        @Override
        public ChunkVertexType getVertexType(ChunkVertexType defaultType) {
            return defaultType;
        }

        @Override
        public void setRenderPassConfiguration(RenderPassConfiguration<?> configuration) {
        }

        @Override
        public Map<Block, BlockRenderLayer> getBlockTypeIds() {
            return null;
        }

        @Override
        public void deleteShaders() {
            deleteCount++;
        }
    }
}
