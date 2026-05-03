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
    private static final Pattern GL_FRAG_DATA = Pattern.compile("gl_FragData\\s*\\[\\s*(\\d+)\\s*\\]");
    private ActiniumLegacyChunkShaderAdapter() {
    }

    public static String translate(ShaderType type, ActiniumTerrainPass pass, String source, int terrainDebugMode) {
        boolean fragmentShader = type == ShaderType.FRAGMENT;
        boolean shadowPass = pass == ActiniumTerrainPass.SHADOW || pass == ActiniumTerrainPass.SHADOW_CUTOUT;
        boolean alphaTestPass = pass == ActiniumTerrainPass.GBUFFER_CUTOUT || pass == ActiniumTerrainPass.GBUFFER_CUTOUT_MIPPED
                || pass == ActiniumTerrainPass.SHADOW_CUTOUT;
        int debugMode = isWorldTerrainPass(pass) ? terrainDebugMode : 0;

        String translated = stripLeadingDirectives(source);

        if (!shadowPass && !shouldPreserveWorldShadowCasting(pass)) {
            translated = SHADOW_CASTING_DEFINE.matcher(translated).replaceAll("// Actinium legacy compat: SHADOW_CASTING disabled");
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

        return vertexPreamble(shadowPass) + translated + vertexFooter(shadowPass);
    }

    public static String postProcessParsedSource(ActiniumTerrainPass pass, String source) {
        boolean shadowPass = pass == ActiniumTerrainPass.SHADOW || pass == ActiniumTerrainPass.SHADOW_CUTOUT;

        if (shadowPass) {
            return source;
        }

        return shouldPreserveWorldShadowCasting(pass)
                ? source
                : SHADOW_CASTING_DEFINE.matcher(source).replaceAll("// Actinium legacy compat: SHADOW_CASTING disabled");
    }

    private static boolean shouldPreserveWorldShadowCasting(ActiniumTerrainPass pass) {
        return isWorldTerrainPass(pass);
    }

    private static boolean isWorldTerrainPass(ActiniumTerrainPass pass) {
        return pass == ActiniumTerrainPass.GBUFFER_SOLID
                || pass == ActiniumTerrainPass.GBUFFER_CUTOUT_MIPPED
                || pass == ActiniumTerrainPass.GBUFFER_CUTOUT;
    }

    private static String stripLeadingDirectives(String source) {
        String stripped = VERSION_DIRECTIVE.matcher(source).replaceFirst("");
        return EXTENSION_DIRECTIVE.matcher(stripped).replaceAll("");
    }

    private static String vertexPreamble(boolean shadowPass) {
        String shadowCompatUniforms = shadowPass
                ? String.join("\n",
                "uniform vec3 actinium_ShadowCompatCameraPosition;",
                "uniform float actinium_ShadowCompatFrameTimeCounter;",
                "uniform float actinium_ShadowCompatRainStrength;",
                "")
                : "";
        String shadowCompatPrototype = shadowPass ? "vec4 actinium_postprocess_shadow_position(vec4 currentPosition);" : "";
        String shadowCompatCall = shadowPass ? "    gl_Position = actinium_postprocess_shadow_position(gl_Position);" : "";

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
                "uniform float iris_FogDensity;",
                "uniform float iris_FogStart;",
                "uniform float iris_FogEnd;",
                "uniform vec4 iris_FogColor;",
                shadowCompatUniforms,
                "",
                "out float actinium_FogFragCoord;",
                "out vec2 actinium_DebugTexCoord;",
                "out vec2 actinium_DebugLightCoord;",
                "",
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
                "struct actinium_FogParameters {",
                "    vec4 color;",
                "    float density;",
                "    float start;",
                "    float end;",
                "    float scale;",
                "};",
                "actinium_FogParameters actinium_gl_Fog = actinium_FogParameters(",
                "    iris_FogColor,",
                "    iris_FogDensity,",
                "    iris_FogStart,",
                "    iris_FogEnd,",
                "    1.0 / max(iris_FogEnd - iris_FogStart, 0.0001)",
                ");",
                "",
                "vec4 _actinium_getVertexPosition() {",
                "    return vec4(_vert_position + u_RegionOffset + _get_draw_translation(_draw_id), 1.0);",
                "}",
                "",
                "vec4 _actinium_getLightTexCoord() {",
                "    return vec4(vec2(_vert_tex_light_coord), 0.0, 1.0);",
                "}",
                "",
                "vec4 actinium_ftransform() {",
                "    return actinium_gl_ModelViewProjectionMatrix * _actinium_getVertexPosition();",
                "}",
                "",
                "#define gl_Vertex _actinium_getVertexPosition()",
                "#define gl_Color _vert_color",
                "#define gl_Normal actinium_iris_Normal",
                "#define gl_MultiTexCoord0 vec4(_vert_tex_diffuse_coord, 0.0, 1.0)",
                "#define gl_MultiTexCoord1 _actinium_getLightTexCoord()",
                "#define gl_MultiTexCoord2 _actinium_getLightTexCoord()",
                "#define gl_NormalMatrix actinium_gl_NormalMatrix",
                "#define gl_ProjectionMatrix actinium_gl_ProjectionMatrix",
                "#define gl_ModelViewMatrix actinium_gl_ModelViewMatrix",
                "#define gl_ModelViewProjectionMatrix actinium_gl_ModelViewProjectionMatrix",
                "#define gl_TextureMatrix actinium_gl_TextureMatrix",
                "#define gl_Fog actinium_gl_Fog",
                "#define gl_FogFragCoord actinium_FogFragCoord",
                "#define ftransform() actinium_ftransform()",
                "#define chunkOffset u_RegionOffset",
                "#define at_tangent actinium_at_tangent",
                "#define at_midBlock actinium_at_midBlock",
                "#define iris_Normal actinium_iris_Normal",
                "",
                "void actinium_pack_main();",
                shadowCompatPrototype,
                "",
                "void main() {",
                "    _vert_init();",
                "    actinium_extended_init();",
                "    vec3 actinium_normal = dot(actinium_Normal, actinium_Normal) > 0.0001 ? normalize(actinium_Normal) : vec3(0.0, 1.0, 0.0);",
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
                shadowCompatCall,
                "}",
                ""
        );
    }

    private static String vertexFooter(boolean shadowPass) {
        if (!shadowPass) {
            return "";
        }

        return String.join("\n",
                "",
                "vec3 actinium_shadow_wave_move(vec3 pos) {",
                "    float timer = actinium_ShadowCompatFrameTimeCounter * 3.141592653589793;",
                "    pos = mod(pos, 157.07963267948966);",
                "    vec2 wave_x = vec2(timer * 0.5, timer) + pos.xy;",
                "    vec2 wave_z = vec2(timer, timer * 1.5) + pos.xy;",
                "    vec2 wave_y = vec2(timer * 0.5, timer * 0.25) - pos.zx;",
                "    wave_x = sin(wave_x + wave_y);",
                "    wave_z = cos(wave_z + wave_y);",
                "    return vec3(wave_x.x + wave_x.y, 0.0, wave_z.x + wave_z.y);",
                "}",
                "",
                "vec4 actinium_postprocess_shadow_position(vec4 currentPosition) {",
                "    #if WAVING == 1 && defined(ENTITY_SMALLGRASS) && defined(ENTITY_LOWERGRASS) && defined(ENTITY_UPPERGRASS) && defined(ENTITY_LEAVES)",
                "    bool actiniumIsFoliageEntity =",
                "        actinium_mc_Entity.x == ENTITY_LOWERGRASS ||",
                "        actinium_mc_Entity.x == ENTITY_UPPERGRASS ||",
                "        actinium_mc_Entity.x == ENTITY_SMALLGRASS ||",
                "        actinium_mc_Entity.x == ENTITY_LEAVES;",
                "    #ifdef ENTITY_SMALLENTS",
                "    actiniumIsFoliageEntity = actiniumIsFoliageEntity || actinium_mc_Entity.x == ENTITY_SMALLENTS;",
                "    #endif",
                "    #ifdef ENTITY_SMALLENTS_NW",
                "    actiniumIsFoliageEntity = actiniumIsFoliageEntity || actinium_mc_Entity.x == ENTITY_SMALLENTS_NW;",
                "    #endif",
                "    if (actiniumIsFoliageEntity) {",
                "        #ifdef ENTITY_SMALLENTS_NW",
                "        if (actinium_mc_Entity.x == ENTITY_SMALLENTS_NW) {",
                "            return currentPosition;",
                "        }",
                "        #endif",
                "        vec3 actiniumWorldPos = gl_Vertex.xyz;",
                "        float weight = float(gl_MultiTexCoord0.t < actinium_mc_midTexCoord.t);",
                "        if (actinium_mc_Entity.x == ENTITY_UPPERGRASS) {",
                "            weight += 1.0;",
                "        } else if (actinium_mc_Entity.x == ENTITY_LEAVES) {",
                "            weight = 0.3;",
                "        }",
                "        #ifdef ENTITY_SMALLENTS",
                "        else if (actinium_mc_Entity.x == ENTITY_SMALLENTS && (weight > 0.9 || fract(actiniumWorldPos.y + 0.0675) > 0.01)) {",
                "            weight = 1.0;",
                "        }",
                "        #endif",
                "        vec2 actiniumLmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy * 1.0323886639676114;",
                "        weight *= actiniumLmcoord.y * actiniumLmcoord.y;",
                "        vec3 waveOffsetWorld = actinium_shadow_wave_move(actiniumWorldPos.xzy) * weight * (0.03 + (actinium_ShadowCompatRainStrength * 0.05));",
                "        vec4 shadowPosition = shadowProjection * shadowModelView * vec4(gl_Vertex.xyz + waveOffsetWorld, 1.0);",
                "        float dist = length(shadowPosition.xy);",
                "        float distortFactor = dist * SHADOW_DIST + (1.0 - SHADOW_DIST);",
                "        shadowPosition.xy *= 1.0 / distortFactor;",
                "        shadowPosition.z *= 0.2;",
                "        return shadowPosition;",
                "    }",
                "    #endif",
                "    return currentPosition;",
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
                "struct actinium_FogParameters {",
                "    vec4 color;",
                "    float density;",
                "    float start;",
                "    float end;",
                "    float scale;",
                "};",
                "uniform float iris_FogDensity;",
                "uniform float iris_FogStart;",
                "uniform float iris_FogEnd;",
                "uniform vec4 iris_FogColor;",
                "actinium_FogParameters actinium_gl_Fog = actinium_FogParameters(",
                "    iris_FogColor,",
                "    iris_FogDensity,",
                "    iris_FogStart,",
                "    iris_FogEnd,",
                "    1.0 / max(iris_FogEnd - iris_FogStart, 0.0001)",
                ");",
                "",
                "#define gl_Fog actinium_gl_Fog",
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
            case 9 -> String.join("\n",
                    "    #if defined SHADOW_CASTING && !defined NETHER",
                    "    fragColor0 = vec4(shadowPos.xy, clamp(shadowPos.z, 0.0, 1.0), 1.0);",
                    "    #else",
                    "    fragColor0 = vec4(0.0, 0.0, 0.0, 1.0);",
                    "    #endif");
            case 10 -> String.join("\n",
                    "    #if defined SHADOW_CASTING && !defined NETHER",
                    "    float actiniumOutX = float(shadowPos.x < 0.0 || shadowPos.x > 1.0);",
                    "    float actiniumOutY = float(shadowPos.y < 0.0 || shadowPos.y > 1.0);",
                    "    float actiniumOutZ = float(shadowPos.z < 0.0 || shadowPos.z > 1.0);",
                    "    fragColor0 = vec4(actiniumOutX, actiniumOutY, actiniumOutZ, 1.0);",
                    "    #else",
                    "    fragColor0 = vec4(1.0, 0.0, 0.0, 1.0);",
                    "    #endif");
            case 11 -> String.join("\n",
                    "    #if defined SHADOW_CASTING && !defined NETHER && defined COLORED_SHADOW",
                    "    fragColor0 = texture(shadowcolor0, shadowPos.xy);",
                    "    #elif defined SHADOW_CASTING && !defined NETHER",
                    "    float actiniumShadowDepth = texture(shadowtex1, shadowPos);",
                    "    fragColor0 = vec4(vec3(actiniumShadowDepth), 1.0);",
                    "    #else",
                    "    fragColor0 = vec4(0.0, 0.0, 0.0, 1.0);",
                    "    #endif");
            case 12 -> String.join("\n",
                    "    #if defined SHADOW_CASTING && !defined NETHER && defined COLORED_SHADOW",
                    "    vec2 actiniumShadowUv = gl_FragCoord.xy / vec2(viewWidth, viewHeight);",
                    "    fragColor0 = texture(shadowcolor0, actiniumShadowUv);",
                    "    #else",
                    "    fragColor0 = vec4(0.0, 0.0, 0.0, 1.0);",
                    "    #endif");
            case 13 -> String.join("\n",
                    "    #if defined SHADOW_CASTING && !defined NETHER",
                    "    vec2 actiniumShadowUv = gl_FragCoord.xy / vec2(viewWidth, viewHeight);",
                    "    float actiniumShadowMask = 1.0 - texture(shadowtex1, vec3(actiniumShadowUv, 0.9999));",
                    "    fragColor0 = vec4(vec3(actiniumShadowMask), 1.0);",
                    "    #else",
                    "    fragColor0 = vec4(0.0, 0.0, 0.0, 1.0);",
                    "    #endif");
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
