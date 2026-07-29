# SimpleFPEWrapper 性能优化对照路线

最后更新：2026-07-29。

## 目的与证据边界

本文记录对 `D:/Code/SimpleFPEWrapper` 的性能改动进行逐项对照后，Actinium 可以复用的原则、
已经等价的实现、不适用项和后续候选。对照基线主要是 SimpleFPEWrapper 的
`plans/12-fpe-draw-cost.md` 及以下提交：

- `247e608`：为 immediate-mode vertex upload 引入带 fence 分段的 persistent ring。
- `354cd1b`：把 fixed-function draw state 的恢复推迟到真正的状态边界。
- `d5b07a0`：把 CPU index upload 从每次 `glBufferData` 改为 persistent ring。
- `324c8c2`：合并相邻的小型 immediate-mode draw。
- `d9b829e`：缩窄 texture state barrier，并降低 native TLS hot path 开销。
- `42ff056`：将可兼容的 multi-draw 直接下发到 native backend。

SimpleFPEWrapper 的数据来自 GTX 1660 SUPER、GLES backend 和 1x1 viewport 的微基准，不能
直接作为 Actinium 在 Minecraft 工作负载上的性能结论。本文只把其中有测量支撑的因果关系
作为候选假设；Actinium 的优先级仍以自身的 `GLSMPerfDebug` 分段统计、客户端帧时间和
OpenGL 状态验证为准。

### 上游实现基线

原作者对 `impl/s1-s3` 的总结表明，上游已进一步完成固定管线翻译、persistent vertex/index
ring、backend/VAO/纹理状态 shadow、deferred draw-state restore、显示列表编译与合并、连续
immediate draw 合并、client-array ring、native multi-draw，以及相应的端到端渲染验证和微基准。
因此这些能力在本文中只作为“已等价”或“不适用”记录；本地 checkout 的 `42ff056` 是该仓库
当前可见基线，后续移植判断以代码和运行数据为准，不以提交数量或上游总结中的功能清单代替验证。

## 已有等价项

| SimpleFPEWrapper 路线 | Actinium 现状 | 结论 |
|---|---|---|
| Persistent coherent ring、分段 fence、overflow fallback | `PersistentStreamingBuffer` 已提供 coherent mapping、ring offset、fence reclaim；`TessellatorStreamingDrawer` 和 `BufferBuilderStreamingDrawer` 均优先使用它并回退到 `OrphanStreamingBuffer` | 已等价，不重新实现 ring |
| 避免每次上传 `glBufferData` | 两个 streaming drawer 的正常路径直接复制到 mapped buffer；orphan 仅是硬件不支持或 ring 暂时不可用时的 fallback | 已等价；应分别统计 persistent/orphan 命中率 |
| Shadow GL state，减少同步查询 | `GLStateManager` 已跟踪 VAO、VBO、program、texture 等状态，`getBoundVAO()` 和 `getBoundVBO()` 读取缓存 | 已等价；不要另建一套 shadow state |
| Dirty-gated uniform/attribute 提交 | GLSM 使用 generation/dirty 标记更新 FFP uniform 和 current attributes；streaming VAO 在格式首次出现时创建并复用 | 已等价；后续优化应先证明仍有重复 driver call |
| Native multi-draw | Celeritas 已有 `DirectMultiDrawEmitter`、`IndirectMultiDrawEmitter` 和 native `multiDrawElementsBaseVertex`/`multiDrawElementsIndirect` backend | 区块管线已等价，不从 wrapper 再移植 |
| VAO 隔离 app-owned vertex state | streaming drawer 按 vertex format 分别持有 persistent/orphan VAO，attribute pointer 在 VAO 创建时捕获对应 VBO | 已等价，也是本轮可删除 draw-time persistent VBO bind 的前提 |

## 不适用或暂不采用

| 项目 | 原因 | 后续条件 |
|---|---|---|
| native TLS model、`eglGetCurrentContext` 缓存和动态符号分派优化 | 属于 C/C++ shared library、EGL 和 ELF TLS 成本；Actinium 运行在 JVM/LWJGL 上，没有同一调用模型 | 不迁移 |
| 使用 default VAO 的 client-memory array 来绕过 upload | Actinium 的现代渲染路径要求 core-profile 兼容并隔离第三方模组状态；client pointer 会破坏这一边界 | 不迁移 |
| 全局推迟 program/VAO/VBO 恢复 | SimpleFPEWrapper 包装完整入口面，能定义 barrier；Actinium 与 Minecraft、光影及其他模组共享 GL 上下文，任意调用点都可能成为观察边界 | 只允许对副作用已局部证明的单条路径缩窄 |
| 将 persistent mapping 改为 non-coherent 并显式 flush | SimpleFPEWrapper 的 NVIDIA 实测慢约 3.4 倍，driver 在 flush 时复制 | 除非 Actinium 的跨厂商数据证明 coherent mapping 是瓶颈，否则不重试 |
| 固定 ring offset 为零或取消 fence | SimpleFPEWrapper 实测出现约 4 倍退化，原因是覆盖 GPU 仍在读取的区域导致 stall | 不迁移 |
| 直接移植 immediate/display-list merge | Actinium 已有独立的 Tessellator、BufferBuilder、font batch、command recording 和 chunk multi-draw 所有权，跨边界合并可能改变 texture、matrix、shader 和 debug source 语义 | 先用生产 workload 找到明确的小 draw 来源，再在其所有者内部设计 |

## 候选优先级

### P0：缩窄 BufferBuilder persistent draw 的 VBO 状态操作

状态：本轮实现。

证据：`PersistentStreamingBuffer.upload` 只复制到长期映射地址，不 bind buffer；
`createStreamingVertexArray` 已在 attribute pointer 创建时捕获 persistent VBO；同仓库的
`TessellatorStreamingDrawer` persistent 分支也只 bind VAO。原 `drawRaw` 仍在每次成功
persistent upload 后 bind persistent VBO，并在 `finally` 无条件恢复调用者 VBO。这与
SimpleFPEWrapper `d9b829e` 的核心结论一致：状态操作应按实际副作用收窄。

实现边界：

- persistent 成功时只 bind 对应 VAO；仍保留 persistent VBO ID 供 draw diagnostics 使用。
- persistent 路径始终恢复调用者 VAO，但不写 `GL_ARRAY_BUFFER`，因为该 binding 从未改变。
- orphan fallback 继续通过 `GLStateManager` bind upload VBO，并恢复调用者 VBO。该显式 bind
  还负责让 GLSM cache 与随后绕过 cache 的 backend bind/unbind 保持可恢复关系，不能孤立删除。
- fence、upload、wide-line、shader pre-draw、draw conversion、debug 和 error capture 语义不变。

风险：对 VAO 创建流程或第三方 raw GL 绕过 GLSM 的错误假设会造成 attribute source 不正确或
状态缓存失配。风险局限于 BufferBuilder streaming persistent path，且可通过强制 orphan 属性
快速回退验证。

### P1：统一 orphan uploader 与 GLSM 的 binding 所有权

证据：`OrphanStreamingBuffer.upload` 通过 `RENDER_BACKEND` 自行 bind VBO 并最终 bind 0，调用者
为了恢复 GLSM cache 还要先通过 `GLStateManager` bind 一次、再在 draw 后恢复。这一所有权分裂
制造了重复调用，也容易在未来重构时产生 cache/driver 不一致。

候选方向：先定义由 uploader 或调用者单方拥有 bind/restore 的接口契约，再同时迁移
`BufferBuilderStreamingDrawer` 与 `TessellatorStreamingDrawer`。不得只删除当前显式 bind。

风险：共享 `glsm` 模块、所有 streaming caller、异常路径和 buffer destroy 都受影响，属于中等
风险跨模块改动。必须增加 fake backend 的调用序列测试，并验证调用者原先绑定的 VBO 为 0、
普通 VBO 和同一 VBO 三种情况。

### P2：按生产采样合并同源小 draw

证据：SimpleFPEWrapper `324c8c2` 将 tiny batch 从约 0.89 us 降到 0.33 us，但收益依赖严格的
相邻状态兼容。Actinium 已能通过 `GLSMPerfDebug.countBufferBuilder` 按 debug source、draw mode
和 vertex count 找到高频小 draw。

候选方向：只在拥有完整状态边界的具体 producer 内合并，例如同一 font batch 或同一 GUI
renderer；不要在通用 `BufferBuilderStreamingDrawer` 中跨调用缓存。进入设计前需证明某个 source
同时满足高调用量、小 vertex count、相同 format/program/texture/state。

风险：draw order、透明混合、matrix、texture、shader uniform、debug attribution 和异常时 flush
都可能改变，风险高。需要像 SimpleFPEWrapper 一样为每个 primitive 和状态切换边界做画面测试。

### P3：针对 ring 大小和 fence 分段做跨厂商调优

证据：SimpleFPEWrapper 证明 rotating offset 与 fence 是必要成本，但没有证明其容量和分段参数
适用于 Minecraft。Actinium 目前固定 16 MiB，并已有 remaining、persistent/orphan upload stage。

候选方向：记录每帧上传量、wrap、同步等待和 orphan overflow 次数后，再决定容量是否应配置化
或按 workload 调整。没有 stall/overflow 数据时不改常量。

风险：增大容量会增加长期映射显存/共享内存，减小容量或改变 fence 粒度会引入 GPU stall；必须
覆盖 NVIDIA、AMD 和 Intel。

## 验证策略

### 自动化验证

- 单元测试直接调用 `BufferBuilderStreamingDrawer.DrawPath` 的生产决策，验证 persistent upload
  选中 persistent VAO/VBO 且不声明 array-buffer 副作用，orphan upload 选中 orphan VAO/VBO 且
  必须恢复调用者 binding。
- 运行目标测试和 `compileJava`，并将 `GRADLE_USER_HOME` 固定为 `D:/gradle`。
- 发布前仍运行完整 `build`；本轮单元测试不能替代真实 OpenGL context 验证。

### 客户端正确性验证

1. 在支持 buffer storage 的设备上分别运行默认 persistent 路径和
   `-Dactinium.glsm.forceOrphanStreaming=true` 强制 orphan 路径。
2. 开启 GL state capture，在调用前绑定非零 VAO/VBO，验证 draw 后 caller VAO 始终恢复，
   persistent 路径的 caller VBO 未被写，orphan 路径的 caller VBO 被恢复。
3. 用超过 ring 可用空间的连续 upload 触发单帧 orphan fallback，确认 fallback draw 和下一次
   persistent draw 都正确。
4. 覆盖世界区块、GUI/font、shader pack、Distant Horizons、资源重载和 `destroy`/重建场景，检查
   无 GL error、无 attribute 错位、无闪烁。
5. 开启 draw diagnostics，确认 persistent 日志记录 persistent buffer ID，fallback 日志记录
   orphan buffer ID；`after-restore` 仍记录调用者保存的 VAO/VBO。

### 性能验证

- 用相同场景、分辨率、shader pack 和视距做 A/B，至少预热后采集 60 秒。
- 比较 `BUFFERBUILDER_STREAM_DRAW`、`BUFFERBUILDER_PERSISTENT_UPLOAD`、
  `BUFFERBUILDER_ORPHAN_UPLOAD` 和 `BUFFERBUILDER_DRAW_CALL` 的 count、累计时间及每次平均值。
- 同时记录 CPU frame time 的 p50/p95/p99 和 orphan 命中率。验收目标是 persistent draw 的状态
  调用减少且画面/GL error 无回归；微小于噪声时不继续扩大状态恢复优化范围。
