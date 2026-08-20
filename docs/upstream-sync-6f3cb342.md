# Celeritas 上游同步记录：6f3cb342 之后

## 同步目标

- 上游仓库：`D:/Code/celeritas-mirror`
- 基准 SHA：`6f3cb3424d4c96afadd7b50356b24d016823e067`
- 评估终点：`0a3624bc2ba5bb28b01ccbbc6d185fa9247bddcf`
- 提交范围：基准之后到评估终点，共 34 个线性提交
- 目标项目：Minecraft 1.12.2 / Cleanroom Loader 的 Actinium
- 本轮状态：已建立同步记录；低风险同步批次已完成编译与检查，运行时验证和后续专项仍待处理

## 分支与日期

- 分支：`codex/sync-celeritas-upstream-6f3cb342`
- 日期：2026-08-20（Asia/Shanghai）
- 工作约束：同步按明确 SHA 和文件范围进行；机械同步、平台适配和运行时验证分阶段完成

## 已阅读的规范文件

- `AGENTS.md`
- `CONTRIBUTING.md`
- `docs/architecture.md`
- `docs/compatibility-matrix.md`
- `docs/upstream-maintenance.md`

本次评估遵循以下原则：Actinium 面向 1.12.2 / Cleanroom，不能直接照搬现代 Fabric、Forge 或现代 Minecraft 实现；子项目依赖方向保持 `GTNHLib <- glsm <- celeritas-common <- shader <- 根项目`；子项目不得依赖 `com.dhj.actinium` 实现类；Mixin 只负责注入，业务逻辑放在 Mixin 包外；不使用反射实现业务逻辑或测试；所有文件保持 UTF-8 无 BOM。

上游同步按明确 SHA 和文件范围进行，机械同步、1.12/Cleanroom 适配、LWJGL 适配以及 Mixin/bridge 适配分开处理。不能用整目录覆盖代替逐文件核对，也不能把编译通过当作运行时兼容性验证。

## 提交分类

以下分类覆盖范围内的全部 34 个提交，每个提交只列入一个分类。SHA 使用完整值，短 SHA 仅用于标题中的快速检索。

### 已开始同步

本分类表示已经锁定为首批同步范围。实际代码改动与验证结果在文档末尾的阶段记录中维护。

- `3ad8610b771553b1654dcffb92554e5424da63c7` — **Remove enum array allocation to reduce noise in profilers**。同步 `ChunkUpdateType.VALUES` 缓存及 `VisibleChunkCollector` 的调用点，当前代码结构接近上游，改动小、风险低，适合作为首个机械同步提交。
- `db1077091a64c7149abedf33ce334df01b9b78cb` — **Simplify legacy-support polyfills (#28)**。本轮只摘取 `BufferMapRangeFunctions.MAP_FULL_AND_SLICE` 的 null 处理，以及 `BufferCopyFunctions.PIXEL_PACK` 失败时的 `finally` 清理，确保 mapped buffer 解除映射并清理 binding。不上游整体删除 Actinium 自有的 `BufferStorageFunctions`、`MultidrawFunctions` 或其他旧驱动 fallback；这部分必须按本地 GLSM/LWJGL 结构手工合并。
- `0203a1fe96d07893ff76fe7146b0f1f39794c23c` — **dont compile chunk age declarations if theyre unused (#26)**。将上游 `assets/sodium` shader 的条件声明手工映射到 Actinium 的 `assets/actinium` opaque chunk shader；只有 `USE_FOG` 且 `CHUNK_FADE_IN_DURATION_MS > 0` 时才声明/读取 chunk age varying 和 uniform，避免无 fade 路径的链接告警与 uniform 数据分配。

### 建议后续同步

这些提交有潜在价值，但当前没有足够的直接调用场景，或应等待相关功能需求出现后再同步。

- `4afdabd23a81b9ebb9aeee1594e6354d826b3c26` — **Support instanced vertex attribute bindings (#27)**。上游扩展顶点属性 binding、`GlProgram` 和 tessellation API。Actinium 的 GLSM/backend 已有等价的 `glVertexAttribDivisor` 能力，但 Celeritas 当前没有明确的实例化绘制调用；后续若出现实际 caller，再核对 `com.mitchej123.lwjgl` wrapper 和本地 attribute 绑定路径。
- `1948aef10fc4a182fa305e0a7628d45264771a04` — **Add additional GL methods to LWJGL wrapper**。增加 `glDrawArrays`、纹理、draw buffers、framebuffer blit 等 wrapper API。Actinium backend 已存在部分等价底层能力，当前未确认 Celeritas 有对应直接调用；暂不为追上游而单独扩大 LWJGL service 变更面。

### 需手工适配

这些提交有明确的功能或性能收益，但与 Actinium 的本地结构、1.12.2 API、LWJGL service 或 shader 资源路径存在差异，不能直接 cherry-pick。

- `fadd0c40fadd7d3d26278d5ab4b377a7c5cc7256` — **Decouple chunk task dispatch rate from FPS**。将固定每帧任务预算改为依据 worker 吞吐和 starvation 调整 in-flight target，涉及 `ChunkBuilder`、`ChunkJobQueue`、`RenderSectionManager`、`RenderListManager`、`VisibleChunkCollector` 等调度语义。Actinium 的 `RenderSectionManager` 及 shadow pass、扩展世界高度有本地差异，需要单独压测初始建图、低 FPS、任务取消、资源重载、维度切换和 shadow pass。
- `208127b4a5b754584bd81bceac3417df8509fdfa` — **Fix GlFence.sync calling API with bogus parameters**。上游修复的是 `glWaitSync(id, flags, timeout)` 的参数；Actinium 的 `GlFence` 使用 `glClientWaitSync`，其返回值和等待语义被 `MappedStagingBuffer` 的 GPU upload 回收使用。两者不是同一个 API，不能直接同步；后续必须先确认本地是否存在真正的 `glWaitSync` 调用，再独立设计适配。
- `3d071b85205db183f54d30b7b36fe54ea096a2a2` — **Implement nearly-correct bilinear interpolation for ambient occlusion**。改变 chunk vertex ABI、AO 数据和 shader varying，涉及 compact/vanilla-like vertex format、`ChunkVertexEncoder`、mesh builder 及 terrain shader。Actinium 当前 stride 和 shader 资源均有本地差异，还要检查 `ExtendedChunkVertexType`、Iris 扩展顶点、triangulated pass 和旧版 GLSL。
- `7de8b00c0647ed169001d8ea889efafc81044264` — **Support EXT_timer_query for ancient drivers, and remove nested query support**。上游从 timestamp query 改为 `GL_TIME_ELAPSED` begin/end，并增加 `EXT_timer_query` wrapper。Actinium 当前 `TimerQueryManager` 使用 timestamp query，且 chunk、frame、Iris composite/finalize 计时区间存在嵌套可能；直接同步会改变计时器契约并触发非法嵌套，因此本轮不直接同步，后续需先设计计时层级并补齐本地 LWJGL wrapper。
- `0a3624bc2ba5bb28b01ccbbc6d185fa9247bddcf` — **Make bilinear interpolation slightly faster (#33)**。这是 AO 提交的后续优化，继续修改 `ChunkMeshBufferBuilder`、`ChunkVertexEncoder`、`XHFPTerrainVertex` 和 shader，必须与 `3d071b85` 成对手工适配，不能单独 cherry-pick。

### 成组重构

这些提交共同改变核心数据布局或可见性算法，必须作为完整专项评估和 benchmark 处理，不能零散同步其中一个优化提交。

#### MultiDraw / section render data 链

- `a3b3f98a9454364a89c8be4ddd8626132ca96e14` — **Aggressively optimize multidraw command emission**。引入 `BatchAssembler`，大幅调整 `SectionRenderDataStorage`、`SectionRenderDataUnsafe`、direct/indirect emitter 和 `DefaultChunkRenderer`。
- `2119366eb112576e0fcb1c68201aba1bb9649825` — **coalesce more adjacent multidraw commands**。继续改变 MultiDraw command 合并规则，依赖前一个 emitter/data layout 变化。
- `ee1e803864c9cf7e473ce97d23fc4a67737d694d` — **compact unsorted render data**。压缩 unsorted render data 的存储和读取路径，影响 section storage 与 render list。
- `8a7af9672802a71790b3e42de86fdbe4eb63885b` — **Refactor Strategy APIs a bit**。重构 `SectionRenderDataUnsafe.Strategy` API，必须和新的存储布局一起评估。
- `a517fb5cd3eb2ace01c9faf320662ede4fa9493c` — **Apply a few more cleanups to the SectionRenderDataUnsafe.Strategy API**。对同一 Strategy API 链做后续清理，不能脱离前四个提交独立同步。

Actinium 的 `DefaultChunkRenderer`、MultiDraw emitter、render bridge 和 attribute binding 有本地改动。后续需要同时验证 sorted/unsorted pass、direct/indirect fallback、animated section、LWJGL2 多重绘制路径以及 shader 的 region offset/draw ID。

#### Occlusion / lattice 链

- `94d5587030750e8bd8c0d3509467dfdac2e814bc` — **Create packed metadata for sections**。引入 section packed metadata，为后续数组化 lattice 和热路径访问提供基础；Actinium 当前没有对应 `PackedSectionMetadata`。
- `c3d030bcf366ddfb0d06ef1172aa93291e2cf19f` — **Add APIs for getting more accurate frustum information**。扩展 viewport/frustum 信息，为 region cull cache 提供边界判断依据。
- `094d4a21be692093c680204070d3e5b849da0e92` — **Cache frustum results for the whole region when possible**。引入 region 级 frustum cache，改变 `OcclusionCuller` 和 `RenderListManager` 的可见性路径。
- `450dc85f594ee8fac16197f708071a35b0f8c7f9` — **Implement a better storage system for occlusion node data**。以 `SectionLattice` 等结构替换对象化 `OcclusionNode` 数据，涉及多个 render-list、section 和 culler 类，是主要架构切换点。
- `066cda9d90253e47b3071abcf99b0e9fd5c6577d` — **Improve docs and simplify region cull cache**。继续调整 packed metadata 和 region cull cache 的契约，依赖前置存储改造。
- `a0d478d012fa3c4197d5c192c1f6ca936d9db302` — **Optimize rebasing the lattice**。优化 camera-centered lattice rebasing，要求重新验证 section 坐标、边界和异步任务生命周期。
- `841096aac6c836b838002bb5b9874988f229dc00` — **Unroll and inline some neighbor-visiting code in the hot loop**。对 lattice 邻居遍历热循环做展开和内联，只能在 lattice 结构稳定后评估。
- `244dae560820339161977b020c9b2e56d1f69f33` — **Remove visit buffering**。移除访问缓冲并调整 `OcclusionCuller`/collector 交互，可能改变异步可见性快照的生命周期。
- `c4208b3604ff19ac4b72db6887e17b3ba6516dda` — **Fix race conditions when reading visibility state**。修复异步读取 visibility state 的竞态，是该链条的正确性修复，不能只同步前面的性能优化而跳过。
- `8273d92807ca59bff220aff0e613ee219b205c6c` — **Assert the number of update types fits in 3 bits**。为 packed metadata 的 3-bit update type 假设增加断言；只有随 packed metadata/lattice 链一起同步时才有实际意义。

Actinium 当前仍使用 `Long2ReferenceMap<OcclusionNode>` 和旧的异步 graph search 生命周期；同时近期加入了 `DepthsUpdateCompat`，支持动态 `minSection/maxSection` 和非 vanilla 世界高度。迁移 lattice 时必须重新验证 Y 维度、sentinel、数组索引、camera rebasing 和 visibility snapshot 发布，不能按 vanilla `0..15` section 范围直接套用。

### 跳过

这些提交属于仓库元数据、现代平台专用修复、已经完整 revert 的移动，或 Actinium 没有对应实现，当前不产生可独立同步的收益。

- `faec61d93eea123a6a0f1d04b6c4e9c8bd173a80` — **Remove obsolete files**。删除上游 `Jenkinsfile` 和 `versions.json`，属于上游仓库元数据，Actinium 无对应同步需求。
- `691f34d9db1e33bbea827d9912f87704d2a6bc04` — **Make target property names globally unique**。修改 Stonecutter target 属性名和上游构建配置，Actinium 不使用同一 target 配置。
- `551538575ce9db5d4aabdbdac47e4049df8eefa9` — **Disable all subprojects by default for local builds**。调整 Stonecutter/CI 本地构建行为，和 Actinium 的 Gradle 多项目配置不对应。
- `45640b5cd729340ecba05379e9d14f029e92861d` — **Update the README**。仅更新上游 README 文本，不影响 Actinium 代码或兼容性。
- `a0a915e3b6649d58a97f950bc15b7d3b37392acb` — **Fix Fabric build crashing at runtime due to broken BlockColors mixin**。Fabric 专用 BlockColors Mixin，Actinium 面向 1.12.2/Cleanroom，平台入口和类加载条件不同。
- `7d0eeed5e497e0e70875555d29c2542a339c411d` — **Fix import**。属于上述 Fabric BlockColors 修复链的导入调整，目标平台不适用。
- `7df5a37920ac83e221e9c234458eb0c7c9e70aa3` — **Move stareval and parsing classes to common**。随后被完整 revert；最终没有应同步的净功能变化。
- `ac59a78dc75fcf1d9e78eabdc86e96298be3861c` — **Revert "Move stareval and parsing classes to common"**。完整撤销前一个移动提交，不能作为独立功能同步。
- `7704538a2367df614fc494d1c48a98451ef774fc` — **Extend the region frustum optimization to Iris frustums**。依赖上游现代 Iris shadow frustum 类；Actinium 没有同一套 frustum 实现，不能直接复制。

## 已落地同步范围

当前已落地的低风险同步范围如下。所有代码均保留 Actinium 的本地 fallback 和资源命名空间：

1. 完整评估并同步 `3ad8610b771553b1654dcffb92554e5424da63c7`：
   - `celeritas-common/src/main/java/org/embeddedt/embeddium/impl/render/chunk/ChunkUpdateType.java`
   - `celeritas-common/src/main/java/org/embeddedt/embeddium/impl/render/chunk/lists/VisibleChunkCollector.java`
2. 从 `db1077091a64c7149abedf33ce334df01b9b78cb` 只同步两项行为：
   - `BufferMapRangeFunctions` 的 null 处理；
   - `BufferCopyFunctions` `PIXEL_PACK` 失败路径的 `finally` 清理，确保 mapped buffer 解除映射并清除 binding。

不会从 `db107709` 一并删除 Actinium 现有的 legacy-support fallback；`BufferStorageFunctions`、`MultidrawFunctions`、`MappedStagingBuffer` 和 `GLRenderDevice` 的其余差异需单独审阅。

3. 手工适配 `0203a1fe96d07893ff76fe7146b0f1f39794c23c`：
   - `src/main/resources/assets/actinium/shaders/blocks/block_layer_opaque.vsh`
   - `src/main/resources/assets/actinium/shaders/blocks/block_layer_opaque.fsh`
   - 仅在启用 fog 且 chunk fade duration 大于 0 时声明 `v_ChunkAgeMs` 和 `celeritas_ChunkAges`，保持 `DefaultChunkShaderInterface` 的 optional uniform 绑定契约。

## 不直接同步的提交

### `208127b4`

上游改动面向 `glWaitSync`，Actinium 的 `GlFence` 使用 `glClientWaitSync`。后者由 staging buffer 上传和 GPU 回收路径使用，直接套用会改变同步模型。因此本轮不直接同步该提交，也不把它当作同名方法的机械修复；后续先查明本地是否有 `glWaitSync` 正常入口，再决定是否做独立适配。

### `7de8b00c`

上游使用 `GL_TIME_ELAPSED` begin/end query，并加入 `EXT_timer_query` 支持，同时移除 nested query。Actinium 当前 `TimerQueryManager` 使用 timestamp query，且 chunk render、frame render、Iris composite/finalize 之间存在嵌套计时区间。两套模型的嵌套契约不同，故本轮不直接同步；后续需先明确计时器层级，再修改本地 `LWJGLService`、LWJGL2/LWJGL3 wrapper 和 profiler 实现。

## 验证与后续拆分计划

### 当前验证状态

- 已落地同步修改 4 个 `celeritas-common` Java 文件和 2 个 Actinium shader 资源文件，未修改 GLSM、Mixin 或其他模块。
- `compileJava --no-daemon` 已通过，使用 Java 25 和 `GRADLE_USER_HOME=D:/gradle`。
- 最近一次 `check --no-daemon` 已通过：包含 `processResources`、根项目测试、模块边界、compat bridge Jar 和 remap Jar 校验；共 24 个 actionable tasks。
- `git diff --check` 已通过，已落地变更文件确认 UTF-8 无 BOM。
- 尚未运行完整 `build`，也尚未进行客户端运行验证、光影包验证、维度切换、资源重载或条件模组验证。

### 后续顺序

1. 已完成 `3ad8610b` 的机械同步并通过 `compileJava`、`check`。
2. 已完成 `db107709` 两项 buffer 行为修复，保留本地 fallback，并通过 `compileJava`、`check`。
3. 对 `fadd0c40` 建立独立调度性能专项，验证低 FPS、初始建图、rebuild 取消、维度切换、资源重载、render distance 变化和 shadow pass。
4. 将 `3d071b85` 与 `0a3624bc` 作为一个 AO ABI 变更批次处理，检查 compact/vanilla-like vertex stride、`ExtendedChunkVertexType`、普通 terrain、Iris terrain、triangulated pass 和 shader pack。
5. 将 MultiDraw 五提交作为完整数据布局和发射器重构，先 benchmark 再决定是否移植。
6. 将 Occlusion/lattice 十提交作为完整可见性架构重构，优先解决动态世界高度、异步 snapshot 和 camera rebasing 的适配，再进行性能优化。
7. 所有渲染相关代码完成后，按项目规范运行 `compileJava`、`check` 和 `build --no-daemon`；Gradle 缓存统一使用 `D:/gradle`，并进行无光影、目标光影包、维度切换、资源重载和条件兼容模组的运行验证。

建议机械同步、1.12/Cleanroom 适配、LWJGL 适配以及 Mixin/bridge 适配分别提交，避免在同一个提交中混合上游大范围同步和无关重构。每个后续提交应记录来源 upstream SHA、实际文件范围、适配原因和验证结果。

## 许可证与同步边界

- 必须保留上游源文件的版权头、许可证文本和署名信息。
- 只要同步内容涉及第三方来源或许可证变化，就要同步检查并更新 `THIRD_PARTY_NOTICES.md`；不能根据包名推断许可证，也不能删除或统一改写版权头。
- 禁止整目录覆盖或无差别复制上游目录；必须基于明确 SHA 列出实际文件范围，逐文件处理平台、LWJGL、Mixin 和 bridge 差异。
- 不使用 `duplicatesStrategy = EXCLUDE` 掩盖同步造成的重复 class。
- 未完成运行时验证时，不把兼容状态标记为“已验证”。
- 后续提交须遵循 Conventional Commits 格式，并保持每个提交只处理一个清晰的同步或适配主题。
