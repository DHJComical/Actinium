# Distant Horizons 兼容性说明

兼容状态：**部分兼容**（版本敏感，必须按指定版本验证）
最后更新：2026-09-16

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

`MixinRenderGlobal`（`mixin/vintage/core/terrain`）**覆盖单参数** `renderBlockLayer(BlockRenderLayer)`，
而不是四参数入口：

- 原版四参数入口保留，DH 的 HEAD 注入仍然可达；
- 覆盖后的方法体保留 `enableLightmap()` 调用，DH 的延迟注入因此落在
  `actinium$beginIrisTranslucents()` 之后（Iris 已进入半透明阶段），时机正确。

> 与 Angelica 同取向：Actinium 只替换"实际绘制"层，不占用 DH 也要注入的渲染入口。
> 若把 `@Overwrite` 挪回四参数入口，原版四参数不再调用单参数，DH 的延迟注入将永远不触发。

### Iris 侧接管

`shader` 子项目的 `net.coderbot.iris.compat.dh`（`DHCompat`、`LodRendererEvents`、
`DHCompatInternal`、`IrisLodRenderProgram`、`IrisGenericRenderProgram`、`DhFrameBufferWrapper`）
通过 DH 的 API events 注册 Iris 的 LOD / generic override program、framebuffer 与深度纹理。

## 开关归属

- `IIrisAccessor`：DH 的 1.12.2 Iris 支持（上游 `b15b57cf`）在检测到 `actinium` 已加载时自行绑定；
- `renderProxy.setDeferTransparentRendering()`：DH 侧既没有配置项也没有内部调用（整仓只有 setter 定义），
  该开关由 Iris 集成设置。Actinium 在 `LodRendererEvents` 的 `DhApiBeforeRenderEvent` 处理器里设置它，
  与 Angelica 完全一致；Actinium 侧不再有第二处（原先 `DistantHorizonsCompat` 每帧重同步的那处已随类删除）。

Actinium 不注入 DH：`compat/dh` 只保留 `DhFogColorBridge`（走 `DhApiBeforeFogRenderEvent` 报告雾色）。
搭配更早的 DH 会缺失光影 LOD 集成；`gradle/scripts/dependencies.gradle` 里的
`distant-horizons-508933:8389134`（3.2.0-b）早于这些提交，升级前 dev 环境不会走新的光影 LOD 路径。

## 移除 DH 注入后由 DH 自身承担的部分

以下行为此前由 Actinium 注入 DH 实现，现在交由 DH 侧接管：

- **far clip / far fade / AA**：DH 的 `RenderUtil.getFarClipPlaneDistanceInBlocks()` 与
  `LodRenderer.renderTerrain()` 只判断 `IRIS_ACCESSOR != null`，而 DH 自注册的访问器恒非 null，
  无光影包时也会走 Iris 分支（far clip 缩短为 `√2/2`）。建议 DH 上游改判 `isShaderPackInUse()`。
- **多线程 LOD 构建**：方块状态缓存查询的加锁（原 `MixinFullDataToRenderDataTransformer`）。DH 侧该缓存
  目前仍是非同步的 lazy 静态字段，没有公开 API 可以替代这层保护，属于最需要 DH 侧接管的稳定性回归。
- **启动顺序**：原 `MixinConfig` / `MixinDependencySetup` 让 Actinium 提前建立 DH 客户端绑定、
  并让 DH 自身的绑定调用跳过；移除后 Actinium 不再提前绑定 DH 依赖。DH 的
  `Config.Client.Advanced.*` 静态初始化若在 FML init 之前被触碰，会因 `IMinecraftSharedWrapper`
  未绑定而 NPE——这条现在由 DH 自身保证。
- **雾色**（已修）：DH 的无光影路径经 `MinecraftRenderWrapper.getFogColor` 用裸 `glGetFloatv(GL_FOG_COLOR)`
  取雾色，而 Actinium 的 `GLSMRedirector` 少了 `glGetFloatv -> glGetFloat` 这条重定向（Angelica 有），
  查询因此落到真实 GL、读到默认黑色，远处云与 LOD 被染黑。补上重定向后查询回到 GLSM 虚拟状态，
  DH 侧无需改动（`GLStateManager.glGetFloat` 本就有 `GL_FOG_COLOR` 分支）。
- **F3 覆盖层**：DH 渲染目标状态的调试行（原 `InvokerGlDhMetaRenderer`）。深度纹理 id 与尺寸可用
  `IDhApiRenderProxy.getDhDepthTextureGlId()` 与 `DhApiColorDepthTextureCreatedEvent` 重建
  （`LodRendererEvents` 已在用这两条 API）。

## 验证记录

- 兼容矩阵记录：光影包 + DH LOD 场景已验证（MakeUp/BSL/Complementary/Bliss/
  iterationT/iterationRP）。
- DH 3.1.2-b + 光影 + 进出世界/维度切换回归通过。

## 待办

- [ ] DH 发布含 `b15b57cf` / `ae21a1a0` 的版本后升级 gradle 依赖并做完整回归
- [ ] 实机确认 `renderBlockLayer` 覆盖层级下移后的地形与 DH LOD 渲染（含 GTCEu 改写
      `renderWorldPass` 调用点的路径）
- [ ] 与 DH 侧确认上节各项由 DH 接管后的实际表现（尤其 far clip 与雾色）
- [ ] 新版本 DH 验证后更新兼容矩阵
