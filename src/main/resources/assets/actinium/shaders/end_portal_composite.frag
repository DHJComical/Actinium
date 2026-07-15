#version 330 core

const int MAX_LAYERS = 16;

in vec2 v_TexCoord;

uniform sampler2D u_EndSky;
uniform sampler2D u_EndPortal;
uniform int u_LayerCount;
uniform vec4 u_Transforms[MAX_LAYERS];
uniform vec2 u_Offsets[MAX_LAYERS];
uniform vec3 u_Colors[MAX_LAYERS];

out vec4 fragColor;

vec2 transformCoordinate(int layer, vec2 projected) {
    return u_Offsets[layer] + vec2(
        dot(projected, u_Transforms[layer].xy),
        dot(projected, u_Transforms[layer].zw)
    );
}

void main() {
    vec2 projected = v_TexCoord * 2.0 - 1.0;
    vec3 color = texture(u_EndSky, transformCoordinate(0, projected)).rgb * u_Colors[0];

    for (int layer = 1; layer < u_LayerCount; ++layer) {
        color += texture(u_EndPortal, transformCoordinate(layer, projected)).rgb * u_Colors[layer];
    }

    fragColor = vec4(color, 1.0);
}
