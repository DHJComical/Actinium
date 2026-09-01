// Native ESSL has no fixed-function fog interface.  MobileGlues and SFPEW
// translate the legacy gl_Fog/gl_FogFragCoord symbols, while a direct ESSL
// context receives equivalent values through explicit uniforms.
#ifdef GL_ES
#define BP_GL_ES 1
#else
#define BP_GL_ES 0
#endif
#ifdef MG_MOBILEGLUES
#define BP_MOBILEGLUES 1
#else
#define BP_MOBILEGLUES 0
#endif

#if BP_GL_ES && !BP_MOBILEGLUES
#define BP_NATIVE_ES 1
#else
#define BP_NATIVE_ES 0
#endif

#if BP_NATIVE_ES
precision mediump float;
precision mediump sampler2D;
uniform vec3 bpWorldFogColor;
uniform float bpFogStart;
uniform float bpFogScale;
varying mediump float bpFogFragCoord;
#endif

uniform sampler2D sampler;
uniform vec2 screenSize;
uniform float fogDensity;
uniform vec3 fogColor;
uniform float opacity;

void main() {
    vec2 uv = gl_FragCoord.xy / screenSize;
    vec3 color = texture2D(sampler, uv).rgb;
#if BP_NATIVE_ES
    color = mix(color, bpWorldFogColor,
            clamp((bpFogFragCoord - bpFogStart) * bpFogScale, 0.0, 1.0));
#else
    color = mix(color, gl_Fog.color.rgb,
            clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0));
#endif
    color = mix(color, fogColor, fogDensity);
    // Fade the remote view out by fading its alpha so the background shows
    // through instead of the view darkening to black.  Explicit depth writes
    // are unnecessary: the default fragment depth is gl_FragCoord.z.
    gl_FragColor = vec4(color, opacity);
}
