#version 330 core

#import <sodium:include/fog.glsl>

in vec4 v_Color;
in vec2 v_TexCoord;
in float v_ChunkAgeMs;
in float v_MaterialMipBias;

#ifdef ACTINIUM_EXTENDED_VERTEX_FORMAT
in vec2 v_ActiniumMidTexCoord;
in vec4 v_ActiniumTangent;
in vec3 v_ActiniumNormal;
in vec4 v_ActiniumMidBlock;
in vec2 v_ActiniumEntityData;
#endif

#ifdef USE_FRAGMENT_DISCARD
in float v_MaterialAlphaCutoff;
#endif

#if defined(USE_FOG_POSTMODERN)
in float v_SphericalFragDistance;
in float v_CylindricalFragDistance;
#elif defined(USE_FOG)
in float v_FragDistance;
#endif

uniform sampler2D u_BlockTex;
uniform vec4 u_FogColor;

#ifdef USE_FOG_SMOOTH
uniform float u_FogStart;
uniform float u_FogEnd;
#endif

#ifdef USE_FOG_POSTMODERN
uniform float u_RenderDistFogStart;
uniform float u_RenderDistFogEnd;
uniform float u_EnvFogStart;
uniform float u_EnvFogEnd;
#endif

#ifdef USE_FOG_EXP2
uniform float u_FogDensity;
#endif

#ifndef LEGACY
out vec4 fragColor;
#else
#define fragColor gl_FragColor
#endif

void main() {
    vec4 diffuseColor = texture(u_BlockTex, v_TexCoord, v_MaterialMipBias);

#ifdef USE_FRAGMENT_DISCARD
    float alphaCutoff = v_MaterialAlphaCutoff > 0.0 ? v_MaterialAlphaCutoff : 0.1;
    if (diffuseColor.a < alphaCutoff) {
        discard;
    }
#endif

#ifdef USE_VANILLA_COLOR_FORMAT
    diffuseColor *= v_Color;
#else
    diffuseColor.rgb *= v_Color.rgb;
    diffuseColor.rgb *= v_Color.a;
#endif

#ifdef USE_FOG
#if defined(CHUNK_FADE_IN_DURATION_MS) && CHUNK_FADE_IN_DURATION_MS > 0
    diffuseColor = vec4(mix(u_FogColor.rgb, diffuseColor.rgb, (clamp(v_ChunkAgeMs, 0, CHUNK_FADE_IN_DURATION_MS) / CHUNK_FADE_IN_DURATION_MS)), diffuseColor.a);
#endif

#ifdef USE_FOG_POSTMODERN
    float fogValue = max(_linearFogValue(v_CylindricalFragDistance, u_RenderDistFogStart, u_RenderDistFogEnd),
                         _linearFogValue(v_SphericalFragDistance, u_EnvFogStart, u_EnvFogEnd));

    fragColor = vec4(mix(diffuseColor.rgb, u_FogColor.rgb, fogValue * u_FogColor.a), diffuseColor.a);
#elif defined(USE_FOG_EXP2)
    fragColor = _exp2Fog(diffuseColor, v_FragDistance, u_FogColor, u_FogDensity);
#elif defined(USE_FOG_SMOOTH)
    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
#endif
#else
    fragColor = diffuseColor;
#endif
}
