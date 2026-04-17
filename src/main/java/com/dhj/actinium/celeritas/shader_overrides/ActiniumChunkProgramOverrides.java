package com.dhj.actinium.celeritas.shader_overrides;

import com.dhj.actinium.celeritas.ActiniumShaders;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.gl.shader.GlShader;
import org.embeddedt.embeddium.impl.gl.shader.ShaderConstants;
import org.embeddedt.embeddium.impl.gl.shader.ShaderParser;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderBindingPoints;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderComponent;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderOptions;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.shader.ShaderLoader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public final class ActiniumChunkProgramOverrides {
    private static final Pattern VERSION_DIRECTIVE = Pattern.compile("^#version.*$", Pattern.MULTILINE);
    private static final Pattern IN_PARAM = Pattern.compile("^in ", Pattern.MULTILINE);
    private static final Pattern OUT_PARAM = Pattern.compile("^out ", Pattern.MULTILINE);
    private static final String LEGACY_PREAMBLE = String.join("\n",
            "#version 120",
            "#extension GL_EXT_gpu_shader4 : require",
            "#define LEGACY",
            "#define uint unsigned int",
            "#define texture texture2D"
    ) + "\n";

    private static final List<ChunkShaderComponent.Factory<?>> COMPONENTS = List.of(ChunkShaderFogComponent.FOG_SERVICE.getFogMode());

    private final EnumMap<ActiniumTerrainPass, @Nullable GlProgram<ChunkShaderInterface>> programs = new EnumMap<>(ActiniumTerrainPass.class);
    private final boolean enableLegacyGlPatches = !LWJGL.isOpenGLVersionSupported(3, 2);

    public @Nullable GlProgram<? extends ChunkShaderInterface> getProgramOverride(TerrainRenderPass pass,
                                                                                   RenderPassConfiguration<?> configuration,
                                                                                   boolean shadowPass) {
        ActiniumTerrainPass terrainPass = ActiniumTerrainPass.fromTerrainPass(pass, shadowPass);
        GlProgram<ChunkShaderInterface> program = this.programs.get(terrainPass);

        if (program == null && !this.programs.containsKey(terrainPass)) {
            try {
                program = this.createShader(terrainPass, configuration);
            } catch (Exception e) {
                ActiniumShaders.logger().error("Failed to create Actinium terrain override program for {}", terrainPass.name(), e);
            }

            this.programs.put(terrainPass, program);
        }

        return program;
    }

    public void deleteShaders() {
        for (GlProgram<ChunkShaderInterface> program : this.programs.values()) {
            if (program != null) {
                program.delete();
            }
        }

        this.programs.clear();
    }

    private GlProgram<ChunkShaderInterface> createShader(ActiniumTerrainPass pass, RenderPassConfiguration<?> configuration) {
        TerrainRenderPass renderPass = pass.toTerrainPass(configuration);
        ChunkShaderOptions options = new ChunkShaderOptions(COMPONENTS, renderPass);
        ShaderConstants constants = options.constants();
        List<GlShader> loadedShaders = new ArrayList<>(2);

        loadedShaders.add(this.loadShader(ShaderType.VERTEX, "actinium:blocks/chunk_layer_override.vsh", constants));
        loadedShaders.add(this.loadShader(ShaderType.FRAGMENT, "actinium:blocks/chunk_layer_override.fsh", constants));

        try {
            GlProgram.Builder builder = GlProgram.builder("actinium:chunk_shader_" + pass.name().toLowerCase(Locale.ROOT));
            loadedShaders.forEach(builder::attachShader);
            int attributeIndex = 0;

            for (var attribute : configuration.getVertexTypeForPass(renderPass).getVertexFormat().getAttributes()) {
                builder.bindAttribute(attribute.getName(), attributeIndex++);
            }

            if (!this.enableLegacyGlPatches) {
                builder.bindFragmentData("fragColor", ChunkShaderBindingPoints.FRAG_COLOR);
            }

            return builder.link(shader -> new ActiniumChunkShaderInterface(shader, options));
        } finally {
            loadedShaders.forEach(GlShader::delete);
        }
    }

    private GlShader loadShader(ShaderType type, String path, ShaderConstants constants) {
        String shaderSource = ShaderParser.parseShader(this.resolveShaderSource(path), this::resolveShaderSource, constants);

        if (this.enableLegacyGlPatches) {
            if (type != ShaderType.VERTEX && type != ShaderType.FRAGMENT) {
                throw new IllegalStateException("Cannot load non-vertex or non-fragment shaders on legacy GL");
            }

            shaderSource = VERSION_DIRECTIVE.matcher(shaderSource).replaceFirst(LEGACY_PREAMBLE);

            if (type == ShaderType.VERTEX) {
                shaderSource = IN_PARAM.matcher(shaderSource).replaceAll("attribute ");
            } else {
                shaderSource = IN_PARAM.matcher(shaderSource).replaceAll("varying ");
            }

            shaderSource = OUT_PARAM.matcher(shaderSource).replaceAll("varying ");
        }

        return new GlShader(type, path, shaderSource);
    }

    private String resolveShaderSource(String path) {
        String activeShaderSource = ActiniumShaderPackManager.getShaderSource(path);

        if (activeShaderSource != null) {
            return activeShaderSource;
        }

        return ShaderLoader.getShaderSource(path);
    }
}
