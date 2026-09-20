# Distant Horizons 兼容性说明

兼容状态：**部分兼容**（版本敏感，必须按指定版本验证）
最后更新：2026-09-18

## 模组信息

- Distant Horizons（modid `distanthorizons`），按兼容矩阵指定版本
- 接入方式：**不注入 DH**。DH 自行持有 Iris 访问器并驱动 LOD 渲染；Actinium 只在 Iris 侧通过 DH 的
  公开 API（`DhApi`、render proxy、override、events）接管 shader program / framebuffer / 深度纹理，
  并设置 deferred 开关与雾色

## 机制

### DH 自己的两个驱动点

DH 的 cleanroom Mixin 直接注入原版渲染：

- **不透明 LOD**：`RenderGlobal.renderBlockLayer(BlockRenderLayer, double, int, Entity)` 的 HEAD
  （`@Inject`），SOLID 时调用 `ClientApi.renderLods()`；
- **延迟透明 LOD**：`RenderGlobal.renderBlockLayer(BlockRenderLayer)`（单参数私有重载）里
  `EntityRenderer.enableLightmap()` 调用之后，TRANSLUCENT 时调用
  `ClientApi.renderDeferredLodsForShaders()`（上游提交 `ae21a1a0` 起）。

DH 的渲染状态矩阵由它自己的 `MixinActiveRenderInfo` 从原版 `ActiveRenderInfo` 的
`MODELVIEW`/`PROJECTION` buffer 采集（**Actinium 不覆盖 `ActiveRenderInfo`，该路径保持原版**）。

### Actinium 侧的配合

`MixinRenderGlobal`（`mixin/vintage/core/terrain`）**覆盖四参数入口**
`renderBlockLayer(BlockRenderLayer, double, int, Entity)`，并在 TRANSLUCENT 分支显式调用
原版的单参数重载 `renderBlockLayer(BlockRenderLayer)`（`@Shadow`，保持 vanilla 不改动）：

- DH 在四参数入口的 HEAD 注入是虚拟注入点，不受 `@Overwrite` 影响，继续生效；
- 单参数重载保持原版实现，DH 锚定其内部 `enableLightmap()` 调用的延迟注入因此可达，
  且落在 `actinium$beginIrisTranslucents()` 之后（Iris 已进入半透明阶段），时机正确。

> 为什么不能覆盖单参数重载：Mixin 拒绝在已被更高优先级 Mixin 覆盖的方法内使用指令级
> 注入点（`@At(INVOKE …)`）；Actinium（priority 1000）先于 DH（900）合并，覆盖单参数会让
> DH 的延迟注入在 Mixin 应用期抛 `InvalidInjectionException`。反过来只覆盖四参却不显式
> 调单参，该注入又永远不触发——覆盖四参入口并显式调用单参重载，是唯一让两个 DH 注入
> 都保持可达的组合。

### Iris 侧接管

`shader` 子项目的 `net.coderbot.iris.compat.dh`（`DHCompat`、`LodRendererEvents`、
`DHCompatInternal`、`IrisLodRenderProgram`、`IrisGenericRenderProgram`、`DhFrameBufferWrapper`）
通过 DH 的 API events 注册 Iris 的 LOD / generic override program、framebuffer 与深度纹理。

## 开关归属

- `IIrisAccessor`：DH 的 1.12.2 Iris 支持（上游 `b15b57cf`）在检测到 `actinium` 已加载时自行绑定；
- `renderProxy.setDeferTransparentRendering()`：DH 侧既没有配置项也没有内部调用（整仓只有 setter 定义），
  该开关由 Iris 集成设置。Actinium 在 `LodRendererEvents` 的 `DhApiBeforeRenderEvent` 处理器里设置它，
  与 Angelica 完全一致；Actinium 侧不再有第二处（原先 `DistantHorizonsCompat` 每帧重同步的那处已随类删除）。

Actinium 不注入 DH，也不持有任何 DH 侧状态：`compat/dh` 目录已无实现类。雾色问题由 GLSM 重定向解决
（见下节），不需要额外的 API 桥。
搭配更早的 DH 会缺失光影 LOD 集成；`gradle/scripts/dependencies.gradle` 已改 pin 到
`maven.modrinth:uCdwusMi:Sa0ttGJr`（3.3.0-1.12.2，首个包含这两个提交的发布版）。

## 上游补丁移植（Angelica #2090 / #2091）

DH 开发者（DarkShadow44）提交、并已被 Angelica upstream 合并的两个修复，Actinium 按同样语义移植。

### far uniform 报告实际渲染边界（#2091，upstream `2aa4ac75`）

上游修复：`CameraUniforms.getRenderDistanceInBlocks()` 在 DH 存在且未启用
`AngelicaConfig.useVanillaChunkTracking`（即默认的邻居就绪 tracker 模式）时，把报告给光影包的
`far` 减一圈。原因是 Celeritas 的 tracker 要求相邻 chunk 全部加载后才发布当前 chunk，而配置渲染距离
之外的 chunk 永远不加载，最外圈因此永不渲染。光影包用 `far` 做原版地形到 DH LOD 的过渡，必须落在
真实可见边界上，报告配置加载半径会让过渡位置外移一圈。

Actinium 的等价实现（不新增桥、不新增依赖边）：

- `celeritas-common` 的 `ChunkTracker` 新增只读 `getRequiredNeighborRadius()`；
- shader 侧 `SkyRenderDistance.farBlocks(renderDistanceChunks, unrenderedOuterRings)` 只做
  "减去未渲染外圈"的计算，**不套用 Actinium 的天空下限**：下限会把边界报得比实际渲染的地形更远，
  光影包的过渡带随即落在没有地形的区间里，表现为 LOD 与原版地形之间的大断层（渲染距离 2 区块时
  尤其明显，`far` 会被抬到 8×16=128 而地形只到 16）。天空下限仍由 `effectiveChunks` 路径承担
  （`HorizonRenderer` 几何、`setupCameraTransform` 投影、`updateFogColor` 雾色，见 `8faa19ee`／
  `6eb7cc40`），`CameraUniforms` 只在 DH + Celeritas 组合**之外**才走那条带下限的路径；
- `CameraUniforms.getUnrenderedOuterRings()` 只在 DH 已加载、Celeritas 地形渲染启用且世界已加载时
  读取 tracker 半径（当前恒为 1，等价于上游的 `-1`）。

上游条件里的 `useVanillaChunkTracking` 在 Actinium 没有对应实现：`ActiniumConfig.useVanillaChunkTracking`
自导入起没有任何使用点，`ChunkTracker.setRequiredNeighborRadius` 也没有调用方，Actinium 只存在邻居
就绪 tracker 这一种模式。因此这里读 tracker 的真实半径，而不是引入一个恒为 false 的开关。

### 移除多余的 attrib pointer 调用（#2090，upstream `8f981510`）

`IrisGenericRenderProgram` 构造 VAO 时在 `glEnableVertexAttribArray(0)` 之前调用
`glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)`：此刻 attrib 0 指向"构造时恰好绑定的
`GL_ARRAY_BUFFER`"（不保证是 DH 的顶点 buffer），会向 GLSM 的 per-VAO attrib 记录写入一条伪状态。
DH 在绘制前通过 `bindVertexBuffer(int)` 绑定自己的 buffer 并设置 `stride = 12` 的 pointer，构造期那次
设置没有作用，已按上游删除。

## 与 Angelica 对齐：不代打的补丁

核对 Angelica 源码：它对 DH 的类**零注入**——mixin 配置里没有任何一处提到
`distanthorizons`，也搜不到 `IRIS_ACCESSOR`、`FullDataToRenderDataTransformer`、`createClientBindings`。
下列补丁 Angelica 都不打，因此 Actinium 也不打（Actinium 侧原有实现已随 `mixin/mod/dh` 一并删除）：

- **far clip / far fade / AA**：DH 的 `RenderUtil.getFarClipPlaneDistanceInBlocks()` 与
  `LodRenderer.renderTerrain()` 只判断 `IRIS_ACCESSOR != null`，而 DH 自注册的访问器恒非 null，
  无光影包时也会走 Iris 分支（far clip 缩短为 `√2/2`）。
- **多线程 LOD 构建**：`BlockStateWrapper` 的方块状态缓存是非同步的 lazy 静态字段（原
  `MixinFullDataToRenderDataTransformer` 负责加锁）。
- **启动顺序**：DH 的 `Config.Client.Advanced.*` 若在 `FMLInitializationEvent` 之前被触碰，会因
  `IMinecraftSharedWrapper` 未绑定而 NPE（原 `MixinConfig` / `MixinDependencySetup` 负责提前绑定，
  并让 DH 自身的绑定调用跳过）。
- **F3 覆盖层**：DH 渲染目标状态的调试行（原 `InvokerGlDhMetaRenderer`）。

以上都属于 DH 自身的状态，需要时由 DH 修复。

### 雾色：GLSM 侧的缺口，不是 DH 的补丁

DH 的无光影路径经 `MinecraftRenderWrapper.getFogColor` 用裸 `glGetFloatv(GL_FOG_COLOR)` 取雾色，而
Actinium 的 `glFog` 只把雾色写进 GLSM 虚拟状态、不转发真实 GL。Actinium 的 `GLSMRedirector` 原先缺少
`glGetFloatv -> glGetFloat` 重定向（Angelica 有这条），查询因此落到真实 GL、读到默认黑色，远处云与
LOD 被染黑。补上重定向后（并补齐 `GLStateManager` 缺失的数组重载）查询回到虚拟状态，
`GLStateManager.glGetFloat` 本就有 `GL_FOG_COLOR` 分支，DH 侧无需改动。

## 验证记录

- 兼容矩阵记录：光影包 + DH LOD 场景已验证（MakeUp/BSL/Complementary/Bliss/
  iterationT/iterationRP）。
- DH 3.1.2-b + 光影 + 进出世界/维度切换回归通过。
- 2026-09-18：DH 3.3.0-1.12.2（`maven.modrinth:uCdwusMi:Sa0ttGJr`，Actinium `6e66a3c7`）
  实机回归通过：光影包 + DH LOD（矩阵六包）、无光影 LOD/雾色/天空盒、进出世界/维度切换。
- 2026-09-18：移植 Angelica #2090 / #2091（见上节）。`compileJava test` 通过，含
  `SkyRenderDistanceTest` 新增的 far 边界用例；实机表现待验证。

## 待办

- [ ] 实机验证渲染距离 2 区块等低配置下 LOD 与原版地形的过渡（#2091）、DH generic object
      渲染（#2090），以及去掉 `far` 天空下限后低渲染距离的天空/云/天体的表现
- [ ] 实机确认四参入口 `@Overwrite` + TRANSLUCENT 显式调用单参重载下的地形与 DH LOD
      渲染（含 GTCEu 改写 `renderWorldPass` 调用点的路径）
- [ ] 与 DH 侧确认上节各项由 DH 接管后的实际表现（尤其 far clip 与雾色）
