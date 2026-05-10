# Actinium 开发计划

最后更新：2026-05-05（基于 `ffa0e1d` 代码阅读）

---

## 当前状态

MakeUp Ultra Fast 9.4c 在 Cleanroom Loader 环境下稳定运行。核心管线
（gbuffers → shadow → prepare → deferred → composite → final）已完整跑通。
本轮冲刺完成了 mc_midTexCoord 修复、translucent redirect 开启、drawBuffers 动态解析、
测试基线建立、TerrainPhase 拆分、GlStateGuard 提取共六项任务，所有架构目标也已完成。

下一个里程碑：**第一个第二光影包（BSL 或 Complementary）可运行**。

---

## 立刻

### 待完成

**用 BSL 或 Complementary 跑第一次兼容测试**

所有基础设施已就绪，这是现在唯一阻塞兼容性扩展的事。拿 BSL 8.x 或 Complementary
Reimagined 放进 `shaderpacks/`，记录所有出错的 program 和阶段，建立问题清单。
BSL 与 MakeUp 的主要差异点：`vec4 mc_midTexCoord`（已修复）、更多 drawBuffers
组合、`colortex8+` 写入路径、更复杂的 `prepare` 链。

**补全 MakeUp 高设置场景的手动验证**

文档指出以下场景尚未系统验证：体积云、水面反射、DOF、volumetric light、
雨雪粒子、飞溅粒子、雾过渡。BSL 测试前应确认这些在 MakeUp 上无回归，
避免把旧问题混入新包的问题清单。

**建立兼容性矩阵文档**

在 `docs/compat-matrix.md` 里按光影包 × 功能维度记录验证状态，
明确哪些组合是承诺支持、哪些是 known limitation。格式参考：

| 光影包 | 版本 | terrain | shadow | composite | deferred | TAA | 天气 | 最后验证 |
|--------|------|:-------:|:------:|:---------:|:--------:|:---:|:----:|---------|
| MakeUp Ultra Fast | 9.4c | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ffa0e1d |
| BSL Shaders | — | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | 未测 |
| Complementary | — | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | 未测 |

### 本轮冲刺已完成

- `ffa0e1d` mc_midTexCoord 正则修复（vec2/vec4 均覆盖）+ ActiniumTerrainPhaseTest 109 行
- `3ae5e17` translucent terrain redirect 拆分开启（`ENABLE_EXTERNAL_TRANSLUCENT_TERRAIN_REDIRECT = true`）
- `95a1a0e` drawBuffers 动态解析（`ActiniumShaderPackResources.readProgramDrawBuffers()`）
- `ab9b203` JUnit 测试基线（5 个测试文件，GlslTransformUtils 快照测试）
- `3df6a5d` ActiniumTerrainPhase 拆分 + ActiniumGlStateGuard 提取（340 行）
- `74c74d7` GLSL 变换迁移到 Maven AST 管线（glsl-transformation-lib）
- `e566a0d` 显式阶段状态机（ActiniumPipelinePhase 10 个阶段 + transitionPhase）
- `424b5ff` Actinium Mixin 归入 `com/dhj/actinium`，上游文件完全隔离

---

## 中期

BSL 兼容测试过程中同步推进。

**prepare / deferred 时序继续向 Iris 标准靠拢**

当前实现是保守折中：`prepare` 在 terrain setup 前执行，`deferred` 在半透明前执行，
buffer ownership 不够精确。需要推进的方向：shadow-to-prepare 时机对齐、
deferred 的 scene/depth 输入更精确、buffer ownership 逐阶段化。
文档明确指出这是中期目标。

**buffer flip 语义统一**

`explicitFlips` 机制已实现，`flipWrittenTargets` 也存在，但有两处手动
`flipTarget` 调用（第 2356 行 colortex1、第 2368 行 colortex8）游离在
统一机制之外，属于潜在的 ping-pong 错误来源。需要把所有 flip 路径收拢到
`flipWrittenTargets` 一套逻辑里，向 Iris `BufferFlipper` 对齐。

**开光影后的 terrain/world-stage 性能分析**

`ENABLE_EXTERNAL_TERRAIN_REDIRECT = false` 保证了稳定性，但 translucent redirect
开启后引入了额外的 FBO 切换和 blit 开销。需要量化这个开销，找出热路径，
在不重新打开不稳定 solid redirect 的前提下优化。

**RenderPipeline 进一步拆分**

TerrainPhase 拆出后 RenderPipeline 仍有 6479 行。可沿 phase 边界继续拆分：
shadow 渲染逻辑（约 1500 行，第 1537 行附近）、post program 调度（composite/deferred 链路）。
每次拆分跟着功能改动顺手做，不单独开大重构。

**补全测试覆盖**

当前覆盖薄弱的区域：`ActiniumLegacyFullscreenShaderAdapterTest` 只有 2 个用例，
`gl_FragData` 索引替换、`ftransform` 消除、多 varying 场景均未覆盖；
`ActiniumShaderPropertiesTest` 的 `explicitFlips` 解析、profile 切换未覆盖。
BSL 测试过程中发现的每个 bug 应同步补测试用例，防止回归。

---

## 长期

管线稳定、第二个光影包跑通后推进。

**solid / cutout terrain 完整 gbuffer 路径**

`ENABLE_EXTERNAL_TERRAIN_REDIRECT` 目前硬编码 `false`。开启需要：material/normal/specular
data 接入 Celeritas vertex format、solid terrain FBO attachment 路径验证、
与现有 translucent redirect 的共存测试。不要在 BSL 兼容完成前触碰这个开关。

**colortex8–15 完整写入路径**

当前状态：`PostTargets` 已定义全部 16 个常量，`PostShaderInterface` 已绑定 colortex8
sampler（第 190、328 行），`RenderPipeline` 已做 `bindPipelineTextureAliases`（第 2476–2483 行）——
读和采样已可用，但 gbuffers 阶段往 colortex8+ 写入的 FBO color attachment 路径尚未实现。
比"完全没做"近很多，是中等规模的补全工作。

**VintageHorizons 兼容层**

代码里已有 `SOFT_LOD_UNIFORM_PATTERN`（第 138–139 行）处理 `softLod` uniform 注入。
正式实现需要：检测 VintageHorizons 是否加载、参考现代 DH + Iris 协议实现
`dh_terrain` / `dh_water` program、与 `VintageRenderSectionManager` 的渲染调用对接。

**uniform 懒更新**

`GlUniform` 基类目前无 dirty flag，每帧无条件上传所有 uniform。
多 program 场景下开销可测量。低优先级，稳定后优化。

**compute shader / image load store**

依赖 OpenGL 4.3+，1.12.2 生态硬件覆盖率是瓶颈。优先级最低，暂不排期。

---

## 不在计划内

- **Nothirium 兼容**：与 Celeritas chunk 渲染架构互斥，不支持
- **compute / image load store**：GL 4.3+ 依赖，硬件覆盖率不足，暂不支持