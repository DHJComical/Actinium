package com.dhj.actinium.shader.pipeline;

import com.dhj.actinium.shader.transform.ActiniumGlslTransformUtils;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.Transformer;

import java.util.HashSet;
import java.util.regex.Pattern;

final class ActiniumLegacyFullscreenShaderAdapter {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?m)^\\s*#version\\s+.+$");

    private ActiniumLegacyFullscreenShaderAdapter() {
    }

    public static String translate(ShaderType type, String source) {
        boolean fragmentShader = type == ShaderType.FRAGMENT;

        String parseableSource = ActiniumGlslTransformUtils.replaceTexture(source);
        ShaderParser.ParsedShader parsed = ShaderParser.parseShader(parseableSource);
        Transformer transformer = new Transformer(parsed.full());
        transformer.renameFunctionCall(ActiniumGlslTransformUtils.TEXTURE_RENAMES);
        transformer.renameFunctionCall("shadow2D", "actinium_shadow2D");
        transformer.rename("gl_FragColor", "fragColor0");
        transformer.renameArray("gl_FragData", "fragColor", new HashSet<>());

        String pre = VERSION_PATTERN.matcher(ActiniumGlslTransformUtils.getFormattedShader(parsed.pre(), "")).replaceFirst("").trim();
        String header = (fragmentShader ? fragmentPreamble() : vertexPreamble()) + (pre.isEmpty() ? "" : pre + "\n");
        String translated = ActiniumGlslTransformUtils.getFormattedShader(parsed.full(), header);
        return ActiniumGlslTransformUtils.restoreReservedWords(translated);
    }

    private static String vertexPreamble() {
        return String.join("\n",
                "#version 330 core",
                "#define MC_VERSION 11202",
                "",
                "out vec4 actinium_TexCoord0;",
                "out vec4 actinium_TexCoord1;",
                "",
                "layout(location = 0) in vec3 actinium_aPosition;",
                "layout(location = 2) in vec2 actinium_aTexCoord0;",
                "",
                "vec4 actinium_gl_Vertex;",
                "vec4 actinium_gl_MultiTexCoord0;",
                "vec4 actinium_gl_MultiTexCoord1;",
                "vec4 actinium_gl_TexCoord[2];",
                "mat4 actinium_gl_ModelViewMatrix = mat4(1.0);",
                "mat4 actinium_gl_ProjectionMatrix = mat4(",
                "    2.0, 0.0, 0.0, 0.0,",
                "    0.0, 2.0, 0.0, 0.0,",
                "    0.0, 0.0, -2.0, 0.0,",
                "   -1.0, -1.0, -1.0, 1.0",
                ");",
                "mat4 actinium_gl_ModelViewProjectionMatrix = actinium_gl_ProjectionMatrix * actinium_gl_ModelViewMatrix;",
                "mat4 actinium_gl_TextureMatrix[2] = mat4[2](mat4(1.0), mat4(1.0));",
                "",
                "#define gl_Vertex actinium_gl_Vertex",
                "#define gl_MultiTexCoord0 actinium_gl_MultiTexCoord0",
                "#define gl_MultiTexCoord1 actinium_gl_MultiTexCoord1",
                "#define gl_TexCoord actinium_gl_TexCoord",
                "#define gl_ModelViewMatrix actinium_gl_ModelViewMatrix",
                "#define gl_ProjectionMatrix actinium_gl_ProjectionMatrix",
                "#define gl_ModelViewProjectionMatrix actinium_gl_ModelViewProjectionMatrix",
                "#define gl_TextureMatrix actinium_gl_TextureMatrix",
                "#define ftransform() (actinium_gl_ModelViewProjectionMatrix * actinium_gl_Vertex)",
                "",
                "void actinium_pack_main();",
                "",
                "void main() {",
                "    actinium_gl_Vertex = vec4(actinium_aPosition, 1.0);",
                "    actinium_gl_MultiTexCoord0 = vec4(actinium_aTexCoord0, 0.0, 1.0);",
                "    actinium_gl_MultiTexCoord1 = vec4(actinium_aTexCoord0, 0.0, 1.0);",
                "    actinium_gl_TexCoord[0] = actinium_gl_MultiTexCoord0;",
                "    actinium_gl_TexCoord[1] = actinium_gl_MultiTexCoord1;",
                "    actinium_pack_main();",
                "    actinium_TexCoord0 = actinium_gl_TexCoord[0];",
                "    actinium_TexCoord1 = actinium_gl_TexCoord[1];",
                "}",
                "");
    }

    private static String fragmentPreamble() {
        String outputs = String.join("\n",
                "out vec4 fragColor0;",
                "out vec4 fragColor1;",
                "out vec4 fragColor2;",
                "out vec4 fragColor3;"
        );

        return String.join("\n",
                "#version 330 core",
                "#define MC_VERSION 11202",
                "",
                "in vec4 actinium_TexCoord0;",
                "in vec4 actinium_TexCoord1;",
                "",
                outputs,
                "",
                "vec4 actinium_gl_TexCoord[2];",
                "",
                "#define gl_TexCoord actinium_gl_TexCoord",
                "#define gl_FragColor fragColor0",
                "",
                "void actinium_pack_main();",
                "",
                "vec4 actinium_shadow2D(sampler2DShadow samplerState, vec3 coord) {",
                "    return vec4(texture(samplerState, coord), 0.0, 0.0, 1.0);",
                "}",
                "",
                "void main() {",
                "    actinium_gl_TexCoord[0] = actinium_TexCoord0;",
                "    actinium_gl_TexCoord[1] = actinium_TexCoord1;",
                "    actinium_pack_main();",
                "}",
                "");
    }
}
