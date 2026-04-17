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

void actinium_extended_init() {
    actinium_MidTexCoord = mc_midTexCoord * ACTINIUM_MID_TEX_SCALE;
    actinium_Tangent = at_tangent;
    actinium_Normal = iris_Normal;
    actinium_MidBlock = vec4(at_midBlock.xyz / 64.0, at_midBlock.w / 15.0);
    actinium_EntityData = vec2(float(mc_Entity >> 1u), float(mc_Entity & 1u));
}
#else
vec2 actinium_MidTexCoord;
vec4 actinium_Tangent;
vec3 actinium_Normal;
vec4 actinium_MidBlock;
vec2 actinium_EntityData;

void actinium_extended_init() {
    actinium_MidTexCoord = vec2(0.0);
    actinium_Tangent = vec4(0.0, 0.0, 1.0, 1.0);
    actinium_Normal = vec3(0.0, 1.0, 0.0);
    actinium_MidBlock = vec4(0.0);
    actinium_EntityData = vec2(0.0);
}
#endif
