# 同步上游 celeritas 方案（fe57c60fa → 5c68ed4cb）

> 状态：**同步完成**。上游 `fe57c60fa..5c68ed4cb` 共 11 个提交：7 个收编（其中 1 个连带
> 另一提交的部分内容），4 个不适用。本地产出 7 个代码 commit。
> 编译（根项目 + 全部子项目 + compatBridge）与全量测试通过。

## 完成记录

| 上游 commit | 本地 commit | 内容 |
|---|---|---|
| `1d92129e4` | `7a795943` | cloned section 缓存容量 512→2048（本地等价类 `com.dhj.actinium.world.cloned.ClonedChunkSectionCache`） |
| `aa453de16` | `46687628` | 缓存 cleanup 早退，避免每帧全量扫描 |
| `1a450d8ca` | `2ce590aa` | 自适应任务调度默认关闭（跟随上游：重载下帧率不稳） |
| `9269da16e` | `f5cbdef8` | region 级 dataRevision 计数器（5d10eba25 的编译前置） |
| `5d10eba25` | `9451fe0f` | sprite ticker 优化：region 级缓存 + revision 惰性失效 + 周期 prune |
| `ee5d140f3` | `ec04db27` | region arena 尺寸策略：惰性分配 + 分档增长；连带 `2334caab0` 的 `elementsFor` helper |
| `9c88ca3d9` | `60f3b800` | chunk shader 支持 EXP 雾模式（修复 EXP 被折叠为 EXP2 的渲染错误） |

## 上游提交清单（fe57c60fa..5c68ed4cb，stonecutter 分支）

| commit | 内容 | 处置 |
|---|---|---|
| `1d92129e4` | [modern] 缓存容量 512→2048 | 收编（`7a795943`） |
| `acb7d79fd` | [modern] 相邻任务共享 block 解包 | 不适用（见决策 2） |
| `b4f9f4315` | [modern] WorldSlice 泄漏修复 | 不适用（本地无该泄漏结构，见决策 3） |
| `aa453de16` | [modern] 缓存 cleanup 免全扫 | 收编（`46687628`） |
| `1a450d8ca` | 自适应调度默认关闭 | 收编（`2ce590aa`） |
| `9269da16e` | region revision 计数器 | 收编（`f5cbdef8`） |
| `5d10eba25` | sprite ticker 优化 | 收编（`9451fe0f`） |
| `2334caab0` | 多 vertex format 共享 arena | 不适用（见决策 4） |
| `ee5d140f3` | region arena sizing | 收编（`ec04db27`） |
| `9c88ca3d9` | EXP 雾模式 | 收编（`60f3b800`） |
| `5c68ed4cb` | 构建配置（config/build cache、插件升级） | 不适用（见决策 7） |

## 关键决策

1. **`1a450d8ca` 跟随上游默认关闭自适应调度**。上游理由"重载下帧率不稳"直接适用于我们的
   同源移植；本地附加防护（`MAX_FRAME_NANOS` 钳制、保守初值）只防初始阶段目标爆表，没有
   证据表明解决了上游所报的高负载帧率抖动。perf 打点不依赖该开关：关闭后 `chunk.scheduler`
   段的 `targetInFlight`/`sortsPerMesh` 退化为常量（可据此识别 legacy 模式），段本身仍输出。
2. **`acb7d79fd`（共享 block 解包，省至多 20% meshing）不适用**：其前提是 modern 的
   `PalettedContainer` 不可变快照 + `sodium$unpack` 模型；本地 `ClonedChunkSection` 直接引用
   live `ExtendedBlockStorage`（block 数据非快照，light 数据才有 eager 拷贝），`WorldSlice`
   的 `blockStatesArrays` 归 slice 私有、跨任务复用。等效收益需前置独立重构：EBS 深拷贝快照
   → section 内共享解包缓存 → copyData 改别名共享 → 复验本地 `#63`/`#126`/Fluidlogged/
   DepthsUpdate 改动。若要做应单独立项，并重测 1.12.2 逐块拷贝模型下的真实收益。
3. **`b4f9f4315`（WorldSlice reset 泄漏）不适用**：上游 bug 是 modern `reset()` 循环上限误用
   轴向长度导致 24/27 槽位引用残留；本地 light/TE 封装在 `ClonedChunkSection` 内由缓存生命周期
   管理，`reset()` 一次性 `Arrays.fill(sections, null)` 且每任务结束在 worker `finally` 中必被
   调用，无残留路径。
4. **`2334caab0`（多 vertex format 共享 arena）不适用**：本地地形管线同一时刻只有一种
   `ChunkVertexType`（`ActiniumWorldRenderer.chooseVertexType()` 单选后注入全部
   TerrainRenderPass），每 region 至多一个 DeviceResources，共享收益为零；而融合成本高——
   `DefaultChunkRenderer`/`BatchAssembler` 已被本地动画/MultiDrawMode/HBM-CE seam 重写，
   且需并行维护 CachedBatch-tessellation 生命周期。若长期策略改为低摩擦跟随上游结构，可另行
   做"对齐式收编"。
5. **`ee5d140f3` 独立套用，连带 `2334caab0` 的 `elementsFor`**：本地 `GlBufferArena` 为 2334
   之前形态，与上游 ee5d 基线的差异仅 `elementsFor`（整除上取整 helper + 两处调用点），一并
   带上；`RenderRegion` 两处构造调用点改用本地声明的 `MINIMUM_GEOMETRY_ARENA_BYTES`(512KiB)/
   `MINIMUM_INDEX_ARENA_BYTES`(64KiB)，不引用 2334 的常量。行为变化：空 region 不再按满配
   预占缓冲，首个上传才分配，resize 增加防抖（required*2 <= capacity 时只压缩）。
6. **`9c88ca3d9` 资源与平台映射**：内置 shader 在本仓库发布于 `assets/actinium/shaders/`
   （非上游 `assets/sodium/shaders`）；`block_layer_opaque.fsh` 的本地演化
   （`USE_FOG_POSTMODERN` 分支等）保留，只改 legacy 雾段。上游 modern `FogHelper` 的
   "GL_EXP 不再折叠进 EXP2" 语义在本仓库只落在 `ChunkFogMode.fromGLMode`。修复的实际缺陷：
   失明/水下等 `GL_EXP` 雾此前按 EXP2 曲线（`e^-(d·c)²`）渲染，正确曲线为 `e^-(d·c)`。
7. **`5c68ed4cb`（构建配置）不适用**：buildSrc/jvmdowngrader/stonecutter 均为上游专属；
   唯一可借鉴的通用项是 `org.gradle.caching=true`（可独立试验）；`configuration-cache` 与
   GTNH 系脚本的兼容性需专门验证，不随同步混入。
8. **兼容面**：本轮 7 个提交均不触碰 compatBridge 镜像类（`org.taumc.celeritas.*`）与
   HBM-CE 注入缝；`RenderSectionManager`/`DefaultChunkRenderer` 的本地扩展区域未被涉及。

## 遗留验证项

- **EXP 雾实机**：失明药水/水下场景、无光影与光影开启两态，与固定管线（glsm）雾效对比。
- **sprite ticker 回归**：动画纹理（水/岩浆/火）正常跳动；可用 perf 日志对比 ticker 耗时。
- **缓存 2048 容量内存观察**：本地条目比上游 modern 重（双 NibbleArray eager 拷贝 + Biome +
  TE map），峰值出现在 5 秒窗口内大量重建时，大渲染距离下留意堆占用。
- **调度器默认关闭后的回归**：`chunk.scheduler` 段退化为常量属预期；高负载（飞行/传送）下
  帧率稳定性应与上游结论一致地改善。
- 上一轮遗留（planar fog 观感、shadow pass、GUI 滑块、C 类运行专项）仍未执行。

## 下一轮提醒

- Celeritas 同步基准推进至 `5c68ed4cb`（2026-09-08）。上游镜像 `D:/Code/celeritas-mirror`
  与 git.taumc.org 上游 HEAD 一致。
- `acb7d79fd` 若立项做等效重构，需先复验 `WorldSlice`/`ClonedChunkSection` 的本地改动面。
- `2334caab0` 的对齐式收编窗口：若未来本地引入第二 vertex format 或上游出现依赖该结构的
  后续提交，再评估。
