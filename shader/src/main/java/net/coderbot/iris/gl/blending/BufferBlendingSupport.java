package net.coderbot.iris.gl.blending;

/**
 * Reports whether the GL context can apply per-buffer blend state ({@code glEnablei}/{@code glBlendFuncSeparatei},
 * core in OpenGL 4.0 or via {@code GL_ARB_draw_buffers_blend}).
 *
 * <p>shaders.properties parsing decides whether {@code blend.<program>.<buffer>} directives can be honoured. The
 * production answer comes from the GL capabilities captured at context creation, which do not exist in unit tests,
 * so the check is injected: shader pack loading passes {@code RenderSystem::supportsBufferBlending}, tests pass a
 * constant.
 */
@FunctionalInterface
public interface BufferBlendingSupport {
	/**
	 * Returns {@code true} when per-buffer blend directives can be applied; callers ignore those directives otherwise.
	 */
	boolean isSupported();
}
