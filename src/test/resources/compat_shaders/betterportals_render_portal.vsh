// Native ESSL has no fixed-function vertex interface.  MobileGlues and SFPEW
// translate the legacy interface before submitting it to GLES; a direct ESSL
// context uses the explicit attribute/matrix path below.
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

// The compatibility wrappers do not expose gl_ClipVertex in translated ESSL.
// SFPEW's generated source is modern as well, so omit the assignment there.
#if BP_GL_ES || BP_MOBILEGLUES
#define BP_SKIP_FIXED_CLIP 1
#else
#if __VERSION__ >= 130
#define BP_SKIP_FIXED_CLIP 1
#else
#define BP_SKIP_FIXED_CLIP 0
#endif
#endif

#if BP_NATIVE_ES
precision highp float;
attribute vec4 Position;
uniform mat4 bpModelViewMatrix;
uniform mat4 bpProjectionMatrix;
varying mediump float bpFogFragCoord;
#endif

void main() {
#if BP_NATIVE_ES
    vec4 viewPos = bpModelViewMatrix * Position;
    gl_Position = bpProjectionMatrix * viewPos;
    bpFogFragCoord = length(viewPos.xyz);
#else
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
#if !BP_SKIP_FIXED_CLIP
    gl_ClipVertex = viewPos;
#endif
    gl_Position = gl_ProjectionMatrix * viewPos;
    gl_FogFragCoord = length(viewPos.xyz);
#endif
}
