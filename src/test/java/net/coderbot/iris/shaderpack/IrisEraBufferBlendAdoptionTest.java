package net.coderbot.iris.shaderpack;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizons.angelica.glsm.states.BlendState;
import net.coderbot.iris.gl.blending.BlendModeFunction;
import net.coderbot.iris.gl.blending.BlendModeOverride;
import net.coderbot.iris.gl.blending.BufferBlendInformation;
import net.coderbot.iris.gl.blending.BufferBlendingSupport;
import net.coderbot.iris.shaderpack.include.AbsolutePackPath;
import net.coderbot.iris.shaderpack.include.IncludeGraph;
import net.coderbot.iris.shaderpack.option.ShaderPackOptions;
import net.minecraft.launchwrapper.Launch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IrisEraBufferBlendAdoptionTest {
    private static final List<StringPair> LEGACY_ENVIRONMENT = List.of(
            new StringPair("MC_OS_LINUX", ""),
            new StringPair("MC_VERSION", "11202"),
            new StringPair("IS_ACTINIUM", "")
    );
    private static final String EMPTY_PROGRAM = "void main() {}\n";

    @TempDir
    Path tempDir;

    @BeforeAll
    static void initializeLaunchEnvironment() {
        Launch.blackboard = new HashMap<>();
        Launch.blackboard.put("fml.deobfuscatedEnvironment", false);
    }

    @Test
    void adoptsPerBufferDirectivesFromAnIrisEraGate() throws IOException {
        ShaderProperties properties = parse("""
                size.buffer.colortex10 = 2 2
                #if MC_VERSION >= 11800 || MC_VERSION == 11605
                \t
                \t# reminder: some translucent shaders are the same as their opaque variants
                \tblend.gbuffers_clouds.colortex3                = off
                \tblend.gbuffers_entities.colortex2              = off
                \tblend.gbuffers_textured.colortex5              = SRC_ALPHA ONE_MINUS_SRC_ALPHA ONE ONE_MINUS_SRC_ALPHA
                \tblend.gbuffers_water.colortex3                 = off
                \t
                #endif
                """);

        assertEquals(Map.of(
                "gbuffers_clouds", List.of("3 off"),
                "gbuffers_entities", List.of("2 off"),
                "gbuffers_textured", List.of("5 SRC_ALPHA ONE_MINUS_SRC_ALPHA ONE ONE_MINUS_SRC_ALPHA"),
                "gbuffers_water", List.of("3 off")
        ), bufferBlends(properties));

        BufferBlendInformation water = properties.getBufferBlendOverrides().get("gbuffers_water").get(0);
        assertEquals(3, water.getIndex());
        assertNull(water.getBlendMode());

        BlendState textured = properties.getBufferBlendOverrides().get("gbuffers_textured").get(0).getBlendMode();
        assertEquals(BlendModeFunction.SRC_ALPHA.getGlId(), textured.getSrcRgb());
        assertEquals(BlendModeFunction.ONE_MINUS_SRC_ALPHA.getGlId(), textured.getDstRgb());
        assertEquals(BlendModeFunction.ONE.getGlId(), textured.getSrcAlpha());
        assertEquals(BlendModeFunction.ONE_MINUS_SRC_ALPHA.getGlId(), textured.getDstAlpha());
    }

    @Test
    void ignoresDirectivesGatedBehindVersionsNewerThanTheIrisEra() throws IOException {
        ShaderProperties properties = parse("""
                #if MC_VERSION >= 11800 || MC_VERSION == 11605
                \tblend.gbuffers_terrain.colortex2 = off
                \t#if MC_VERSION >= 260000
                \t\tblend.gbuffers_skytextured.colortex5 = off
                \t#endif
                \tblend.gbuffers_water.colortex3 = off
                #endif
                #if MC_VERSION >= 12102
                blend.gbuffers_hand.colortex2 = off
                #endif
                """);

        assertEquals(Map.of(
                "gbuffers_terrain", List.of("2 off"),
                "gbuffers_water", List.of("3 off")
        ), bufferBlends(properties));
    }

    @Test
    void keepsBuffersAlreadyConfiguredForTheEnvironmentVersion() throws IOException {
        ShaderProperties properties = parse("""
                blend.gbuffers_water.composite = SRC_ALPHA ONE_MINUS_SRC_ALPHA ONE ONE
                blend.gbuffers_entities.colortex2 = ONE ZERO ONE ZERO
                #if MC_VERSION >= 11800
                blend.gbuffers_terrain.colortex2 = off
                #else
                blend.gbuffers_terrain.colortex2 = ONE ONE ONE ONE
                #endif
                #if MC_VERSION >= 11800
                blend.gbuffers_water.colortex3 = off
                blend.gbuffers_water.colortex2 = off
                blend.gbuffers_entities.gnormal = off
                blend.gbuffers_hand.colortex2 = off
                #endif
                """);

        assertEquals(Map.of(
                "gbuffers_water", List.of("3 SRC_ALPHA ONE_MINUS_SRC_ALPHA ONE ONE", "2 off"),
                "gbuffers_entities", List.of("2 ONE ZERO ONE ZERO"),
                "gbuffers_terrain", List.of("2 ONE ONE ONE ONE"),
                "gbuffers_hand", List.of("2 off")
        ), bufferBlends(properties));
    }

    @Test
    void appendsAdoptedDirectivesInFileOrderAfterTheEnvironmentEntries() throws IOException {
        ShaderProperties properties = parse("""
                #if MC_VERSION >= 11800
                blend.gbuffers_water.colortex5 = off
                #endif
                blend.gbuffers_water.colortex3 = off
                #if MC_VERSION >= 11800
                blend.gbuffers_water.colortex1 = off
                blend.gbuffers_water.colortex4 = ONE ZERO ONE ZERO
                #endif
                blend.gbuffers_water.colortex6 = off
                """);

        assertEquals(
                List.of("3 off", "6 off", "5 off", "1 off", "4 ONE ZERO ONE ZERO"),
                bufferBlends(properties).get("gbuffers_water")
        );
    }

    @Test
    void adoptsOnlyPerBufferBlendDirectives() throws IOException {
        ShaderProperties properties = parse("""
                #if MC_VERSION >= 11800
                blend.gbuffers_weather = off
                blend.gbuffers_block = SRC_ALPHA ONE_MINUS_SRC_ALPHA ONE ONE
                alphaTest.gbuffers_block = off
                alphaTest.gbuffers_clouds = GREATER 0.1
                clouds = off
                sky = false
                size.buffer.colortex4 = 0.5 0.5
                blend.gbuffers_water.colortex3 = off
                #endif
                """);

        assertEquals(Map.of("gbuffers_water", List.of("3 off")), bufferBlends(properties));
        assertTrue(properties.getBlendModeOverrides().isEmpty());
        assertTrue(properties.getAlphaTestOverrides().isEmpty());
        assertSame(CloudSetting.DEFAULT, properties.getCloudSetting());
        assertSame(OptionalBoolean.DEFAULT, properties.getSky());
        assertTrue(properties.getTextureScaleOverrides().isEmpty());
    }

    @Test
    void doesNotQueryBufferBlendingSupportWithoutPerBufferDirectives() throws IOException {
        AtomicInteger queries = new AtomicInteger();

        ShaderProperties properties = parse("""
                #if MC_VERSION >= 11800
                blend.gbuffers_weather = off
                alphaTest.gbuffers_block = off
                #endif
                blend.dh_water = SRC_ALPHA ONE_MINUS_SRC_ALPHA ONE ONE_MINUS_SRC_ALPHA
                """, LEGACY_ENVIRONMENT, () -> {
            queries.incrementAndGet();
            return true;
        });

        assertEquals(0, queries.get());
        assertTrue(properties.getBufferBlendOverrides().isEmpty());
    }

    static Stream<Arguments> environmentsAtOrAboveTheIrisEra() {
        return Stream.of(
                Arguments.of(11800, Map.of(
                        "gbuffers_water", List.of("3 off"),
                        "gbuffers_entities", List.of("2 off")
                )),
                Arguments.of(12001, Map.of(
                        "gbuffers_water", List.of("3 off")
                ))
        );
    }

    @ParameterizedTest(name = "environment MC_VERSION {0}")
    @MethodSource("environmentsAtOrAboveTheIrisEra")
    void leavesEnvironmentsAtOrAboveTheIrisEraAsEvaluated(int mcVersion, Map<String, List<String>> expected) throws IOException {
        ShaderProperties properties = parse("""
                #if MC_VERSION >= 11800
                blend.gbuffers_water.colortex3 = off
                #endif
                #if MC_VERSION < 12000
                blend.gbuffers_entities.colortex2 = off
                #endif
                """, List.of(new StringPair("MC_VERSION", Integer.toString(mcVersion))));

        assertEquals(expected, bufferBlends(properties));
    }

    @Test
    void skipsPerBufferDirectivesWithoutBufferBlendingSupport() throws IOException {
        ShaderProperties properties = parse("""
                blend.gbuffers_water.colortex3 = off
                blend.gbuffers_weather = off
                #if MC_VERSION >= 11800
                blend.gbuffers_entities.colortex2 = off
                #endif
                """, LEGACY_ENVIRONMENT, () -> false);

        assertTrue(properties.getBufferBlendOverrides().isEmpty());
        assertSame(BlendModeOverride.OFF, properties.getBlendModeOverrides().get("gbuffers_weather"));
    }

    @ParameterizedTest(name = "gated={0}")
    @ValueSource(booleans = {false, true})
    void rejectsNonNumericColortexSuffixLikeUpstream(boolean gated) {
        RuntimeException failure = assertThrowsExactly(RuntimeException.class,
                () -> parse(gateIf(gated, "blend.gbuffers_water.colortexX = off")));

        assertEquals("Failed to parse buffer blend!", failure.getMessage());
        assertInstanceOf(NumberFormatException.class, failure.getCause());
    }

    @ParameterizedTest(name = "gated={0}")
    @ValueSource(booleans = {false, true})
    void rejectsUnknownBufferNameLikeUpstream(boolean gated) {
        RuntimeException failure = assertThrowsExactly(RuntimeException.class,
                () -> parse(gateIf(gated, "blend.gbuffers_water.gaux9 = off")));

        assertEquals("Failed to parse buffer blend! index = -1", failure.getMessage());
    }

    @ParameterizedTest(name = "gated={0}")
    @ValueSource(booleans = {false, true})
    void rejectsUnknownBlendFunctionLikeUpstream(boolean gated) {
        assertThrowsExactly(NoSuchElementException.class,
                () -> parse(gateIf(gated, "blend.gbuffers_water.colortex3 = SRC_ALPHA BOGUS ONE ONE")));
    }

    @Test
    void evaluatesShaderOptionsTogetherWithTheIrisEraVersion() throws IOException {
        String source = """
                #if IMPROVED_RAIN_DEFINE == 1 && !defined MC_OS_MAC && MC_VERSION >= 11605
                    blend.gbuffers_weather.colortex12=off
                #endif
                blend.gbuffers_textured.colortex4=off
                """;
        String program = "#define IMPROVED_RAIN_DEFINE 1 //[0 1]\n" + EMPTY_PROGRAM;

        ShaderProperties defaults = new ShaderProperties(source, createOptions(program, Map.of()), LEGACY_ENVIRONMENT, () -> true);
        ShaderProperties rainDisabled = new ShaderProperties(source,
                createOptions(program, Map.of("IMPROVED_RAIN_DEFINE", "0")), LEGACY_ENVIRONMENT, () -> true);

        assertEquals(Map.of(
                "gbuffers_textured", List.of("4 off"),
                "gbuffers_weather", List.of("12 off")
        ), bufferBlends(defaults));
        assertEquals(Map.of("gbuffers_textured", List.of("4 off")), bufferBlends(rainDisabled));
    }

    private static String gateIf(boolean gated, String directive) {
        if (!gated) {
            return directive + "\n";
        }

        return "#if MC_VERSION >= 11800\n" + directive + "\n#endif\n";
    }

    private ShaderProperties parse(String source) throws IOException {
        return parse(source, LEGACY_ENVIRONMENT);
    }

    private ShaderProperties parse(String source, List<StringPair> environmentDefines) throws IOException {
        return parse(source, environmentDefines, () -> true);
    }

    private ShaderProperties parse(String source, List<StringPair> environmentDefines, BufferBlendingSupport support)
            throws IOException {
        return new ShaderProperties(source, createOptions(EMPTY_PROGRAM, Map.of()), environmentDefines, support);
    }

    private ShaderPackOptions createOptions(String program, Map<String, String> changedConfigs) throws IOException {
        Files.writeString(tempDir.resolve("entry.glsl"), program);
        IncludeGraph graph = new IncludeGraph(
                tempDir,
                ImmutableList.of(AbsolutePackPath.fromAbsolutePath("/entry.glsl"))
        );
        return new ShaderPackOptions(graph, changedConfigs);
    }

    private static Map<String, List<String>> bufferBlends(ShaderProperties properties) {
        Map<String, List<String>> rendered = new HashMap<>();
        properties.getBufferBlendOverrides().forEach((program, informations) -> {
            List<String> renderedInformations = new ArrayList<>();
            for (BufferBlendInformation information : informations) {
                renderedInformations.add(render(information));
            }
            rendered.put(program, renderedInformations);
        });
        return rendered;
    }

    private static String render(BufferBlendInformation information) {
        BlendState blendMode = information.getBlendMode();
        if (blendMode == null) {
            return information.getIndex() + " off";
        }

        return information.getIndex() + " " + functionName(blendMode.getSrcRgb()) + " " + functionName(blendMode.getDstRgb())
                + " " + functionName(blendMode.getSrcAlpha()) + " " + functionName(blendMode.getDstAlpha());
    }

    private static String functionName(int glId) {
        for (BlendModeFunction function : BlendModeFunction.values()) {
            if (function.getGlId() == glId) {
                return function.name();
            }
        }

        throw new AssertionError("Unknown blend function GL id " + glId);
    }
}
