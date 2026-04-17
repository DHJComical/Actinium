#version 330 core

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_vertex.glsl>
#import <sodium:include/chunk_matrices.glsl>
#import <sodium:include/chunk_material.glsl>
#import <actinium:include/chunk_vertex_extended.glsl>

out vec4 v_Color;
out vec2 v_TexCoord;
out float v_ChunkAgeMs;
out float v_MaterialMipBias;

#ifdef ACTINIUM_EXTENDED_VERTEX_FORMAT
out vec2 v_ActiniumMidTexCoord;
out vec4 v_ActiniumTangent;
out vec3 v_ActiniumNormal;
out vec4 v_ActiniumMidBlock;
out vec2 v_ActiniumEntityData;
#endif

#ifdef USE_FRAGMENT_DISCARD
out float v_MaterialAlphaCutoff;
#endif

#if defined(USE_FOG_POSTMODERN)
out float v_SphericalFragDistance;
out float v_CylindricalFragDistance;
#elif defined(USE_FOG)
out float v_FragDistance;
#endif

uniform int u_FogShape;
uniform vec3 u_RegionOffset;

#ifndef CELERITAS_NO_LIGHTMAP
uniform sampler2D u_LightTex;

vec4 _sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}
#endif

uniform float celeritas_ChunkAges[REGION_SIZE];

void main() {
    _vert_init();
    actinium_extended_init();

#ifdef ACTINIUM_EXTENDED_VERTEX_FORMAT
    v_ActiniumMidTexCoord = actinium_MidTexCoord;
    v_ActiniumTangent = actinium_Tangent;
    v_ActiniumNormal = actinium_Normal;
    v_ActiniumMidBlock = actinium_MidBlock;
    v_ActiniumEntityData = actinium_EntityData;
#endif

    vec3 translation = u_RegionOffset + _get_draw_translation(_draw_id);
    vec3 position = _vert_position + translation;

#if defined(USE_FOG_POSTMODERN)
    v_SphericalFragDistance = getFragDistance(FOG_SHAPE_SPHERICAL, position);
    v_CylindricalFragDistance = getFragDistance(FOG_SHAPE_CYLINDRICAL, position);
#elif defined(USE_FOG)
    v_FragDistance = getFragDistance(u_FogShape, position);
#endif

    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);

#ifdef CELERITAS_NO_LIGHTMAP
    v_Color = _vert_color;
#else
    v_Color = _vert_color * _sample_lightmap(u_LightTex, _vert_tex_light_coord);
#endif
    v_TexCoord = _vert_tex_diffuse_coord;
    v_MaterialMipBias = _material_mip_bias(_material_params);

#ifdef USE_FRAGMENT_DISCARD
    v_MaterialAlphaCutoff = _material_alpha_cutoff(_material_params);
#endif

    v_ChunkAgeMs = celeritas_ChunkAges[_draw_id];
}
