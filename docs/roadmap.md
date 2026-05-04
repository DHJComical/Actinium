# Actinium 开发计划

最后更新：2026-05-04

---

## 当前状态

MakeUp Ultra Fast 9.4c 在 Cleanroom Loader 环境下可用，核心管线（gbuffers → shadow → prepare → deferred → composite → final）已完整跑通。当前实现是兼容优先的保守方案，部分路径尚未对齐 Iris 标准。

---

## 立刻（当前冲刺）

优先级最高，阻塞后续兼容测试的问题。

**修复 mc_midTexCoord 正则，覆盖 vec4 声明**

当前 `MC_MID_TEX_DECLARATION` 只匹配 `vec2`，BSL 使用 `vec4` 声明，导致声明残留、编译报错。将正则中的类型匹配改为 `\S+`，preamble 注入类型升为 `vec4`，赋值时补零扩展为 `vec4(MidTexCoord, 0.0, 1.0)`。

**开启 JUnit，建立 GLSL 变换测试基线**

在 `gradle.properties` 中设置 `enable_junit_testing=true`，为 `ActiniumLegacyChunkShaderAdapter.translate()` 的核心变换路径写第一批单元测试。mc_midTexCoord 修复作为第一个用例，防止回归。后续每次修改变换逻辑都同步补测试。

**拆分 ENABLE_EXTERNAL_TERRAIN_REDIRECT，单独开启 translucent**

当前整个 terrain redirect 开关关闭。新增 `ENABLE_EXTERNAL_TRANSLUCENT_TERRAIN_REDIRECT = true`，solid/cutout 保持关闭。`bindTerrainPassFramebuffer` 和 `unbindTerrainPassFramebuffer` 按 pass 类型分支判断，让 `gbuffers_water` 正式进入 shader 管线。

**drawBuffers 从硬编码改为按 program 动态解析**

`resolveTerrainPassDrawBuffers` 当前硬编码返回 `[colortex1, gaux4]`，BSL 和 Complementary 的 `/* DRAWBUFFERS:XX */` 各不相同。在编译阶段解析每个 program 的 drawBuffers 声明，存入 program 元数据，运行时按 pass 查表返回。

---

## 中期（BSL / Complementary 兼容前）

**用 BSL 或 Complementary 跑第一次兼容测试**

以 BSL 为目标，记录出错的 program 和阶段，建立第二个光影包的问题清单。每修复一个问题同步补测试，防止 MakeUp 回归。

**实现光影包 GLSL 快照测试**

将每个光影包关键 program 经过 `translate()` 后的 GLSL 源码作为快照文件提交进 git。每次修改变换逻辑后自动对比快照，变化必须主动确认才能更新。MakeUp 和 BSL 各建一份快照基线。

**拆分 ActiniumTerrainPhase**

将 terrain redirect 的绑定/解绑/drawBuffers 逻辑从 `ActiniumRenderPipeline`（当前 6281 行）移入独立的 `ActiniumTerrainPhase` 类。RenderPipeline 只做调用，便于后续单独测试和修改 terrain 逻辑。

**提取 ActiniumGlStateGuard**

GL 状态保存/恢复逻辑散落在 RenderPipeline 各处，是状态泄漏的主要来源。提取为实现 `AutoCloseable` 的独立类，用 try-with-resources 包裹每个 pass，彻底消除 FBO、texture unit、blend/depth 状态的意外泄漏。

**prepare / deferred 时序继续向 Iris 标准靠拢**

当前实现是兼容折中：`prepare` 在 terrain 前运行，`deferred` 在半透明前运行，buffer ownership 保守。逐步推进：shadow-to-prepare 时机对齐、deferred 的 scene/depth 输入更精确、buffer ownership 更接近 Iris `CompositeRenderer` 的逐阶段语义。

**建立兼容性矩阵文档**

在 `docs/` 中维护光影包 × 功能的验证矩阵（terrain / shadow / composite / deferred / TAA / 天气 / 粒子）。每次发版前必须手动跑一遍已标 ✅ 的组合，明确哪些是承诺支持、哪些是 known limitation。

---

## 长期（管线稳定后）

**solid / cutout terrain 完整 gbuffer 路径**

接通外部 terrain program，提供 material/normal/specular data。当前保守方案回避了大量回归，待 translucent redirect 稳定、测试覆盖足够后推进。需要与 Celeritas 的 chunk vertex format 对齐。

**colortex8+ render target 支持**

突破当前 8 个 color target（`colortex0~3` + `gaux1~4`）的限制，支持 `colortex8` 以上的 target，覆盖更多现代光影包的需求。涉及 `ActiniumPostTargets` 的 FBO 分配和 `ActiniumWorldTargets` 的 sampler 绑定。

**改进 buffer flip 语义**

当前基础 flip 可用，复杂多 pass 光影包存在从 ping-pong 错误一侧读取 stale data 的风险。参考 Iris `BufferFlipper` 实现更精确的 flip tracking，修复 history target 在 TAA 场景下的边界问题。

**VintageHorizons 兼容层**

检测 VintageHorizons 是否加载，参考现代 Distant Horizons + Iris 协议实现 `dh_terrain` / `dh_water` program 接入。VintageHorizons 当前明确不兼容任何 shader mod，需要与上游协商接口或通过 Mixin 注入 LOD 渲染调用点。

**uniform 懒更新**

`ActiniumOptiFineUniforms` 当前每帧无条件上传所有 uniform。为变化频率低的 uniform（`viewWidth`、`viewHeight`、投影矩阵等）引入 dirty flag，只在值变化时调用 `glUniform*`，减少多 program 场景下的无效 GPU 通信开销。

---

## 架构（持续推进）

这些改动没有硬截止，但越早做、上游同步和后续维护越省力。

**把 Actinium 新增类严格归入 com/dhj/actinium**

当前 `org/taumc/celeritas/` 下混有 Actinium 新增的 Mixin（如 `EntityRendererActiniumColorStateMixin`）和上游 Celeritas 的文件。目标：`org/taumc/celeritas/` 只放从上游同步的文件，一字不改；所有 Actinium 新增行为通过 Mixin 注入，放在 `com/dhj/actinium/` 下。这样 `git diff origin/upstream` 就能精确看到两者的差异，cherry-pick 上游修复不再需要人肉 diff。

**RenderPipeline 显式阶段状态机**

当前管线阶段流转靠布尔开关和运行时 if，没有显式建模。引入 `ActiniumPipelinePhase` 枚举和 `transition()` 方法，非法转换（如在 SHADOW 阶段触发 COMPOSITE）在转换时自动捕获，phase exit/enter 负责清理和初始化，替代散落各处的手动状态保存。

**GLSL 变换从正则迁移到 glsl-transformation-lib**

当前 `ActiniumLegacyChunkShaderAdapter` 用正则和字符串替换做 GLSL 变换，对边界情况（注释内的声明、宏保护的代码块、多行声明）处理脆弱。迁移到 `glsl-transformation-lib`（embeddedt 开发，已在 Angelica 中使用），基于 AST 做结构化变换，稳健性和可维护性大幅提升。

---

## 不在计划内

- **Compute shader / image load store**：依赖 OpenGL 4.3+，1.12.2 生态的硬件兼容性有限，暂不支持
- **Nothirium 兼容**：与 Celeritas 的 chunk 渲染架构互斥，不支持
