# 同步上游 celeritas 方案（0a3624bc → b8c1079a）

> 状态：**已全部完成**。7 个上游提交全部收编，各阶段独立提交、编译 + `check`（423 测试）通过。
> dev 运行验证（阴影剔除运行时表现 + JMH 基准 + 上一轮待验证专项）待执行。

## 完成记录

| 阶段 | commit | 内容 |
|---|---|---|
| 1+2 | `099a74f` | FFC + 快照/RegionCullCache 对齐 |
| 3a | `d8dde88` | 阴影剔除算法/数据类（ShadowOcclusionCuller/ShadowSearchFrustum/SectionLattice/OcclusionCuller/Viewport） |
| 3b | `106b7e3` | 渲染器架构重构（SectionGraph/updateForShadowPass + 本地双 manager 适配） |
| 4 | `f375bab` | Iris 阴影剔除激活（setupShadowTerrain + AdvancedShadowCullingFrustum 实现 ShadowSearchFrustum） |
| 5 | `16eaec9` | JMH 基准（LWJGL 3.4.1 适配，含 GL bench） |

协调风险已解决：与 `fix/issue64-ntm-glsm`（HBM seam）merge 预演无冲突，FFC + seam 共存。

## 遗留验证项（C 类）

- 阴影剔除运行时表现（MakeUp/BSL/Complementary 光影包）
- JMH 基准运行（需 EGL 环境：Linux Mesa/llvmpipe 或 Windows Mesa3D）
- 上一轮 `6f3cb342.md` 列出的待运行验证专项（fadd 调度、AO ABI、MultiDraw、Occlusion/Lattice、Iris frustum 三态）

## 目标

将 `celeritas-common/` 从上次同步基线 `0a3624bc`（对应 Actinium commit `771f1c5`）推进到
上游镜像 stonecutter 分支 HEAD `b8c1079a`，共 **7 个上游提交**，全部收编（含阴影剔除）。

## 上一轮同步遗留待办（docs/upstream-sync-6f3cb342.md 核对结果）

上一轮同步（`6f3cb342.md`，终点 `0a3624bc`）的所有"已落地"代码已确认在当前代码树中
（AO ABI stride 24/32 + `a_RdhFactor`、fadd 自适应调度、`PackedSectionMetadata`、
Occlusion/Lattice 十提交等均存在）。起点干净，无代码遗漏。

但上一轮明确遗留了以下**待办项**，建议与本轮同步一并评估：

### A. 上一轮"不直接同步"的提交（需独立评估，非本轮 7 提交范围）
- `208127b4` — **Fix GlFence.sync calling API with bogus parameters**。上游面向 `glWaitSync`，
  Actinium 的 `GlFence` 用 `glClientWaitSync`。需先查明本地是否有 `glWaitSync` 正常入口再决定。
- `7de8b00c` — **Support EXT_timer_query / remove nested query**。上游改 `GL_TIME_ELAPSED`
  begin/end；Actinium `TimerQueryManager` 用 timestamp query 且有嵌套计时区间。需先明确计时器层级。

### B. 上一轮"可选后续"提交（有实际调用方/需求时才做）
- `4afdabd2` — **Support instanced vertex attribute bindings (#27)**。Actinium GLSM 已有等价能力，
  当前无实例化绘制调用。
- `1948aef1` — **Add additional GL methods to LWJGL wrapper**。需确认本地有无对应直接调用。

### C. 上一轮"待运行验证"专项（代码已落地，缺客户端运行验证）
- `fadd0c40` 调度：初始建图、低 FPS、任务取消、资源重载、render distance 变化、扩展世界高度、shadow pass。
- AO ABI：legacy OpenGL、特定 Compact/Vanilla-like format、translucent/triangulated 细分。
- MultiDraw 五提交：legacy OpenGL/LWJGL2、direct/indirect/individual 切换、动画 section。
- Occlusion/Lattice 十提交：初始建图、负 Y/扩展高度、camera 大幅移动/rebase、异步 occlusion 开关、
  render distance 变化、资源重载、shadow pass、低 FPS。
- Iris frustum 三态：region cull 实际收益、advanced/safe-zone shadow culling、各光影包组合。

> 建议：C 类运行专项与本轮阶段 3/4（阴影剔除）的验证可合并执行，避免重复起客户端。

## 上游提交清单（0a3624bc..b8c1079a）

| # | commit | 内容 | 可收编性 |
|---|---|---|---|
| 1 | `f3c5642e` | JMH 基准（新增 bench + build.gradle） | 独立，最后收编 |
| 2 | `9b4256d0` | fast frustum clamping（新增 FastFrustumClamping） | 内部，可直接收编 |
| 3 | `daaefb72` | 微优化（FastFrustumClamping + OcclusionCuller） | 内部，可直接收编 |
| 4 | `7c7c8228` | portal 角度细化（OcclusionCuller） | 内部，可直接收编 |
| 5 | `d96b07f3` | 多根 BFS 时禁用 FFC（OcclusionCuller） | 内部，可直接收编 |
| 6 | `8fdbdb49` | 激进优化（快照/RegionCullCache/FFC 重构） | **需适配**（与本地分叉冲突） |
| 7 | `b8c1079a` | receiver-driven shadow occlusion culling（大改） | **需重大适配** |

## 上游改动文件

### common 侧（12 个 main 文件）
- **新增**：`SectionGraph`、`FastFrustumClamping`、`ShadowOcclusionCuller`、`ShadowSearchFrustum`
- **修改**：`RenderSectionManager`、`RenderListManager`、`OcclusionCuller`、`RegionCullCache`、
  `SectionLattice`、`SimpleWorldRenderer`、`Viewport`

### 非 common 侧（modern Iris，需单独评估）
- `modern/.../WorldRendererMixin.java`
- `modern/src/main/shaders_java/net/irisshaders/iris/shadows/ShadowMatrices.java`
- `.../shadows/ShadowRenderer.java`
- `.../shadows/frustum/advanced/AdvancedShadowCullingFrustum.java`
- `.../frustum/advanced/ReversedAdvancedShadowCullingFrustum.java`
- `gradle.properties`（JMH `jmh_version`）

## Actinium 本地分叉（同步时的适配点）

勘察确认 `771f1c5` 之后 Actinium 的 `celeritas-common` 相对上游已有实质分叉：

1. **`SectionLattice.VisibilitySnapshot`**：Actinium 仍为 `long[] visitState` 全量拷贝 + 单套
   `snapshotBuffers`；上游 `8fdbdb49` 改为 `int[] visibleFrames` 半内存快照 + `SnapshotBuffers`
   双缓冲，`b8c1079a` 再扩展成 main/shadow 两套。**冲突，需按新语义重写而非 diff 移植**。
2. **`RegionCullCache`**：Actinium 为 stamp 版（`currentStamp` 自增 + `stamp[regionId] != currentStamp`
   判定）；上游为 reset 版（`begin()` 里 `Arrays.fill(classification, UNCOMPUTED)`），并新增
   `PARTIAL_DISTANCE_IN`/`PARTIAL_FRUSTUM_IN`/`cached()`。**冲突，需收敛到上游 reset 版**。
3. **`OcclusionCuller`**：Actinium 有 HBM-CE `OcclusionNode` seam（`fcdde1e`），加在
   `isWithinFrustum(Viewport,int,int,int)` 重载之上。上游 7 提交**未删除**这些成员，seam 不受影响；
   但 `b8c1079a` 把 `ANGLE_REFINEMENT_MASKS`/`isVisibleInPartialRegion`/`regionOrigin` 等从 `private`
   改为包可见（`ShadowOcclusionCuller` 需 static import）——需同步放开，注意不与 seam 靶点重名。
4. **`RenderSectionManager`**：Actinium 本地「双 RenderListManager + `getCurrentRenderListManager()`
   + `isInShadowPass()`」结构（20+ 处调用）。上游 `b8c1079a` 改为 `SectionGraph` 单搜索线程 +
   `updateForShadowPass`。**架构冲突**，需决策：保留本地 `getCurrentRenderListManager()` 抽象作为
   缝合层（把两个 manager 改为共享一个 `SectionGraph`），还是完全采纳上游结构。
5. **`SimpleWorldRenderer`**：Actinium 本地「shadow 时切换 render distance
   （`getShadowEffectiveRenderDistance()`/`setRenderDistance` 恢复）」+ `setWorld` 用
   `ChunkTracker.Subscription`。上游新 `setupTerrain`/`setupShadowTerrain` 无对应物，需保留本地增强。
6. **`nearestToZero`**：Actinium 内联成私有方法（上游是 `MathUtil` 静态导入）。覆盖后恢复。

## 阶段计划（每阶段独立提交）

### 阶段 1：OcclusionCuller/FFC 内部优化（9b→daa→7c→d96 + 8f 的 FFC 部分）
- **文件**：`OcclusionCuller`、`FastFrustumClamping`（新增）
- **风险**：低（纯内部，无公共 API 变化；OcclusionNode seam 不动）
- **注意**：`9b/daa` 里对 `RegionCullCache`/`SectionLattice` 的少量改动**暂不包含**（依赖阶段 2 快照
  重构），阶段 1 只收编 OcclusionCuller + FastFrustumClamping 部分
- **验证**：`./gradlew compileJava` + `check`

### 阶段 2：快照/RegionCullCache 对齐（8f 的前置）
- **文件**：`SectionLattice`、`RegionCullCache`
- **风险**：中（BFS 帧戳语义、多根 FFC 禁用逻辑依赖 `VisibilitySnapshot.isSectionVisible`）
- **做法**：把 Actinium `long[]` 快照收敛到上游 `int[] visibleFrames` + `SnapshotBuffers`；
  `RegionCullCache` 从 stamp 版收敛到 reset 版（含 `PARTIAL_*`/`UNCOMPUTED`/`cached()`）
- **验证**：编译 + `check`；dev 目测可见性无回归

### 阶段 3：b8c1079a common 侧（SectionGraph/ShadowOcclusionCuller 等）
- **文件**：`SectionGraph`、`ShadowOcclusionCuller`、`ShadowSearchFrustum`（新增）；
  `RenderListManager`、`SectionLattice`、`OcclusionCuller`、`Viewport`、`RenderSectionManager`、
  `SimpleWorldRenderer`（修改）
- **风险**：高（核心渲染器重构）
- **适配点**：本地双 manager 结构、render-distance 切换、HBM-CE seam、OcclusionCuller 静态成员放开
- **验证**：编译 + `check`；dev 目测地形/阴影渲染无回归
- **收编后阴影剔除默认「休眠」**：lattice 以 `hasShadowPass=true` 创建才启用接收者搜索；
  若 Actinium 不接入 `ShadowSearchFrustum`，`updateForShadowPass` 里 `lightVector` 恒 null，
  走 frustum-only 回退，功能等价现状

### 阶段 4：阴影剔除 Iris 接入（shader 模块，非 common）
- **文件**：`shader/src/main/java/net/coderbot/iris/shadows/frustum/advanced/AdvancedShadowCullingFrustum.java`
  （实现 `ShadowSearchFrustum`）、`shader/.../ShadowRenderer.java` 等
- **风险**：中（Actinium 的 Iris 是 1.12 vendored 版，需独立适配，不覆盖上游 modern 文件）
- **决策点**：是否让 1.12 shadow 帧走 `setupShadowTerrain`/接收者搜索
- **验证**：dev 用目标光影包（MakeUp/BSL/Complementary）验证阴影渲染

### 阶段 5：JMH 基准（f3c5642e）
- **文件**：`celeritas-common/build.gradle`（jmh sourceSet/配置/任务）、`gradle.properties`
  （`jmh_version=1.37`）、`celeritas-common/src/jmh/java/**`
- **风险**：低（构建无影响，`jmh` 任务仅在显式触发时执行）
- **验证**：`./gradlew :celeritas-common:jmh` 能跑（可选）；`check` 通过

## 提交拆分建议

每阶段一个 commit，Conventional Commits：
- `perf(celeritas): sync fast frustum clamping from upstream`（阶段 1）
- `refactor(celeritas): align section visibility snapshot with upstream`（阶段 2）
- `feat(celeritas): sync receiver-driven shadow occlusion culling`（阶段 3）
- `feat(shader): wire shadow occlusion search frustum`（阶段 4）
- `build(celeritas): add JMH benchmark harness`（阶段 5）

## 风险与回退

- 阶段 2/3 若引入可见性回归（方块闪烁、剔除错误），可单独 revert 对应阶段 commit（各阶段独立）。
- 阶段 3 改动面大，建议在确认前**先在 dev 环境目测**再合入。
- 非 common 的 modern Iris 文件（`net.irisshaders`）**不直接覆盖**，阶段 4 在 Actinium 自己的
  `shader/` 模块适配。
