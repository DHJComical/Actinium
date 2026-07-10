# Actinium

[简体中文](README-zh.md)

Actinium is an experimental rendering and shader compatibility mod for Minecraft 1.12.2 on Cleanroom Loader. It aims to bring a more modern rendering pipeline to the legacy client while keeping shader packs, classic modded content, and performance-oriented rendering work in the same world.

The project currently combines work around Celeritas, GLSM, GTNHLib, and an Iris-style shader pipeline. Its focus is practical compatibility: terrain rendering, entity rendering, shadow passes, post-processing stages, shader uniforms, framebuffer ownership, and the OpenGL state transitions that old and new renderers both depend on.

## What Actinium Does

- Reworks the Minecraft 1.12.2 client rendering path around a modernized pipeline.
- Integrates Celeritas terrain rendering concepts into the Cleanroom environment.
- Takes over and stabilizes GLSM-related OpenGL state handling.
- Implements an Iris-inspired shader pass model, including `shadow`, `gbuffers`, `prepare`, `deferred`, `composite`, and `final` stages.
- Improves compatibility with shader packs that expect modern framebuffer, uniform, and program-binding behavior.
- Keeps legacy modded rendering paths in view, including entities, block entities, particles, weather, sky rendering, water reflections, and shadow rendering.

## Current Status

Actinium is under active development. It is not a drop-in replacement for every Minecraft 1.12 rendering stack yet, and shader pack behavior can still vary by pack, preset, driver, and mod list.

The current development direction is compatibility-first:

- preserve expected vanilla and modded rendering behavior;
- make shader pack pipeline stages predictable;
- reduce hidden OpenGL state leaks between legacy rendering and shader rendering;
- keep regressions visible through focused tests and manual shader-pack checks.

Shader packs such as MakeUp, BSL, and Complementary are useful compatibility targets during development, but support should be treated as ongoing work rather than a finished compatibility matrix.

## Requirements

- Minecraft 1.12.2
- Cleanroom Loader
- Java 25 toolchain; produced bytecode targets Java 21
- A graphics environment capable of running shader packs used for testing

## Building

From the project root:

```powershell
.\gradlew.bat build --no-daemon
```

For a faster compile-only check:

```powershell
.\gradlew.bat compileJava --no-daemon
```

Install `build/libs/Actinium-<version>.jar` in a compatible Cleanroom instance. The
`-sources.jar` file is for development, and the unremapped `-all.jar` is not a runtime mod.

## Repository Layout

- `src/` contains Actinium integration, compatibility hooks, mixins, and runtime resources.
- `shader/` contains the integrated Iris-style shader pipeline.
- `glsm/` contains the embedded GLSM-side integration.
- `GTNHLib/` contains the embedded GTNHLib pieces used by the project.
- `celeritas-common/` contains the embedded Celeritas renderer implementation.
- `docs/` contains development notes, compatibility gaps, and future documentation.
- `gradle/scripts/` contains shared Gradle build and dependency logic.

See [the architecture guide](docs/architecture.md), [current roadmap](docs/roadmap.md),
and [compatibility matrix](docs/compatibility-matrix.md) before changing the render pipeline.
Third-party source provenance and licenses are summarized in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Related Projects

Actinium builds on ideas, code, and compatibility research from several projects. The items below are listed to make those roots visible; each upstream project remains governed by its own license and authorship.

- Celeritas: provides much of the original Minecraft 1.12 performance-mod foundation and terrain-rendering direction that Actinium continues to adapt.
- GLSM: provides shader-pack loading, shader state management, and compatibility behavior that Actinium integrates and stabilizes.
- GTNHLib: provides utility code and compatibility infrastructure used by the embedded legacy rendering stack.
- Iris: serves as a major reference for shader pipeline structure, pass ordering, framebuffer behavior, and shader-pack expectations.
- GLSL Transformation Library: used for GLSL parsing and transformation work needed by shader compatibility code.
- Sodium: informs the broader performance-oriented rendering model and modern Minecraft renderer design.
- Angelica and the GTNH rendering ecosystem: useful references for Minecraft 1.7/1.12-era shader compatibility, legacy OpenGL behavior, and modded-client integration.
- Cleanroom Loader: provides the target Minecraft 1.12.2 runtime environment.

## License

Actinium is distributed under the license in `LICENSE`.

Code originating from other projects, along with compatibility changes made to that code, remains under the original license of the respective upstream project unless explicitly stated otherwise in the relevant source files.
