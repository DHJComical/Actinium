# Actinium Shader Pipeline Status

Last updated: 2026-04-26

This document summarizes the current Actinium shader-pipeline work, the architecture that now exists, the external projects used as references, and the known risks that should guide the next development pass.

## Current State

Actinium is now able to run the MakeUp Ultra Fast shader pack in a usable state. The core world render path, sky path, water path, post pipeline, GUI isolation, TAA compatibility, volumetric-cloud texture sampling, and shadow path have all been iterated to the point where the latest reported state is stable: the scene renders normally, volumetric clouds no longer stretch to infinity, and real-time shadows are working again in the current MakeUp test configuration.

The current work is not yet a complete Iris-equivalent implementation. It is a compatibility pipeline that supports enough OptiFine/Iris-style behavior for MakeUp to work, while still using selective fallbacks and guarded integration points where the full deferred renderer is not yet implemented.

The current working baseline now includes the completed MakeUp shadow bring-up described below. The most important recent changes are concentrated in shadow compatibility, entity shadow semantics, and the final cleanup needed to keep the runtime stable after the shadow work.

## Recent Progress

### Stability and GUI Isolation

- Fixed repeated cases where post/final rendering polluted GUI rendering, including white screens, missing inventory, menu residue, and framebuffer state leaking after the world pass.
- Restored main framebuffer state after post/final passes so GUI and first-person rendering are not affected by shader framebuffer bindings.
- Stabilized first-person hand and held-item rendering after shader passes; missing faces in first-person view were resolved.

### Terrain and Water

- Recovered normal terrain rendering after multiple regressions where terrain was invisible, black, or milk-white.
- Fixed translucent terrain/water block metadata and shader block mapping so vanilla water is recognized by shader pack logic.
- Restored water rendering from black to normal, including improved full-screen water reflection behavior.
- Disabled broad external terrain redirection for solid/cutout paths where it caused scene loss; translucent terrain remains the sensitive path for water and post interactions.

### Shadows and Entities

- Rebuilt the MakeUp shadow path into a usable state after multiple iterations of black terrain, missing shadows, unstable noise, and camera-relative shadow jitter.
- Block, grass, foliage, and animal shadows are now rendering correctly in the current verified state.
- Fixed dynamic foliage shadow behavior so waving plants animate in shadows instead of projecting a static silhouette.
- Routed shadow-entity rendering through the external `shadow` program and refreshed `entityId` / `entityColor` semantics during entity draws, bringing the result closer to shader-core/Iris expectations.
- Reworked entity-side shader binding several times to isolate side effects from `RenderGlobal.renderEntities`, ending with a per-entity binding path that no longer blocks the final shadow result.

### Sky and Clouds

- Reworked sky handling several times to prevent white skyboxes, flashing horizon walls, broken sun/moon quads, and lower-sky artifacts.
- Replaced unstable vanilla sky suppression behavior with a managed sky path that better preserves shader-pack sky color and celestial handling.
- Fixed cloud color synchronization enough for the current usable state.
- Added cloud setting override support so shader-pack `clouds=off/fast/fancy/on` can affect vanilla cloud rendering.
- Fixed custom pack texture wrapping for `gaux2` and `noisetex`, avoiding `CLAMP_TO_EDGE` on repeating cloud/noise textures. This fixed MakeUp volumetric clouds stretching toward four directions.

### Post Pipeline and Deferred Compatibility

- Enabled external scene post programs and final pass support instead of hard-disabling the post chain.
- Added sampler/unit compatibility for common OptiFine/Iris post inputs, including `colortex4..7`, `gaux1..4`, `depthtex0..2`, `shadowtex0/1`, `shadowcolor0/1`, and `noisetex`.
- Expanded post depth target support to include `depthtex2`.
- Adjusted `prepare` and `deferred` execution timing. The old early-world execution path is disabled; compatibility execution now happens in the post phase after the real scene exists.
- Split `prepare` and `deferred` handling so `prepare` does not overwrite scene `colortex1`, while `deferred` can run from the current real scene and merge its outputs. This fixed the milk-white scene caused by sky/prepass data replacing world color.

### TAA

- Improved temporal anti-aliasing behavior against MakeUp by referencing Iris 26.1 behavior.
- Reduced static blur, movement blur, ghosting, and camera-relative floating artifacts enough that the result is considered usable.
- TAA still remains a compatibility-sensitive area because it depends on previous matrices, previous camera position, depth snapshots, and history buffer semantics.

### Shader Pack GUI

- Built a fuller shader-pack configuration GUI modeled after Iris-style option screens.
- Fixed option rows being clickable outside the visible scroll region.
- Removed the dirt background from the in-game shader config panel so players can observe live shader effects while adjusting options.
- Corrected shader pack selection button alignment issues.

## Current Architecture

### Main Pipeline Owner

`src/main/java/com/dhj/actinium/shader/pipeline/ActiniumRenderPipeline.java`

This singleton coordinates almost all shader integration work:

- Tracks render stage state: world, sky, clouds, weather, shadow, post, and final.
- Captures camera, matrices, fog color, previous-frame data, frame counters, and TAA jitter.
- Resolves and compiles external shader pack programs.
- Owns world-stage targets, post targets, shadow targets, fallback textures, custom shader-pack textures, and terrain input textures.
- Executes prepare/deferred/composite/final programs.
- Restores OpenGL/framebuffer state after shader passes.
- Owns the shadow pass integration for terrain and entities, including current MakeUp-focused compatibility behavior.

### Post Targets

`src/main/java/com/dhj/actinium/shader/pipeline/ActiniumPostTargets.java`

This manages OptiFine/Iris-style post render targets:

- `colortex0..3`
- `gaux1..4`
- `depthtex0..2`
- target clear directives
- ping-pong source/write textures
- framebuffer binding for post draw buffers
- copying scene, pre-translucent depth, and selected intermediate outputs

Current compatibility still assumes a limited target set of 8 color targets. More advanced packs may need additional color attachments, image bindings, compute support, or more exact buffer-flip semantics.

### Post Shader Interface

`src/main/java/com/dhj/actinium/shader/pipeline/ActiniumPostShaderInterface.java`

This binds uniforms and samplers expected by external post programs:

- Scene samplers: `colortex0..7`, `gcolor`, `gdepth`, `gnormal`, `composite`, `gaux1..4`
- Depth samplers: `depthtex0`, `depthtex1`, `depthtex2`
- Shadow samplers: `shadow`, `watershadow`, `shadowtex0`, `shadowtex1`, `shadowcolor0`, `shadowcolor1`
- Temporal inputs: previous matrices, camera position, previous camera position, frame counters, frame time
- Environment inputs: fog color, sky color, rain, world time, moon phase, sun/moon/shadow light positions, eye brightness, water state

### Mixin Entry Points

`src/main/java/org/taumc/celeritas/mixin/features/render/EntityRendererActiniumPipelineMixin.java`

Major world-render hooks:

- Begin world pass.
- Legacy prepare/deferred hooks remain present but are disabled by pipeline guards.
- Capture world state and run post before first-person hand rendering.
- Run shadow pipeline before world rendering.
- Begin/end weather stage.

`src/main/java/org/taumc/celeritas/mixin/features/render/RenderGlobalActiniumPipelineMixin.java`

Major world-geometry hooks:

- Begin/end managed sky.
- Track sky texture/celestial segments.
- Begin/end cloud stage.
- Replace or suppress unstable parts of vanilla sky/horizon behavior.
- Stabilize selection-box rendering to avoid diagonal artifacts.

`src/main/java/org/taumc/celeritas/mixin/features/render/GameSettingsActiniumCloudMixin.java`

Cloud-mode override hook:

- Intercepts `GameSettings.shouldRenderClouds()`.
- Applies shader-pack `clouds` property while preserving vanilla low-render-distance behavior.

### Shader Pack Properties

`src/main/java/com/dhj/actinium/shader/pack/ActiniumShaderProperties.java`

Parses shader-pack metadata and directives:

- `clouds`
- shadow flags and shadow settings
- `prepareBeforeShadow`
- program enable/disable directives
- custom texture declarations such as `texture.gbuffers.gaux2`
- explicit buffer flip directives such as `flip.composite.colortex1`
- shader option screen metadata

### Terrain Integration

The terrain shader integration remains intentionally conservative:

- Solid and cutout terrain generally use Celeritas defaults where full external terrain override is unsafe.
- Translucent/water paths have custom context support for shader block IDs and water handling.
- `ENABLE_EXTERNAL_TERRAIN_REDIRECT` is currently false in the pipeline, reflecting the fact that broad terrain redirection was a major source of invisible terrain, black screens, and GUI pollution.

## Important Current Behavior

### Prepare and Deferred

Current behavior is intentionally not a full Iris clone:

- Early `renderPreparePipeline` and `renderDeferredPipeline` calls are guarded by `ENABLE_PRE_SCENE_PIPELINES = false`.
- `prepare` and `deferred` are executed in the post phase through `executePreSceneProgramsInPostPhase`.
- `prepare` output does not merge `colortex1` back into the main post targets.
- `deferred` runs from the real scene input and can merge its draw-buffer outputs.

Reasoning:

- Running MakeUp `prepare/deferred` too early caused depth to look like sky and produced incorrect volumetric clouds and washed output.
- Merging `prepare` `colortex1` replaced the scene with sky/prepass color and made most geometry milk-white.
- Running `deferred` from the real scene recovered effects without reintroducing the white scene.

### Texture Wrapping

Custom pack textures were previously uploaded with `GL_CLAMP_TO_EDGE`. That is wrong for repeating noise/cloud textures.

Current behavior:

- `gaux2` uses repeat wrapping.
- `noisetex` uses repeat wrapping.
- Paths containing `noise` or `cloud` use repeat wrapping.
- Other custom textures remain clamp-to-edge by default.

This is important for MakeUp because its volumetric clouds sample `gaux2` in world space. Clamp wrapping stretches edge pixels toward infinity, exactly matching the observed four-direction cloud streaking.

## Reference Projects

### Iris 26.1

Path: `D:\Github Desktop\Iris-26.1`

Used for:

- Composite/deferred/final pass ordering.
- Buffer flip behavior in `CompositeRenderer`.
- Cloud setting override behavior.
- TAA previous matrix and camera-position expectations.
- OptiFine/Iris sampler naming and render target conventions.

Key reference files:

- `common/src/main/java/net/irisshaders/iris/pipeline/IrisRenderingPipeline.java`
- `common/src/main/java/net/irisshaders/iris/pipeline/CompositeRenderer.java`
- `common/src/main/java/net/irisshaders/iris/pipeline/FinalPassRenderer.java`
- `common/src/main/java/net/irisshaders/iris/mixin/sky/MixinOptions_CloudsOverride.java`
- `common/src/main/java/net/irisshaders/iris/shaderpack/properties/ShaderProperties.java`

### shader-core-src-mod

Path: `D:\Github Desktop\CleanBoom\shader-core-src-mod`

Used for:

- Legacy shader-core sky behavior.
- Horizon wall and sky lower-half behavior.
- Understanding how older shader-core-compatible code expects world-stage sky to be drawn.

Note: the exact path should be rechecked before future comparisons because some earlier searches used `D:\Github Desktop\shader-core-src-mod`, which did not exist.

### Minecraft 1.12.2 Source

Path: `D:\Github Desktop\CleanBoom\minecraft-src`

Used for:

- Vanilla `RenderGlobal.renderSky` and `RenderGlobal.renderClouds` behavior.
- `GameSettings.shouldRenderClouds()` behavior.
- First-person, GUI, and framebuffer ordering around `EntityRenderer.renderWorldPass`.

### glsl-transformation-lib

Path: `D:\Github Desktop\glsl-transformation-lib`

Potential use:

- Future robust GLSL parsing and transformation.
- Replacing current regex/source-string compatibility patches with structured shader transforms.

This has not yet become a core dependency in the current Actinium implementation.

## Active Shader Pack

Primary test pack:

```text
D:\Github Desktop\Actinium\run\client\shaderpacks\MakeUp-UltraFast-9.4c
```

Important MakeUp files examined:

- `shaders/common/prepare_fragment.glsl`
- `shaders/common/deferred_fragment.glsl`
- `shaders/common/composite_fragment.glsl`
- `shaders/common/composite1_fragment.glsl`
- `shaders/common/composite2_fragment.glsl`
- `shaders/common/final_fragment.glsl`
- `shaders/lib/volumetric_clouds.glsl`
- `shaders/src/writebuffers.glsl`
- `shaders/shaders.properties`

MakeUp-specific findings:

- `deferred_fragment.glsl` writes `DRAWBUFFERS:14`, mapping to `colortex1` and `gaux1`.
- `final_fragment.glsl` documents `gaux1` as SSR/Bloom auxiliary, `gaux2` as clouds texture, `gaux3` as exposure auxiliary, and `gaux4` as fog auxiliary.
- `shaders.properties` declares `texture.gbuffers.gaux2` and `texture.deferred.gaux2` as cloud textures.
- MakeUp volumetric clouds depend on repeating `gaux2` sampling.

## Known Risks

### Prepare/Deferred Semantics

The current prepare/deferred path is a compatibility compromise. It works for the current MakeUp testing state, but may not match packs that rely on exact Iris timing:

- Iris runs `prepare` after shadows and `deferred` before translucents.
- Actinium currently runs them during post compatibility execution.
- Depth and scene inputs may not match packs expecting exact pre-translucent or pre-hand timing.

Future work should move toward true Iris stage timing once the framebuffer state and terrain outputs are robust enough.

### Buffer Flip Semantics

Actinium implements explicit flips and ping-pong targets, but it is not yet a complete Iris `BufferFlipper` equivalent.

Potential failure modes:

- stale reads from the wrong side of a target pair
- history targets not matching TAA expectations
- explicit `flip.*` directives working for simple cases but failing in complex multi-pass packs

### Render Target Coverage

Only the legacy 8 color targets are currently modeled:

```text
colortex0, colortex1, colortex2, colortex3, gaux1, gaux2, gaux3, gaux4
```

Potential missing features:

- additional `colortexN` targets beyond legacy aliases
- image load/store bindings
- compute programs
- per-target resolution scaling
- more complete target format handling
- per-texture filtering and mipmap directives beyond current support

### Terrain G-Buffer Completeness

Solid/cutout terrain does not yet fully run external terrain programs in the safe default path.

This still limits:

- material-specific deferred data
- normal/specular data
- full shader-pack water and reflection behavior in packs that depend on richer terrain gbuffers

The conservative approach avoids major regressions but also means many high-end effects will still be absent, even though the current MakeUp shadow result is now considered usable.

### Sky and Cloud Edge Cases

Sky is much more stable now, but it remains sensitive because the implementation combines:

- vanilla 1.12.2 sky geometry
- shader-core-like horizon rendering
- external `gbuffers_skybasic` and `gbuffers_skytextured`
- post-stage volumetric clouds

Risks to watch:

- sunrise/sunset color mismatches
- lower-sky artifacts at extreme heights
- celestial texture depth/order issues
- cloud color varying with camera orientation

### GL State Leakage

Many previous regressions were caused by state leaking into GUI, hand rendering, or framebuffer presentation.

High-risk states:

- active framebuffer
- draw/read buffers
- bound textures on high texture units
- active shader program
- blend/depth/alpha/cull/scissor state
- viewport
- VAO binding

Any new pass should explicitly restore or isolate these states.

### Debug Logging

Debug logging is useful but can become noisy quickly. Existing debug paths already sample framebuffer/texture centers and record pass bindings, but new debug should remain throttled.

Useful log location:

```text
D:\Github Desktop\Actinium\run\client\logs\latest.log
```

## Suggested Next Steps

### Short Term

- Commit the current working shadow fixes and keep the docs synchronized with the verified runtime state.
- Keep testing MakeUp with high feature settings: volumetric clouds, water reflection, shadows, TAA, bloom, DOF, and volumetric light.
- Add targeted debug only when a new regression appears; the latest shadow bring-up required several temporary debug paths that should stay secondary to runtime stability.

### Medium Term

- Implement more Iris-like `prepare` and `deferred` timing instead of post-phase compatibility execution.
- Improve buffer flip tracking to more closely match Iris `CompositeRenderer` and `BufferFlipper` behavior.
- Expand render target support beyond the legacy 8 aliases.
- Improve shader-pack custom texture parsing, including explicit texture formats and filtering/wrap directives.

### Long Term

- Move from regex/string GLSL fixes toward structured GLSL transformation, potentially using `glsl-transformation-lib`.
- Implement a fuller terrain gbuffer path for solid/cutout materials without breaking Celeritas stability.
- Add compute/image support for more modern shader packs.
- Build a small compatibility test matrix with MakeUp plus at least one BSL/Complementary-style pack.

## Current Verification Command

Use this compile command for Java-level validation:

```powershell
.\gradlew compileJava --no-daemon
```

Runtime validation still requires launching the client and checking the active shader pack manually.
