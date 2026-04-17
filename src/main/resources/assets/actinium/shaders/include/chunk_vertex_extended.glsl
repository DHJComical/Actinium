#ifdef ACTINIUM_EXTENDED_VERTEX_FORMAT
in vec2 mc_midTexCoord;
in vec4 at_tangent;
in vec3 iris_Normal;
in uint mc_Entity;
in vec4 at_midBlock;

vec2 actinium_MidTexCoord;
vec4 actinium_Tangent;
vec3 actinium_Normal;
vec4 actinium_MidBlock;
vec2 actinium_EntityData;
vec2 iris_MidTex;
ivec2 iris_Entity;
vec4 irs_Tangent;
vec3 irs_Normal;

void actinium_extended_init() {
    actinium_MidTexCoord = mc_midTexCoord * ACTINIUM_MID_TEX_SCALE;
    actinium_Tangent = at_tangent;
    actinium_Normal = iris_Normal;
    actinium_MidBlock = vec4(at_midBlock.xyz / 64.0, at_midBlock.w / 15.0);
    actinium_EntityData = vec2(float(int(mc_Entity >> 1u) - 1), float(mc_Entity & 1u));
    iris_MidTex = actinium_MidTexCoord;
    iris_Entity = ivec2(int(actinium_EntityData.x), int(actinium_EntityData.y));
    irs_Tangent = actinium_Tangent;
    irs_Normal = actinium_Normal;
}
#else
vec2 actinium_MidTexCoord;
vec4 actinium_Tangent;
vec3 actinium_Normal;
vec4 actinium_MidBlock;
vec2 actinium_EntityData;
vec2 iris_MidTex;
ivec2 iris_Entity;
vec4 irs_Tangent;
vec3 irs_Normal;

void actinium_extended_init() {
    actinium_MidTexCoord = vec2(0.0);
    actinium_Tangent = vec4(0.0, 0.0, 1.0, 1.0);
    actinium_Normal = vec3(0.0, 1.0, 0.0);
    actinium_MidBlock = vec4(0.0);
    actinium_EntityData = vec2(-1.0, 0.0);
    iris_MidTex = actinium_MidTexCoord;
    iris_Entity = ivec2(-1, 0);
    irs_Tangent = actinium_Tangent;
    irs_Normal = actinium_Normal;
}
#endif
