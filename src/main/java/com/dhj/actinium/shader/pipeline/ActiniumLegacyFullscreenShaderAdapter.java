package com.dhj.actinium.shader.pipeline;

import org.embeddedt.embeddium.impl.gl.shader.ShaderType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ActiniumLegacyFullscreenShaderAdapter {
    private static final Pattern VERSION_DIRECTIVE = Pattern.compile("(?m)^\\s*#version\\s+.+$");
    private static final Pattern EXTENSION_DIRECTIVE = Pattern.compile("(?m)^\\s*#extension\\s+.+$");
    private static final Pattern MAIN_DECLARATION = Pattern.compile("\\bvoid\\s+main\\s*\\(\\s*\\)");
    private static final Pattern GL_FRAG_DATA = Pattern.compile("gl_FragData\\s*\\[\\s*(\\d+)\\s*\\]");

    private ActiniumLegacyFullscreenShaderAdapter() {
    }

    public static String translate(ShaderType type, String source) {
        boolean fragmentShader = type == ShaderType.FRAGMENT;

        String translated = stripLeadingDirectives(source);
        translated = MAIN_DECLARATION.matcher(translated).replaceFirst("void actinium_pack_main()");
        translated = translated.replace("attribute ", "in ");
        translated = translated.replace("varying ", fragmentShader ? "in " : "out ");
        translated = translated.replace("texture2D(", "texture(");
        translated = translated.replace("texture2DLod(", "textureLod(");
        translated = translated.replace("shadow2D(", "actinium_shadow2D(");

        if (fragmentShader) {
            translated = replaceFragDataOutputs(translated);
            translated = translated.replace("gl_FragColor", "fragColor0");
        }

        return (fragmentShader ? fragmentPreamble() : vertexPreamble()) + translated;
    }

    private static String stripLeadingDirectives(String source) {
        String stripped = VERSION_DIRECTIVE.matcher(source).replaceFirst("");
        return EXTENSION_DIRECTIVE.matcher(stripped).replaceAll("");
    }

    private static String vertexPreamble() {
        return String.join("\n",
                "#version 330 core",
                "#define MC_VERSION 11202",
                "",
                "out vec4 actinium_TexCoord0;",
                "out vec4 actinium_TexCoord1;",
                "",
                "vec4 actinium_gl_Vertex;",
                "vec4 actinium_gl_MultiTexCoord0;",
                "vec4 actinium_gl_MultiTexCoord1;",
                "vec4 actinium_gl_TexCoord[2];",
                "mat4 actinium_gl_ModelViewMatrix = mat4(1.0);",
                "mat4 actinium_gl_ProjectionMatrix = mat4(1.0);",
                "mat4 actinium_gl_ModelViewProjectionMatrix = mat4(1.0);",
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
                "vec2 actinium_tex_coord_for_vertex(int vertexId) {",
                "    switch (vertexId) {",
                "        case 0: return vec2(0.0, 0.0);",
                "        case 1: return vec2(1.0, 0.0);",
                "        case 2: return vec2(0.0, 1.0);",
                "        default: return vec2(1.0, 1.0);",
                "    }",
                "}",
                "",
                "void main() {",
                "    vec2 texCoord = actinium_tex_coord_for_vertex(gl_VertexID);",
                "    vec2 clipPos = texCoord * 2.0 - 1.0;",
                "    actinium_gl_Vertex = vec4(clipPos, 0.0, 1.0);",
                "    actinium_gl_MultiTexCoord0 = vec4(texCoord, 0.0, 1.0);",
                "    actinium_gl_MultiTexCoord1 = vec4(texCoord, 0.0, 1.0);",
                "    actinium_gl_TexCoord[0] = actinium_gl_MultiTexCoord0;",
                "    actinium_gl_TexCoord[1] = actinium_gl_MultiTexCoord1;",
                "    actinium_pack_main();",
                "    actinium_TexCoord0 = actinium_gl_TexCoord[0];",
                "    actinium_TexCoord1 = actinium_gl_TexCoord[1];",
                "}",
                ""
        );
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
                ""
        );
    }

    private static String replaceFragDataOutputs(String source) {
        Matcher matcher = GL_FRAG_DATA.matcher(source);
        StringBuilder buffer = new StringBuilder(source.length());

        while (matcher.find()) {
            matcher.appendReplacement(buffer, "fragColor" + matcher.group(1));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
