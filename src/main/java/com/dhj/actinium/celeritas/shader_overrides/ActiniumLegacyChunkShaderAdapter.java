package com.dhj.actinium.celeritas.shader_overrides;

import org.embeddedt.embeddium.impl.gl.shader.ShaderType;

import java.util.regex.Matcher;
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
    private static final Pattern GL_FRAG_DATA = Pattern.compile("gl_FragData\\s*\\[\\s*(\\d+)\\s*\\]");
    private ActiniumLegacyChunkShaderAdapter() {
    }

    public static String translate(ShaderType type, ActiniumTerrainPass pass, String source, int terrainDebugMode) {
        boolean fragmentShader = type == ShaderType.FRAGMENT;
        boolean shadowPass = pass == ActiniumTerrainPass.SHADOW || pass == ActiniumTerrainPass.SHADOW_CUTOUT;
        boolean alphaTestPass = pass == ActiniumTerrainPass.GBUFFER_CUTOUT || pass == ActiniumTerrainPass.SHADOW_CUTOUT;
        int debugMode = isWorldTerrainPass(pass) ? terrainDebugMode : 0;

        String translated = stripLeadingDirectives(source);

        if (!shadowPass && !shouldPreserveWorldShadowCasting(pass)) {
            translated = SHADOW_CASTING_DEFINE.matcher(translated).replaceAll("// Actinium legacy compat: SHADOW_CASTING disabled");
        }

        if (!shadowPass) {
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
        translated = replaceIdentifier(translated, "tex", "u_BlockTex");
        translated = replaceIdentifier(translated, "lightmap", "u_LightTex");

        if (fragmentShader) {
            translated = replaceFragDataOutputs(translated);
            translated = translated.replace("gl_FragColor", "fragColor0");
        }

        if (fragmentShader) {
            return fragmentPreamble(alphaTestPass) + translated + fragmentFooter(debugMode);
        }

        return vertexPreamble() + translated;
    }

    public static String postProcessParsedSource(ActiniumTerrainPass pass, String source) {
        boolean shadowPass = pass == ActiniumTerrainPass.SHADOW || pass == ActiniumTerrainPass.SHADOW_CUTOUT;

        if (shadowPass) {
            return source;
        }

        String processed = shouldPreserveWorldShadowCasting(pass)
                ? source
                : SHADOW_CASTING_DEFINE.matcher(source).replaceAll("// Actinium legacy compat: SHADOW_CASTING disabled");
        return FOG_ACTIVE_DEFINE.matcher(processed).replaceAll("// Actinium legacy compat: FOG_ACTIVE disabled");
    }

    private static boolean shouldPreserveWorldShadowCasting(ActiniumTerrainPass pass) {
        return isWorldTerrainPass(pass);
    }

    private static boolean isWorldTerrainPass(ActiniumTerrainPass pass) {
        return pass == ActiniumTerrainPass.GBUFFER_SOLID || pass == ActiniumTerrainPass.GBUFFER_CUTOUT;
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
                "out vec2 actinium_DebugTexCoord;",
                "out vec2 actinium_DebugLightCoord;",
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
                "vec4 actinium_at_tangent;",
                "vec4 actinium_at_midBlock;",
                "vec3 actinium_iris_Normal;",
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
                "#define at_tangent actinium_at_tangent",
                "#define at_midBlock actinium_at_midBlock",
                "#define iris_Normal actinium_iris_Normal",
                "",
                "void actinium_pack_main();",
                "",
                "void main() {",
                "    _vert_init();",
                "    actinium_extended_init();",
                "    vec3 actinium_translation = u_RegionOffset + _get_draw_translation(_draw_id);",
                "    vec3 actinium_position = _vert_position + actinium_translation;",
                "    vec3 actinium_normal = dot(actinium_Normal, actinium_Normal) > 0.0001 ? normalize(actinium_Normal) : vec3(0.0, 1.0, 0.0);",
                "    actinium_gl_Vertex = vec4(actinium_position, 1.0);",
                "    actinium_gl_Color = _vert_color;",
                "    actinium_gl_Normal = actinium_normal;",
                "    actinium_gl_MultiTexCoord0 = vec4(_vert_tex_diffuse_coord, 0.0, 1.0);",
                "    actinium_gl_MultiTexCoord1 = vec4(vec2(_vert_tex_light_coord), 0.0, 1.0);",
                "    actinium_gl_MultiTexCoord2 = actinium_gl_MultiTexCoord1;",
                "    actinium_DebugTexCoord = _vert_tex_diffuse_coord;",
                "    actinium_DebugLightCoord = vec2(_vert_tex_light_coord) * 0.00390625 + vec2(0.03125);",
                "    actinium_gl_NormalMatrix = iris_NormalMatrix;",
                "    actinium_gl_ProjectionMatrix = u_ProjectionMatrix;",
                "    actinium_gl_ModelViewMatrix = u_ModelViewMatrix;",
                "    actinium_gl_ModelViewProjectionMatrix = u_ProjectionMatrix * u_ModelViewMatrix;",
                "    actinium_mc_Entity = vec4(actinium_EntityData.x, actinium_EntityData.y, 0.0, 0.0);",
                "    actinium_mc_midTexCoord = actinium_MidTexCoord;",
                "    actinium_at_tangent = actinium_Tangent;",
                "    actinium_at_midBlock = actinium_MidBlock;",
                "    actinium_iris_Normal = actinium_normal;",
                "    actinium_pack_main();",
                "}",
                ""
        );
    }

    private static String fragmentPreamble(boolean alphaTestPass) {
        String alphaTestBlock = alphaTestPass
                ? String.join("\n",
                "void actinium_apply_legacy_alpha_test() {",
                "    if (fragColor0.a < 0.1) {",
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
                "in vec2 actinium_DebugTexCoord;",
                "in vec2 actinium_DebugLightCoord;",
                "",
                "out vec4 fragColor0;",
                "out vec4 fragColor1;",
                "out vec4 fragColor2;",
                "out vec4 fragColor3;",
                "out vec4 fragColor4;",
                "out vec4 fragColor5;",
                "out vec4 fragColor6;",
                "out vec4 fragColor7;",
                "",
                "#define gl_FogFragCoord actinium_FogFragCoord",
                "#define gl_FragColor fragColor0",
                "",
                "void actinium_pack_main();",
                alphaTestBlock,
                "",
                "vec4 actinium_shadow2D(sampler2DShadow samplerState, vec3 coord) {",
                "    return vec4(texture(samplerState, coord), 0.0, 0.0, 1.0);",
                "}",
                ""
        );
    }

    private static String fragmentFooter(int terrainDebugMode) {
        String debugBlock = switch (terrainDebugMode) {
            case 1 -> "    fragColor0 = vec4(1.0, 0.0, 1.0, 1.0);";
            case 2 -> "    fragColor0 = texture(u_BlockTex, texcoord);";
            case 3 -> "    fragColor0 = vec4(clamp(tintColor.rgb, vec3(0.0), vec3(1.0)), 1.0);";
            case 4 -> String.join("\n",
                    "    #if defined SHADOW_CASTING && !defined NETHER",
                    "    fragColor0 = vec4(vec3(texture(shadowtex1, shadowPos)), 1.0);",
                    "    #else",
                    "    fragColor0 = vec4(0.0, 0.0, 1.0, 1.0);",
                    "    #endif");
            case 5 -> "    fragColor0 = texture(u_BlockTex, actinium_DebugTexCoord);";
            case 6 -> "    fragColor0 = texture(u_LightTex, actinium_DebugLightCoord);";
            case 7 -> String.join("\n",
                    "    #if defined SHADOW_CASTING && !defined NETHER",
                    "    vec2 actiniumShadowTexel = vec2(1.0 / float(shadowMapResolution));",
                    "    vec3 actiniumShadowCoord = vec3(shadowPos.xy, shadowPos.z - 0.0015);",
                    "    float actiniumShadow = 0.0;",
                    "    actiniumShadow += texture(shadowtex1, actiniumShadowCoord + vec3(-actiniumShadowTexel.x, -actiniumShadowTexel.y, 0.0));",
                    "    actiniumShadow += texture(shadowtex1, actiniumShadowCoord + vec3( 0.0, -actiniumShadowTexel.y, 0.0));",
                    "    actiniumShadow += texture(shadowtex1, actiniumShadowCoord + vec3( actiniumShadowTexel.x, -actiniumShadowTexel.y, 0.0));",
                    "    actiniumShadow += texture(shadowtex1, actiniumShadowCoord + vec3(-actiniumShadowTexel.x,  0.0, 0.0));",
                    "    actiniumShadow += texture(shadowtex1, actiniumShadowCoord);",
                    "    actiniumShadow += texture(shadowtex1, actiniumShadowCoord + vec3( actiniumShadowTexel.x,  0.0, 0.0));",
                    "    actiniumShadow += texture(shadowtex1, actiniumShadowCoord + vec3(-actiniumShadowTexel.x,  actiniumShadowTexel.y, 0.0));",
                    "    actiniumShadow += texture(shadowtex1, actiniumShadowCoord + vec3( 0.0,  actiniumShadowTexel.y, 0.0));",
                    "    actiniumShadow += texture(shadowtex1, actiniumShadowCoord + vec3( actiniumShadowTexel.x,  actiniumShadowTexel.y, 0.0));",
                    "    fragColor0 = vec4(vec3(actiniumShadow * 0.11111111), 1.0);",
                    "    #else",
                    "    fragColor0 = vec4(1.0);",
                    "    #endif");
            case 8 -> String.join("\n",
                    "    vec4 actiniumBaseColor = texture(u_BlockTex, texcoord) * tintColor;",
                    "    vec3 actiniumRealLight = omniLight + directLightColor * directLightStrength + candleColor;",
                    "    fragColor0 = vec4(actiniumBaseColor.rgb * max(actiniumRealLight, vec3(0.08)), actiniumBaseColor.a);");
            default -> "";
        };

        if (terrainDebugMode > 0) {
            debugBlock = debugBlock + "\n    fragColor1 = fragColor0;\n    fragColor2 = fragColor0;\n    fragColor3 = fragColor0;\n";
        }

        return String.join("\n",
                "",
                "void main() {",
                "    actinium_pack_main();",
                "    actinium_apply_legacy_alpha_test();",
                debugBlock,
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

    private static String replaceIdentifier(String source, String oldName, String newName) {
        return source.replaceAll("\\b" + Pattern.quote(oldName) + "\\b", newName);
    }
}
