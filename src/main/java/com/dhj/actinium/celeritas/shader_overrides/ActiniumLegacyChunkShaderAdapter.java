package com.dhj.actinium.celeritas.shader_overrides;

import org.embeddedt.embeddium.impl.gl.shader.ShaderType;

import java.util.regex.Pattern;

final class ActiniumLegacyChunkShaderAdapter {
    private static final Pattern VERSION_DIRECTIVE = Pattern.compile("(?m)^\\s*#version\\s+.+$");
    private static final Pattern EXTENSION_DIRECTIVE = Pattern.compile("(?m)^\\s*#extension\\s+.+$");
    private static final Pattern MAIN_DECLARATION = Pattern.compile("\\bvoid\\s+main\\s*\\(\\s*\\)");
    private static final Pattern MC_ENTITY_DECLARATION = Pattern.compile("(?m)^\\s*(attribute|in)\\s+vec4\\s+mc_Entity\\s*;\\s*$");
    private static final Pattern MC_MID_TEX_DECLARATION = Pattern.compile("(?m)^\\s*(attribute|in)\\s+vec2\\s+mc_midTexCoord\\s*;\\s*$");
    private static final Pattern AT_TANGENT_DECLARATION = Pattern.compile("(?m)^\\s*(attribute|in)\\s+vec4\\s+at_tangent\\s*;\\s*$");
    private static final Pattern TEX_DECLARATION = Pattern.compile("(?m)^\\s*uniform\\s+sampler2D\\s+tex\\s*;\\s*$");
    private static final Pattern LIGHTMAP_DECLARATION = Pattern.compile("(?m)^\\s*uniform\\s+sampler2D\\s+lightmap\\s*;\\s*$");
    private static final Pattern SHADOW_CASTING_DEFINE = Pattern.compile("(?m)^\\s*#define\\s+SHADOW_CASTING\\b.*$");
    private static final Pattern FOG_ACTIVE_DEFINE = Pattern.compile("(?m)^\\s*#define\\s+FOG_ACTIVE\\b.*$");
    private static final Pattern GL_FRAG_DATA = Pattern.compile("gl_FragData\\s*\\[\\s*\\d+\\s*\\]");
    private ActiniumLegacyChunkShaderAdapter() {
    }

    public static String translate(ShaderType type, ActiniumTerrainPass pass, String source) {
        boolean fragmentShader = type == ShaderType.FRAGMENT;
        boolean shadowPass = pass == ActiniumTerrainPass.SHADOW || pass == ActiniumTerrainPass.SHADOW_CUTOUT;
        boolean alphaTestPass = pass == ActiniumTerrainPass.GBUFFER_CUTOUT || pass == ActiniumTerrainPass.SHADOW_CUTOUT;

        String translated = stripLeadingDirectives(source);

        if (!shadowPass) {
            translated = SHADOW_CASTING_DEFINE.matcher(translated).replaceAll("// Actinium legacy compat: SHADOW_CASTING disabled");
            translated = FOG_ACTIVE_DEFINE.matcher(translated).replaceAll("// Actinium legacy compat: FOG_ACTIVE disabled");
        }

        translated = MAIN_DECLARATION.matcher(translated).replaceFirst("void actinium_pack_main()");
        translated = MC_ENTITY_DECLARATION.matcher(translated).replaceAll("");
        translated = MC_MID_TEX_DECLARATION.matcher(translated).replaceAll("");
        translated = AT_TANGENT_DECLARATION.matcher(translated).replaceAll("");
        translated = TEX_DECLARATION.matcher(translated).replaceAll("");
        translated = LIGHTMAP_DECLARATION.matcher(translated).replaceAll("");
        translated = translated.replace("attribute ", "in ");
        translated = translated.replace("varying ", fragmentShader ? "in " : "out ");
        translated = translated.replace("mc_Entity", "actinium_mc_Entity");
        translated = translated.replace("mc_midTexCoord", "actinium_mc_midTexCoord");
        translated = translated.replace("texture2D(", "texture(");
        translated = translated.replace("texture2DLod(", "textureLod(");
        translated = translated.replace("shadow2D(", "actinium_shadow2D(");
        translated = GL_FRAG_DATA.matcher(translated).replaceAll("fragColor");
        translated = replaceIdentifier(translated, "tex", "u_BlockTex");
        translated = replaceIdentifier(translated, "lightmap", "u_LightTex");

        return (fragmentShader ? fragmentPreamble(alphaTestPass) : vertexPreamble()) + translated;
    }

    public static String postProcessParsedSource(ActiniumTerrainPass pass, String source) {
        boolean shadowPass = pass == ActiniumTerrainPass.SHADOW || pass == ActiniumTerrainPass.SHADOW_CUTOUT;

        if (shadowPass) {
            return source;
        }

        String processed = SHADOW_CASTING_DEFINE.matcher(source).replaceAll("// Actinium legacy compat: SHADOW_CASTING disabled");
        return FOG_ACTIVE_DEFINE.matcher(processed).replaceAll("// Actinium legacy compat: FOG_ACTIVE disabled");
    }

    private static String stripLeadingDirectives(String source) {
        String stripped = VERSION_DIRECTIVE.matcher(source).replaceFirst("");
        return EXTENSION_DIRECTIVE.matcher(stripped).replaceAll("");
    }

    private static String vertexPreamble() {
        return String.join("\n",
                "#version 330 core",
                "#define MC_VERSION 11202",
                "#define MC_GLSL_VERSION 120",
                "#define IS_IRIS 1",
                "#import <sodium:include/chunk_vertex.glsl>",
                "#import <actinium:include/chunk_vertex_extended.glsl>",
                "",
                "uniform mat4 u_ProjectionMatrix;",
                "uniform mat4 u_ModelViewMatrix;",
                "uniform vec3 u_RegionOffset;",
                "uniform mat3 iris_NormalMatrix;",
                "",
                "out float actinium_FogFragCoord;",
                "",
                "vec4 actinium_gl_Vertex;",
                "vec4 actinium_gl_Color;",
                "vec3 actinium_gl_Normal;",
                "vec4 actinium_gl_MultiTexCoord0;",
                "vec4 actinium_gl_MultiTexCoord1;",
                "vec4 actinium_gl_MultiTexCoord2;",
                "mat3 actinium_gl_NormalMatrix;",
                "mat4 actinium_gl_ProjectionMatrix;",
                "mat4 actinium_gl_ModelViewMatrix;",
                "mat4 actinium_gl_ModelViewProjectionMatrix;",
                "mat4 actinium_gl_TextureMatrix[8] = mat4[8](",
                "    mat4(1.0),",
                "    mat4(0.00390625, 0.0, 0.0, 0.0,",
                "         0.0, 0.00390625, 0.0, 0.0,",
                "         0.0, 0.0, 0.00390625, 0.0,",
                "         0.03125, 0.03125, 0.03125, 1.0),",
                "    mat4(1.0), mat4(1.0), mat4(1.0), mat4(1.0), mat4(1.0), mat4(1.0));",
                "vec4 actinium_mc_Entity;",
                "vec2 actinium_mc_midTexCoord;",
                "",
                "#define gl_Vertex actinium_gl_Vertex",
                "#define gl_Color actinium_gl_Color",
                "#define gl_Normal actinium_gl_Normal",
                "#define gl_MultiTexCoord0 actinium_gl_MultiTexCoord0",
                "#define gl_MultiTexCoord1 actinium_gl_MultiTexCoord1",
                "#define gl_MultiTexCoord2 actinium_gl_MultiTexCoord2",
                "#define gl_NormalMatrix actinium_gl_NormalMatrix",
                "#define gl_ProjectionMatrix actinium_gl_ProjectionMatrix",
                "#define gl_ModelViewMatrix actinium_gl_ModelViewMatrix",
                "#define gl_ModelViewProjectionMatrix actinium_gl_ModelViewProjectionMatrix",
                "#define gl_TextureMatrix actinium_gl_TextureMatrix",
                "#define gl_FogFragCoord actinium_FogFragCoord",
                "#define ftransform() (actinium_gl_ModelViewProjectionMatrix * actinium_gl_Vertex)",
                "",
                "void actinium_pack_main();",
                "",
                "void main() {",
                "    _vert_init();",
                "    actinium_extended_init();",
                "    vec3 actinium_translation = u_RegionOffset + _get_draw_translation(_draw_id);",
                "    vec3 actinium_position = _vert_position + actinium_translation;",
                "    actinium_gl_Vertex = vec4(actinium_position, 1.0);",
                "    actinium_gl_Color = _vert_color;",
                "    actinium_gl_Normal = normalize(actinium_Normal);",
                "    actinium_gl_MultiTexCoord0 = vec4(_vert_tex_diffuse_coord, 0.0, 1.0);",
                "    actinium_gl_MultiTexCoord1 = vec4(vec2(_vert_tex_light_coord), 0.0, 1.0);",
                "    actinium_gl_MultiTexCoord2 = actinium_gl_MultiTexCoord1;",
                "    actinium_gl_NormalMatrix = iris_NormalMatrix;",
                "    actinium_gl_ProjectionMatrix = u_ProjectionMatrix;",
                "    actinium_gl_ModelViewMatrix = u_ModelViewMatrix;",
                "    actinium_gl_ModelViewProjectionMatrix = u_ProjectionMatrix * u_ModelViewMatrix;",
                "    actinium_mc_Entity = vec4(actinium_EntityData.x, actinium_EntityData.y, 0.0, 0.0);",
                "    actinium_mc_midTexCoord = actinium_MidTexCoord;",
                "    actinium_pack_main();",
                "}",
                ""
        );
    }

    private static String fragmentPreamble(boolean alphaTestPass) {
        String alphaTestBlock = alphaTestPass
                ? String.join("\n",
                "void actinium_apply_legacy_alpha_test() {",
                "    if (fragColor.a < 0.1) {",
                "        discard;",
                "    }",
                "}",
                "")
                : String.join("\n",
                "void actinium_apply_legacy_alpha_test() {",
                "}",
                "");

        return String.join("\n",
                "#version 330 core",
                "#define MC_VERSION 11202",
                "#define MC_GLSL_VERSION 120",
                "#define IS_IRIS 1",
                "uniform sampler2D u_BlockTex;",
                "uniform sampler2D u_LightTex;",
                "",
                "in float actinium_FogFragCoord;",
                "",
                "out vec4 fragColor;",
                "",
                "#define gl_FogFragCoord actinium_FogFragCoord",
                "",
                "void actinium_pack_main();",
                alphaTestBlock,
                "",
                "vec4 actinium_shadow2D(sampler2DShadow samplerState, vec3 coord) {",
                "    return vec4(texture(samplerState, coord), 0.0, 0.0, 1.0);",
                "}",
                "",
                "void main() {",
                "    actinium_pack_main();",
                "    actinium_apply_legacy_alpha_test();",
                "}",
                ""
        );
    }

    private static String replaceIdentifier(String source, String oldName, String newName) {
        return source.replaceAll("\\b" + Pattern.quote(oldName) + "\\b", newName);
    }
}
