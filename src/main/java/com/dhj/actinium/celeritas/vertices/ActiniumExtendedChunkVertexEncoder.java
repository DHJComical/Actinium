package com.dhj.actinium.celeritas.vertices;

import com.dhj.actinium.block_rendering.ActiniumBlockRenderingSettings;
import com.dhj.actinium.celeritas.ActiniumShaders;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import net.minecraft.block.Block;
import com.dhj.actinium.vertices.ActiniumExtendedDataHelper;
import com.dhj.actinium.vertices.ActiniumNormalHelper;
import com.dhj.actinium.vertices.views.ActiniumQuadView;
import org.embeddedt.embeddium.api.util.NormI8;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.joml.Vector3f;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public class ActiniumExtendedChunkVertexEncoder implements ContextAwareChunkVertexEncoder {
    private static final int MID_TEX_OFFSET = ActiniumExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("mc_midTexCoord").getPointer();
    private static final int TANGENT_OFFSET = ActiniumExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("at_tangent").getPointer();
    private static final int NORMAL_OFFSET = ActiniumExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("iris_Normal").getPointer();
    private static final int MC_ENTITY_OFFSET = ActiniumExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("mc_Entity").getPointer();
    private static final int MID_BLOCK_OFFSET = ActiniumExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("at_midBlock").getPointer();
    private static boolean loggedFluidMapping;

    private final ChunkVertexEncoder baseEncoder = ActiniumExtendedChunkVertexType.BASE_TYPE.createEncoder();
    private final ActiniumQuadView quad = new ActiniumQuadView();
    private final Vector3f normal = new Vector3f();

    private BlockRenderContext context;
    private int vertexCount;
    private float uSum;
    private float vSum;

    @Override
    public void prepareToRenderBlock(BlockRenderContext ctx, Block block, int metadata, short renderType, byte lightValue) {
        this.context = ctx;
        ctx.blockId = ActiniumBlockRenderingSettings.INSTANCE.getBlockStateId(block, metadata);
        ctx.renderType = ActiniumExtendedDataHelper.BLOCK_RENDER_TYPE;
        ctx.lightValue = lightValue;
    }

    @Override
    public void prepareToRenderFluid(BlockRenderContext ctx, Block block, byte lightValue) {
        this.context = ctx;
        ctx.blockId = ActiniumBlockRenderingSettings.INSTANCE.getBlockStateId(block, 0);
        ctx.renderType = ActiniumExtendedDataHelper.FLUID_RENDER_TYPE;
        ctx.lightValue = lightValue;

        if (!loggedFluidMapping && ActiniumShaderPackManager.isDebugEnabled()) {
            loggedFluidMapping = true;
            ActiniumShaders.logger().info("[DEBUG] Fluid shader block mapping: block='{}', shaderBlockId={}, renderType={}",
                    Block.REGISTRY.getNameForObject(block),
                    ctx.blockId,
                    ctx.renderType);
        }
    }

    @Override
    public void prepareToRenderVanilla(BlockRenderContext ctx) {
        this.context = ctx;
    }

    @Override
    public void finishRenderingBlock() {
        if (this.context != null) {
            this.context.reset();
            this.context = null;
        }
        this.vertexCount = 0;
        this.uSum = 0.0f;
        this.vSum = 0.0f;
    }

    @Override
    public long write(long ptr, Material material, Vertex vertex, int sectionIndex) {
        this.uSum += vertex.u;
        this.vSum += vertex.v;
        this.vertexCount++;

        BlockRenderContext ctx = this.context;
        if (ctx == null) {
            ctx = new BlockRenderContext();
        }

        this.baseEncoder.write(ptr, material, vertex, sectionIndex);

        LWJGL.memPutInt(ptr + MC_ENTITY_OFFSET, ((ctx.blockId + 1) << 1) | (ctx.renderType & 1));

        int midBlock = ActiniumExtendedDataHelper.computeMidBlock(vertex.x, vertex.y, vertex.z, ctx.localPosX, ctx.localPosY, ctx.localPosZ);
        LWJGL.memPutInt(ptr + MID_BLOCK_OFFSET, midBlock);
        LWJGL.memPutByte(ptr + MID_BLOCK_OFFSET + 3L, ctx.lightValue);

        if (this.vertexCount == 4) {
            this.vertexCount = 0;

            int midUV = ActiniumExtendedChunkVertexType.encodeMidTexture(this.uSum * 0.25f, this.vSum * 0.25f);
            LWJGL.memPutInt(ptr + MID_TEX_OFFSET, midUV);
            LWJGL.memPutInt(ptr + MID_TEX_OFFSET - ActiniumExtendedChunkVertexType.STRIDE, midUV);
            LWJGL.memPutInt(ptr + MID_TEX_OFFSET - (ActiniumExtendedChunkVertexType.STRIDE * 2L), midUV);
            LWJGL.memPutInt(ptr + MID_TEX_OFFSET - (ActiniumExtendedChunkVertexType.STRIDE * 3L), midUV);

            this.quad.setup(ptr, ActiniumExtendedChunkVertexType.STRIDE);
            ActiniumNormalHelper.computeFaceNormal(this.normal, this.quad);
            int packedNormal = NormI8.pack(this.normal);
            LWJGL.memPutInt(ptr + NORMAL_OFFSET, packedNormal);
            LWJGL.memPutInt(ptr + NORMAL_OFFSET - ActiniumExtendedChunkVertexType.STRIDE, packedNormal);
            LWJGL.memPutInt(ptr + NORMAL_OFFSET - (ActiniumExtendedChunkVertexType.STRIDE * 2L), packedNormal);
            LWJGL.memPutInt(ptr + NORMAL_OFFSET - (ActiniumExtendedChunkVertexType.STRIDE * 3L), packedNormal);

            int tangent = ActiniumNormalHelper.computeTangent(this.normal.x(), this.normal.y(), this.normal.z(), this.quad);
            LWJGL.memPutInt(ptr + TANGENT_OFFSET, tangent);
            LWJGL.memPutInt(ptr + TANGENT_OFFSET - ActiniumExtendedChunkVertexType.STRIDE, tangent);
            LWJGL.memPutInt(ptr + TANGENT_OFFSET - (ActiniumExtendedChunkVertexType.STRIDE * 2L), tangent);
            LWJGL.memPutInt(ptr + TANGENT_OFFSET - (ActiniumExtendedChunkVertexType.STRIDE * 3L), tangent);

            this.uSum = 0.0f;
            this.vSum = 0.0f;
        }

        return ptr + ActiniumExtendedChunkVertexType.STRIDE;
    }
}
