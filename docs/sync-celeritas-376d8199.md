# 同步上游 celeritas 方案（b8c1079a → 376d8199）

> 状态：**同步完成**。2 个上游提交均已收编，`check` 全绿。JMH bench 部分跳过（本地 harness 已 revert）。
> 运行时回归（multidraw 三模式、动画模组组合、shadow pass 双缓存槽）为遗留 C 类验证项。

## 完成记录

| commit | 内容 |
|---|---|
| `54cc25f` | 2dc805c1：region 级 multidraw 批次缓存（CachedBatch + BatchCacheParams 双槽 + cameraValidityInterval）；动画 provider 路径保留逐帧重建、不读写缓存 |
| `1b292e5` | 376d8199：MultiDrawBatch 抽象化 + Direct/IndirectMultiDrawBatch，删除 3 个 Emitter；本地保留 MultiDrawMode 选择与 IndividualDrawEmitter；gl/device 三文件与上游逐字节一致（仅 LWJGL 包名差异） |

适配要点：INDIRECT + 动画模组组合下 batch 为每 region 每帧 ephemeral（one-shot 语义决定无法复用）；
其余路径与上游语义一致。

## 目标

将 `celeritas-common/` 的 multidraw 渲染路径从上次同步基线 `b8c1079a` 推进到上游 HEAD
`376d8199`，共 **2 个上游提交**。JMH bench 文件不收编（本地 JMH harness 已 revert，见
`sync-celeritas-b8c1079a.md`）。

## 上游提交清单

| # | commit | 内容 | 可收编性 |
|---|---|---|---|
| 1 | `2dc805c1` | Cache multidraw batches for regions（新增 CachedBatch；批次按 region 缓存，相机离开包围盒才失效） | **需适配**（BatchAssembler/DefaultChunkRenderer 本地分叉大） |
| 2 | `376d8199` | Refactor multidraw batches again（MultiDrawBatch 抽象化 + Direct/IndirectMultiDrawBatch；删除 3 个 Emitter） | **需适配**（同上；JMH bench 跳过） |

## 上游改动文件

### 2dc805c1（7 文件，+226/-26）
- **新增**：`multidraw/CachedBatch.java`
- **修改**：`multidraw/BatchAssembler.java`、`DefaultChunkRenderer.java`、
  `data/SectionRenderDataStorage.java`、`lists/ChunkRenderListIterable.java`、
  `lists/SortedRenderLists.java`、`gl/device/MultiDrawBatch.java`

### 376d8199（12 文件，+265/-244）
- **新增**：`gl/device/DirectMultiDrawBatch.java`、`gl/device/IndirectMultiDrawBatch.java`
- **删除**：`multidraw/MultiDrawEmitter.java`、`DirectMultiDrawEmitter.java`、`IndirectMultiDrawEmitter.java`
- **修改**：`gl/device/MultiDrawBatch.java`（改抽象基类）、`gl/device/DrawCommandList.java`、
  `gl/device/GLRenderDevice.java`、`multidraw/BatchAssembler.java`、`DefaultChunkRenderer.java`
- **跳过**：`multidraw/bench/MultiDrawBench.java`、`occlusion/bench/OcclusionCullerBench.java`（JMH，本地无 harness）

## 本地分叉勘察（与上游 b8c1079a 逐文件 diff 行数）

| 文件 | 差异行数 | 说明 |
|---|---|---|
| `lists/ChunkRenderListIterable.java` | 0 | 与上游一致，直接移植 |
| `lists/SortedRenderLists.java` | 0 | 与上游一致，直接移植 |
| `gl/device/DrawCommandList.java` | 0 | 与上游一致，直接移植 |
| `gl/device/MultiDrawBatch.java` | 6 | 小差异 |
| `gl/device/GLRenderDevice.java` | 11 | 小差异 |
| `multidraw/MultiDrawEmitter.java` | 4 | 小差异（376d8199 将删除） |
| `multidraw/DirectMultiDrawEmitter.java` | 10 | 小差异（376d8199 将删除） |
| `data/SectionRenderDataStorage.java` | 58 | 中等差异 |
| `multidraw/IndirectMultiDrawEmitter.java` | 70 | 中等差异（376d8199 将删除） |
| `render/chunk/DefaultChunkRenderer.java` | 204 | 大分叉，逐 hunk 对照 |
| `multidraw/BatchAssembler.java` | 472 | 大分叉，逐 hunk 对照 |

> 结论：本次同步**不能** `git apply`，需逐 hunk 对照上游 diff 与本地文件手动移植，
> 保留本地适配（如 `nearestToZero` 内联等既有分叉）。

## 提交拆分

- `perf(celeritas): sync multidraw batch region caching from upstream`（2dc805c1）
- `refactor(celeritas): sync multidraw batch refactor from upstream`（376d8199）

## 验证

- 每阶段：`./gradlew compileJava --no-daemon`；完成后 `./gradlew check --no-daemon` 全绿。
- 删除 Emitter 类后 Grep 确认无残留引用（含 `glsm/`、`src/`、`shader/`）。
- 渲染行为有实际变化（批次缓存 + direct/indirect 重构）：**遗留 C 类运行验证**——dev 环境
  目测区块渲染、direct/indirect/individual 切换（`MultiDrawMode`）、与光影包组合无回归。
