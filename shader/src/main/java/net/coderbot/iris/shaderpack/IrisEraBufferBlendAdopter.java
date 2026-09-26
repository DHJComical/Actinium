package net.coderbot.iris.shaderpack;

import net.coderbot.iris.gl.blending.BufferBlendInformation;
import net.coderbot.iris.gl.blending.BufferBlendingSupport;
import net.coderbot.iris.shaderpack.option.ShaderPackOptions;

import java.util.List;
import java.util.Map;

/**
 * Recovers per-buffer blend directives ({@code blend.<program>.<buffer>}) that shaders.properties only enables for
 * Iris-era Minecraft versions.
 *
 * <p>shaders.properties is preprocessed with the real {@code MC_VERSION} (11202). Packs written for Iris often wrap
 * their per-buffer blend overrides in conditionals such as {@code #if MC_VERSION >= 11800 || MC_VERSION == 11605}.
 * Those conditionals track which loader versions understand the directive, not a Minecraft feature it depends on, yet
 * the pack's GLSL still relies on the override. For example, I Like Vanilla writes bit-packed lightmap and
 * reflectiveness data to colortex3 from {@code gbuffers_water}. Without {@code blend.gbuffers_water.colortex3 = off}
 * the translucent blend scales those bits by the fragment alpha, which follows the camera pitch, and the screen-space
 * reflections decode garbage and flicker.
 *
 * <p>Actinium supports per-buffer blending, so such directives are adopted from a second evaluation of the file as
 * {@link #IRIS_ERA_MC_VERSION}. Only per-buffer blend directives are adopted. Every other gated directive (program-wide
 * blend, alpha test, custom uniforms, clouds, buffer sizes, ...) keeps the environment evaluation, because it may
 * depend on things that only exist in newer Minecraft versions.
 */
interface IrisEraBufferBlendAdopter {
	/**
	 * The {@code MC_VERSION} of the second evaluation. 11800 (Minecraft 1.18) satisfies the gates packs put around
	 * per-buffer blending ({@code >= 11605}, {@code >= 11700}, {@code >= 11800} and I Like Vanilla's
	 * {@code >= 11800 || == 11605}). It stays below gates that guard content for much newer versions, such as
	 * {@code >= 12102} or I Like Vanilla's {@code >= 260000} block, whose
	 * {@code blend.gbuffers_skytextured.colortex5 = off} is meant for Minecraft 26.x only.
	 */
	int IRIS_ERA_MC_VERSION = 11800;

	/**
	 * Returns the per-buffer blend directives, in file order, that the {@link #IRIS_ERA_MC_VERSION} evaluation of
	 * {@code contents} enables for a (program, buffer index) pair that {@code configured} does not cover yet. The
	 * caller appends them after its own entries.
	 *
	 * <p>Returns an empty list when the environment already evaluates as {@link #IRIS_ERA_MC_VERSION} or newer, when
	 * {@code contents} never mentions {@code MC_VERSION}, when the second evaluation has no per-buffer blend directives,
	 * or when the GL context cannot apply per-buffer blending.
	 *
	 * @param contents           the raw shaders.properties contents
	 * @param options            the shader pack options, so option-dependent conditionals evaluate like the
	 *                           environment evaluation
	 * @param environmentDefines the defines of the environment evaluation; the second evaluation replaces only
	 *                           {@code MC_VERSION}
	 * @param configured         the per-buffer blend state the environment evaluation produced, keyed by program;
	 *                           a directive for a buffer index listed here is never adopted, whether that buffer was
	 *                           named {@code colortexN} or by its legacy name
	 * @throws RuntimeException if a per-buffer blend directive of the second evaluation is malformed, as documented on
	 *                          {@link BufferBlendDirective#parse}
	 */
	List<BufferBlendDirective> adopt(String contents, ShaderPackOptions options, Iterable<StringPair> environmentDefines,
									 Map<String, ? extends List<BufferBlendInformation>> configured);

	/**
	 * Creates an adopter that asks {@code bufferBlendingSupport} whether per-buffer blending is available, and only
	 * does so once the second evaluation has produced per-buffer blend directives.
	 */
	static IrisEraBufferBlendAdopter create(BufferBlendingSupport bufferBlendingSupport) {
		return new IrisEraBufferBlendAdopterImpl(bufferBlendingSupport);
	}
}
